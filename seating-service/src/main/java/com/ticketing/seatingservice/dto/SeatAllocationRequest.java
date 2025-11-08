package com.ticketing.seatingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAllocationRequest {
    
    @NotEmpty(message = "At least one seat ID is required")
    private List<Long> seatIds;
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
}