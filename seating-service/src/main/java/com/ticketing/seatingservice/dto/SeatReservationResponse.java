package com.ticketing.seatingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationResponse {
    private boolean success;
    private String message;
    private List<SeatDTO> reservedSeats;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private String reservationId;
}
