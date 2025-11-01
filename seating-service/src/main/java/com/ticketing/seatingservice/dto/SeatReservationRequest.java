package com.ticketing.seatingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    @NotEmpty(message = "At least one seat ID is required")
    private List<Long> seatIds;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    private String orderId; // For idempotency
}
