import { IsOptional, IsString, IsInt, Min, IsNumber, IsArray, ArrayMinSize, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateSeatDto {
    @Type(() => Number)
    @IsInt()
    event_id!: number;

    @IsString()
    seat_code!: string;

    @IsOptional()
    @IsString()
    row?: string;

    @IsOptional()
    @Type(() => Number)
    @IsInt()
    @Min(0)
    number?: number;

    @IsOptional()
    @IsString()
    seat_type?: string;

    @Type(() => Number)
    @IsInt()
    @Min(0)
    price_cents!: number;
}

export class CreateSeatsDto {
    @IsArray()
    @ArrayMinSize(1)
    @ValidateNested({ each: true })
    @Type(() => CreateSeatDto)
    seats!: CreateSeatDto[];
}
