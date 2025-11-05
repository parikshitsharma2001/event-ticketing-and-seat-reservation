import {
  IsArray,
  ArrayNotEmpty,
  IsUUID,
  IsNumber,
  IsOptional,
  IsString,
} from 'class-validator';

export class CreateOrderDto {
  @IsUUID('4', { message: 'user_public_id must be a valid UUID' })
  user_public_id!: string;

  @IsNumber({}, { message: 'event_id must be a number' })
  event_id!: number;

  @IsArray({ message: 'seats must be an array' })
  @ArrayNotEmpty({ message: 'seats cannot be empty' })
  seats!: number[];
}
