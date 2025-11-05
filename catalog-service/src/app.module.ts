import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CatalogModule } from './catalog/catalog.module';
import { DataSourceOptions } from 'typeorm';
import { initDb } from './config/database';
import { Venue } from './catalog/entities/venue';
import { Seat } from './catalog/entities/seat';
import { Event } from './catalog/entities/event';

@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      useFactory: async (): Promise<DataSourceOptions> => {
        await initDb();

        return {
          type: 'postgres',
          host: process.env.DATABASE_HOST,
          port: parseInt(process.env.DATABASE_PORT!),
          username: process.env.DATABASE_USERNAME,
          password: process.env.DATABASE_PASSWORD,
          database: process.env.DATABASE_NAME,
          entities: [Event, Venue, Seat],
          synchronize: false,
          logging: false,
        } as DataSourceOptions;
      },
    }),
    CatalogModule,
  ],
})

export class AppModule { }
