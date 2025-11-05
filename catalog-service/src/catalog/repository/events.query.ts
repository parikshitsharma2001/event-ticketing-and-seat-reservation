import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Seat } from '../entities/seat';
import { Event } from '../entities/event';

@Injectable()
export class EventsQuery {
  constructor(
    @InjectRepository(Event) private readonly eventRepo: Repository<Event>,
    @InjectRepository(Seat) private readonly seatRepo: Repository<Seat>,
  ) { }

  async listEvents(filters: { city?: string; type?: string; status?: string }): Promise<Event[]> {
    const qb = this.eventRepo.createQueryBuilder('e').leftJoinAndSelect('e.venue', 'v');

    if (filters.type) qb.andWhere('e.type = :type', { type: filters.type });
    if (filters.status) qb.andWhere('e.status = :status', { status: filters.status });
    if (filters.city) qb.andWhere('v.city = :city', { city: filters.city });

    return qb.getMany();
  }

  async getEventById(id: number | string): Promise<{ event: Event; seats: Seat[] } | null> {
    const event = await this.eventRepo.createQueryBuilder('e')
      .leftJoinAndSelect('e.venue', 'v')
      .where('e.id = :id', { id: Number(id) })
      .getOne();

    if (!event) return null;

    const seats = await this.seatRepo.createQueryBuilder('s')
      .where('s.event_id = :eventId', { eventId: Number(id) })
      .orderBy('s.row', 'ASC')
      .addOrderBy('s.number', 'ASC')
      .getMany();

    return { event, seats };
  }
}
