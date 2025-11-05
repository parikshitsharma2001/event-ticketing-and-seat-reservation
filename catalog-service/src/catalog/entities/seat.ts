import { Entity, PrimaryGeneratedColumn, Column, ManyToOne, JoinColumn, Unique } from 'typeorm';
import { Event } from './event';

@Entity({ name: 'seats' })
@Unique(['event_id', 'seat_code'])
export class Seat {
  @PrimaryGeneratedColumn()
  id!: number;

  @ManyToOne(() => Event, (event) => event.seats, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'event_id' })
  event!: Event;

  @Column({ type: 'int' })
  event_id!: number;

  @Column({ type: 'text' })
  seat_code!: string;

  @Column({ type: 'text', nullable: true })
  row!: string;

  @Column({ type: 'int', nullable: true })
  number!: number;

  @Column({ type: 'text', nullable: true })
  seat_type!: string;

  @Column({ type: 'int' })
  price_cents!: number;
}
