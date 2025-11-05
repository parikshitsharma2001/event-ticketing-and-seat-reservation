import { Injectable, Logger, BadRequestException, NotFoundException } from '@nestjs/common';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { OrdersQueryRepository } from '../repository/orders.query';
import { OrdersCommandRepository } from '../repository/orders.command';
import { CreateOrderDto } from '../dto/create-order.dto';

const CATALOG_URL = process.env.CATALOG_URL;
const RESERVATION_URL = process.env.RESERVATION_URL;
const PAYMENT_URL = process.env.PAYMENT_URL;
const NOTIFICATION_URL = process.env.NOTIFICATION_URL;
const TAX_PERCENT = Number(process.env.TAX_PERCENT);

@Injectable()
export class OrdersService {
  private readonly logger = new Logger(OrdersService.name);

  constructor(
    private readonly queryRepo: OrdersQueryRepository,
    private readonly commandRepo: OrdersCommandRepository,
  ) { }

  async createOrderFlow(idempotencyKey: string, dto: CreateOrderDto) {
    const { user_public_id, event_id, seats } = dto as any;

    const exist = await this.queryRepo.findByIdempotency(idempotencyKey);
    if (exist) return exist;

    const evtResp = await axios.get(`${CATALOG_URL}/v1/events/${event_id}`);
    if (!evtResp.data || !evtResp.data.event) throw new Error('invalid event');

    const seatsInfo = evtResp.data.seats.filter((s: any) => seats.includes(s.id));
    if (seatsInfo.length !== seats.length) throw new Error('one or more seats not found in catalog');

    try {
      await axios.post(`${RESERVATION_URL}/v1/seats/reserve`, { order_id: null, user_public_id, event_id, seats }, { timeout: 3000 });
    } catch (err: any) {
      const detail = err.response ? err.response.data : err.message;
      const e: any = new Error('reservation failed');
      e.detail = detail;
      throw e;
    }

    const subtotal = seatsInfo.reduce((s: number, it: any) => s + it.price_cents, 0);
    const tax = Math.ceil(subtotal * (TAX_PERCENT / 100));
    const total = subtotal + tax;

    const newOrder = await this.commandRepo.createOrder({
      idempotencyKey,
      userPublicId: user_public_id,
      eventId: event_id,
      totalCents: total,
      taxCents: tax,
    });

    const orderId = newOrder.id;
    for (const s of seatsInfo) {
      await this.commandRepo.insertOrderItem(orderId, s.id, s.seat_code, s.price_cents);
    }

    const payIdempotency = idempotencyKey + '-pay';
    const payResp = await axios.post(`${PAYMENT_URL}/v1/charge`, { order_id: orderId, amount_cents: total, currency: 'INR' }, {
      headers: { 'Idempotency-Key': payIdempotency },
      timeout: 5000,
    });

    return { order: newOrder, payment: payResp.data };
  }

  async handlePaymentCallback(order_id: string, payment_id: string, status: string) {
    if (!order_id || !payment_id || !status) throw new BadRequestException('invalid');
    const order = await this.queryRepo.findById(order_id);
    if (!order) throw new NotFoundException('order not found');

    if (status === 'SUCCESS') {
      if (order.status !== 'PENDING') return { ok: true, note: 'already processed' };

      const items = await this.queryRepo.findOrderItems(order_id);
      const seats = items.map((r: any) => r.seat_id);

      await axios.post(`${RESERVATION_URL}/v1/seats/allocate`, { order_id, event_id: order.eventId, seats }).catch((e) => {
        this.logger.warn('reservation allocate err: ' + (e?.message ?? e));
      });

      for (const seatId of seats) {
        const ticketCode = 'TICKET-' + uuidv4();
        await this.commandRepo.insertTicket(order_id, seatId, ticketCode);
      }

      await this.commandRepo.updateOrderStatus(order_id, 'CONFIRMED');

      try {
        await axios.post(`${NOTIFICATION_URL}/v1/notify`, { type: 'ORDER_CONFIRMED', order_id, seats }, { timeout: 2000 });
      } catch (e: any) {
        this.logger.warn('notify err: ' + (e?.message ?? e));
      }

      return { ok: true };
    } else {
      const items = await this.queryRepo.findOrderItems(order_id);
      const seats = items.map((r: any) => r.seat_id);

      try {
        await axios.post(`${RESERVATION_URL}/v1/seats/release`, { order_id, event_id: order.eventId, seats }, { timeout: 3000 });
      } catch (e: any) {
        this.logger.warn('release err: ' + (e?.message ?? e));
      }

      await this.commandRepo.updateOrderStatus(order_id, 'FAILED');

      try {
        await axios.post(`${NOTIFICATION_URL}/v1/notify`, { type: 'ORDER_FAILED', order_id, seats }, { timeout: 2000 });
      } catch (e: any) {
        this.logger.warn('notify err: ' + (e?.message ?? e));
      }

      return { ok: true };
    }
  }

  async findById(id: string) {
    return this.queryRepo.findById(id);
  }

  async findOrderItems(orderId: string) {
    return this.queryRepo.findOrderItems(orderId);
  }

  async findTickets(orderId: string) {
    return this.queryRepo.findTickets(orderId);
  }
}
