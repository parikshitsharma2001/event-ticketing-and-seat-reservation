import { Body, Controller, Get, Param, Post, Req, Res, HttpException } from '@nestjs/common';
import { CreateChargeAPIDto } from '../dto/create-charge.dto';
import { RefundDto } from '../dto/refund.dto';
import { Request, Response } from 'express';
import { PaymentsService } from '../service/payments';
import * as os from 'os';

@Controller()
export class PaymentsController {
  constructor(
    private readonly paymentsService: PaymentsService,
  ) { }

  @Get('health')
  health() {
    return { ok: true, service: 'payment-service' };
  }

  @Get('metrics')
  getMetrics() {
    const memory = process.memoryUsage();
    const cpuLoad = os.loadavg()[0];
    return {
      service: 'payment-service',
      uptime_seconds: process.uptime(),
      memory_mb: (memory.rss / 1024 / 1024).toFixed(2),
      heap_used_mb: (memory.heapUsed / 1024 / 1024).toFixed(2),
      cpu_load_1m: cpuLoad.toFixed(2),
      platform: os.platform(),
      timestamp: new Date().toISOString(),
    };
  }

  @Post('v1/charge')
  async charge(@Req() req: Request, @Body() body: CreateChargeAPIDto, @Res() res: Response) {
    const idempotencyKey = req.header('Idempotency-Key');
    if (!idempotencyKey) return res.status(400).json({ error: 'Idempotency-Key required' });

    try {
      const payment = await this.paymentsService.createCharge(idempotencyKey, {
        merchant_order_id: body.merchant_order_id,
        amount_cents: body.amount_cents,
        currency: body.currency
      });
      return res.json({
        id: payment.id,
        merchant_order_id: payment.merchantOrderId,
        provider_txn_id: payment.providerTxnId,
        status: payment.status,
        amount_cents: payment.amountCents,
        currency: payment.currency
      });
    } catch (err: any) {
      throw new HttpException(err.message || 'internal', err.status || 500);
    }
  }

  @Post('v1/refund')
  async refund(@Body() body: RefundDto, @Res() res: Response) {
    try {
      const refunded = await this.paymentsService.refund(body.payment_id);
      return res.json({ id: refunded.id, status: refunded.status });
    } catch (err: any) {
      throw new HttpException(err.message || 'internal', err.status || 500);
    }
  }

  @Get('v1/payments/:id')
  async getById(@Param('id') id: string, @Res() res: Response) {
    const payment = await this.paymentsService.findById(id);
    if (!payment) return res.status(404).json({ error: 'not found' });
    return res.json(payment);
  }
}
