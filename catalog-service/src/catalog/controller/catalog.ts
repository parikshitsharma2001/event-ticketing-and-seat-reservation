import { Controller, Get, Post, Body, Param, Query, Res, HttpStatus, UsePipes, ValidationPipe, ParseIntPipe } from '@nestjs/common';
import { CatalogService } from '../service/catalog';
import { Response } from 'express';
import { CreateVenueDto } from '../dto/venue.dto';
import { CreateEventDto, ListEventsQueryDto } from '../dto/event.dto';
import { CreateSeatsDto } from '../dto/seats.dto';

@Controller()
export class CatalogController {
  constructor(private readonly catalogService: CatalogService) { }

  @Get('/health')
  health(@Res() response: Response) {
    return response
      .status(HttpStatus.OK)
      .json({ ok: true, service: 'catalog-service' });
  }

  @Post('/v1/venues')
  @UsePipes(new ValidationPipe({ transform: true, whitelist: false }))
  async createVenue(
    @Body() createVenueDto: CreateVenueDto,
    @Res() response: Response
  ) {
    try {
      const createdVenue = await this.catalogService.createVenue(
        createVenueDto,
      );
      return response.status(HttpStatus.CREATED).json(createdVenue);
    } catch (error) {
      console.error(error);
      return response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: JSON.stringify(error),
      });
    }
  }

  @Post('/v1/events')
  @UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
  async createEvent(
    @Body() createEventDto: CreateEventDto,
    @Res() response: Response,
  ) {
    try {
      const createdEvent = await this.catalogService.createEvent(
        createEventDto,
      );
      return response.status(HttpStatus.CREATED).json(createdEvent);
    } catch (error) {
      console.error(error);
      return response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: JSON.stringify(error),
      });
    }
  }

  @Post('/v1/events/:eventId/seats')
  @UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
  async createSeats(
    @Param('eventId', ParseIntPipe) eventId: number,
    @Body() createSeatsDto: CreateSeatsDto,
    @Res() response: Response,
  ) {
    try {
      const createdSeats = await this.catalogService.createSeats(
        eventId,
        createSeatsDto.seats,
      );
      return response.status(HttpStatus.CREATED).json(createdSeats);
    } catch (error) {
      console.error(error);
      return response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: JSON.stringify(error),
      });
    }
  }

  @Get('/v1/events')
  @UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
  async listEvents(
    @Query() query: ListEventsQueryDto,
    @Res() response: Response,
  ) {
    try {
      const eventsList = await this.catalogService.listEvents({
        city: query.city,
        type: query.type,
        status: query.status,
      });
      return response.json(eventsList);
    } catch (error) {
      console.error(error);
      return response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: JSON.stringify(error),
      });
    }
  }

  @Get('/v1/events/:id')
  async getEvent(
    @Param('id', ParseIntPipe) id: number,
    @Res() response: Response,
  ) {
    try {
      const eventDetails = await this.catalogService.getEventById(id);
      if (!eventDetails) {
        return response.status(HttpStatus.NOT_FOUND).json({ error: 'not found' });
      }
      return response.json(eventDetails);
    } catch (error) {
      console.error(error);
      return response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: JSON.stringify(error),
      });
    }
  }
}
