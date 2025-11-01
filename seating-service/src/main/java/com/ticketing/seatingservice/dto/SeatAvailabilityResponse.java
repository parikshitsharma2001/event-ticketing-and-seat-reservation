package com.ticketing.seatingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailabilityResponse {
    private Long eventId;
    private Long totalSeats;
    private Long availableSeats;
    private Long reservedSeats;
    private Long allocatedSeats;
    private List<SeatDTO> availableSeatsList;
    private Map<String, Long> availabilityBySection;
}
