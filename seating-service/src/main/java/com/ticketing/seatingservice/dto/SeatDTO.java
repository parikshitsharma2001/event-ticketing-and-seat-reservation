package com.ticketing.seatingservice.dto;

import com.ticketing.seatingservice.model.SeatStatus;
import com.ticketing.seatingservice.model.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDTO {
    private Long id;
    private Long eventId;
    private String seatNumber;
    private String rowNumber;
    private String section;
    private SeatType type;
    private BigDecimal price;
    private SeatStatus status;
    private Long reservedBy;
    private Long orderId;
    private LocalDateTime reservedAt;
    private LocalDateTime reservationExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
