package com.ticketing.seatingservice.model;

public enum SeatStatus {
    AVAILABLE,      // Seat is available for booking
    RESERVED,       // Seat is temporarily reserved (15 min hold)
    ALLOCATED,      // Seat is permanently allocated to confirmed order
    BLOCKED         // Seat is blocked (maintenance, VIP, etc.)
}
