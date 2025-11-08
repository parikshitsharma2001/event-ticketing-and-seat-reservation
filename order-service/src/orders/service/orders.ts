import { Injectable, Logger, BadRequestException, NotFoundException } from '@nestjs/common';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { OrdersQueryRepository } from '../repository/orders.query';
import { OrdersCommandRepository } from '../repository/orders.command';
import { CreateOrderDto } from '../dto/create-order.dto';

const CATALOG_URL = process.env.CATALOG_URL;
const SEATING_URL = process.env.SEATING_URL;
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

  async createOrderFlow(idempotencyKey: string, orderRequest: CreateOrderDto) {
    const { user_id, event_id, seats } = orderRequest as any;

    // idempotency
    const existing = await this.queryRepo.findByIdempotency(idempotencyKey);
    if (existing) return existing;

    const toCents = (v: any) =>
      typeof v === 'number' ? Math.round(v * (v > 1000 ? 1 : 100)) // handle cents vs decimal heuristics
        : !isNaN(Number(v)) ? Math.round(Number(v) * 100) : 0;

    // verify event exists
    const catalogResp = await axios.get(`${CATALOG_URL}/v1/events/${event_id}`);
    if (!catalogResp?.data) throw new Error('Invalid event-id');

    // seating availability (authoritative seat state & prices)
    const availResp = await axios.get(`${SEATING_URL}/v1/seats/availability`, {
      params: { eventId: event_id }, timeout: 3000,
    });
    const availableList = availResp?.data?.availableSeatsList;
    if (!Array.isArray(availableList)) throw new Error('Invalid seating availability response');

    const availableById = new Map(availableList.map((s: any) => [Number(s.id), s]));

    // validate requested seats and create price snapshot
    const seatSnapshots = seats.map((sid: number) => {
      const seat = availableById.get(Number(sid));
      if (!seat) throw Object.assign(new Error('Seat missing or not available'), { detail: { missingSeat: sid } });
      if (String(seat.status).toUpperCase() !== 'AVAILABLE') throw Object.assign(new Error('Seat not available'), { detail: { seatId: sid, status: seat.status } });
      return {
        id: Number(seat.id),
        code: (seat.seatNumber || seat.seat_code || seat.seatCode || '').toString(),
        priceCents: seat.price_cents ?? toCents(seat.price)
      };
    });

    // reserve (temporary hold)
    const reserveResp = await axios.post(`${SEATING_URL}/v1/seats/reserve`, {
      eventId: event_id, seatIds: seats, userId: user_id, ttl_seconds: 15 * 60
    }, { timeout: 3000 });
    if (!reserveResp?.data?.success) throw new Error('Seat reservation failed');

    // totals and create PENDING order
    const subtotal = seatSnapshots.reduce((sum: any, s: { priceCents: any; }) => sum + s.priceCents, 0);
    const tax = Math.ceil(subtotal * (TAX_PERCENT / 100));
    const total = subtotal + tax;

    const order = await this.commandRepo.createOrder({
      idempotencyKey,
      userId: user_id,
      eventId: event_id,
      totalCents: total,
      taxCents: tax,
    });

    // persist items
    await Promise.all(seatSnapshots.map((s: { id: number; code: string; priceCents: number; }) => this.commandRepo.insertOrderItem(order.id, s.id, s.code, s.priceCents)));

    const payIdempotency = `${idempotencyKey}-pay`;
    let paymentResp;
    try {
      paymentResp = await axios.post(`${PAYMENT_URL}/v1/charge`, {
        merchant_order_id: order.id, amount_cents: total, currency: 'INR'
      }, { headers: { 'Idempotency-Key': payIdempotency }, timeout: 8000 });
    } catch (err: any) {
      // best-effort release + fail order if payment call itself errored
      try { await axios.post(`${SEATING_URL}/v1/seats/release`, { eventId: event_id, seatIds: seats, orderId: order.id }, { timeout: 3000 }); } catch (_) { }
      await this.commandRepo.updateOrderStatus(order.id, 'FAILED');
      throw Object.assign(new Error('Payment service unreachable'), { detail: err?.response?.data ?? err?.message });
    }

    const paymentData = paymentResp?.data;
    const status = paymentData?.status ?? null;

    if (status === 'SUCCESS') {
      // allocate, create tickets, confirm, notify
      await axios.post(`${SEATING_URL}/v1/seats/allocate`, { eventId: event_id, seatIds: seats, orderId: order.id }).catch(async (e) => {
        await axios.post(`${SEATING_URL}/v1/seats/release`, { eventId: event_id, seatIds: seats, orderId: order.id }).catch(() => { });
        await this.commandRepo.updateOrderStatus(order.id, 'FAILED');
        throw Object.assign(new Error('Seat allocation failed after payment'), { detail: e?.response?.data ?? e?.message });
      });

      await Promise.all(seats.map(async (seatId: number) => {
        const ticketCode = 'TICKET-' + uuidv4();
        await this.commandRepo.insertTicket(order.id, seatId, ticketCode);
      }));
      await this.commandRepo.updateOrderStatus(order.id, 'CONFIRMED');
      axios.post(`${NOTIFICATION_URL}/v1/notify`, { type: 'ORDER_CONFIRMED', order_id: order.id, seats }).catch(e => this.logger.warn('notify err: ' + (e?.message ?? e)));
    } else if (status === 'FAILED' || status === 'CANCELLED') {
      await axios.post(`${SEATING_URL}/v1/seats/release`, { eventId: event_id, seatIds: seats, orderId: order.id }).catch(e => this.logger.warn('release err: ' + (e?.message ?? e)));
      await this.commandRepo.updateOrderStatus(order.id, 'FAILED');
      axios.post(`${NOTIFICATION_URL}/v1/notify`, { type: 'ORDER_FAILED', order_id: order.id, seats }).catch(e => this.logger.warn('notify err: ' + (e?.message ?? e)));
    }
    // else: PENDING - wait for async callback

    return { order, payment: paymentData };
  }

  async handlePaymentCallback(order_id: string, payment_id: string, status: string) {
    if (!order_id || !payment_id || !status) throw new BadRequestException('invalid');
    const order = await this.queryRepo.findById(order_id);
    if (!order) throw new NotFoundException('order not found');

    if (status === 'SUCCESS') {
      if (order.status !== 'PENDING') return { ok: true, note: 'already processed' };

      const items = await this.queryRepo.findOrderItems(order_id);
      const seats = items.map((r: any) => r.seat_id);

      await axios.post(`${SEATING_URL}/v1/seats/allocate`, { order_id, event_id: order.eventId, seats }).catch((e) => {
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
        await axios.post(`${SEATING_URL}/v1/seats/release`, { order_id, event_id: order.eventId, seats }, { timeout: 3000 });
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
