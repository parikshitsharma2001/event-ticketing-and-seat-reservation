import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CatalogController } from './controller/catalog';
import { CatalogService } from './service/catalog';
import { EventsCommand } from './repository/events.command';
import { EventsQuery } from './repository/events.query';
import { SeatsCommand } from './repository/seats.command';
import { VenuesCommand } from './repository/venues.command';
import { VenuesQuery } from './repository/venues.query';
import { Seat } from './entities/seat';
import { Venue } from './entities/venue';
import { Event } from './entities/event';

@Module({
  imports: [TypeOrmModule.forFeature([Event, Seat, Venue])],
  controllers: [CatalogController],
  providers: [
    CatalogService,
    EventsCommand,
    EventsQuery,
    SeatsCommand,
    VenuesCommand,
    VenuesQuery,
  ]
})

export class CatalogModule { }
