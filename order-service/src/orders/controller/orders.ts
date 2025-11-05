import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  Req,
  Res,
  Logger,
  HttpStatus,
} from '@nestjs/common';
import { Request, Response } from 'express';
import * as os from 'os';
import { OrdersService } from '../service/orders';
import { CreateOrderDto } from '../dto/create-order.dto';
import { PaymentCallbackDto } from '../dto/payment-callback.dto';

@Controller()
export class OrdersController {
  private readonly logger = new Logger(OrdersController.name);

  constructor(private readonly ordersService: OrdersService) { }

  @Get('health')
  health() {
    return { ok: true, service: 'order-service' };
  }

  @Get('metrics')
  getMetrics() {
    const memory = process.memoryUsage();
    const cpuLoad = os.loadavg()[0];
    return {
      service: 'order-service',
      uptime_seconds: process.uptime(),
      memory_mb: (memory.rss / 1024 / 1024).toFixed(2),
      heap_used_mb: (memory.heapUsed / 1024 / 1024).toFixed(2),
      cpu_load_1m: cpuLoad.toFixed(2),
      platform: os.platform(),
      timestamp: new Date().toISOString(),
    };
  }

  @Post('v1/orders')
  async createOrder(
    @Req() req: Request,
    @Body() body: CreateOrderDto,
    @Res() res: Response,
  ) {
    const idempotencyKey = req.header('Idempotency-Key');

    try {
      const result = await this.ordersService.createOrderFlow(idempotencyKey!, body);
      return res.status(201).json(result);
    } catch (error: any) {
      this.logger.error(error);
      return res.status(error?.status || HttpStatus.INTERNAL_SERVER_ERROR).json({ error: JSON.stringify(error) });
    }
  }

  @Get('v1/orders/:orderId')
  async getOrderById(@Param('orderId') orderId: string, @Res() res: Response) {
    try {
      const order = await this.ordersService.findById(orderId);
      if (!order) return res.status(404).json({ error: 'not found' });

      const items = await this.ordersService.findOrderItems(orderId);
      const tickets = await this.ordersService.findTickets(orderId);
      return res.json({ order: order, items, tickets });
    } catch (error: any) {
      this.logger.error(error);
      return res.status(error?.status || HttpStatus.INTERNAL_SERVER_ERROR).json({ error: JSON.stringify(error) });
    }
  }

  @Post('v1/payments/callback')
  async paymentCallback(@Body() body: PaymentCallbackDto, @Res() res: Response) {
    try {
      const response = await this.ordersService.handlePaymentCallback(
        body.order_id,
        body.payment_id,
        body.status,
      );
      return res.json(response);
    } catch (error: any) {
      this.logger.error(error);
      return res.status(error?.status || HttpStatus.INTERNAL_SERVER_ERROR).json({ error: JSON.stringify(error) });
    }
  }
}
