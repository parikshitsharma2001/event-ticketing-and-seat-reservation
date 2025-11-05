import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Seat } from '../entities/seat';

@Injectable()
export class SeatsCommand {
  constructor(
    @InjectRepository(Seat) private readonly repo: Repository<Seat>,
  ) { }

  async createSeatsBulk(eventId: number | string, seats: Array<any>): Promise<Seat[]> {
    const results: Seat[] = [];
    for (const s of seats) {
      const seat = this.repo.create({
        event_id: Number(eventId),
        seat_code: s.seat_code,
        row: s.row ?? null,
        number: s.number ?? null,
        seat_type: s.seat_type ?? null,
        price_cents: s.price_cents,
      });
      const saved = await this.repo.save(seat);
      results.push(saved);
    }
    return results;
  }

  async updateSeat(id: number, patch: Partial<Seat>) {
    return this.repo.update(id, patch);
  }

  async deleteSeat(id: number) {
    return this.repo.delete(id);
  }
}
