package com.ticketing.seatingservice.controller;

import com.ticketing.seatingservice.dto.*;
import com.ticketing.seatingservice.service.SeatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/seats")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SeatingController {
    
    private final SeatingService seatingService;
    
    @GetMapping("/availability")
    public ResponseEntity<SeatAvailabilityResponse> getSeatAvailability(
            @RequestParam Long eventId) {
        log.info("GET /v1/seats/availability - eventId: {}", eventId);
        SeatAvailabilityResponse response = seatingService.getSeatAvailability(eventId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<SeatDTO>> getSeats(
            @RequestParam Long eventId,
            @RequestParam(required = false) String status) {
        log.info("GET /v1/seats - eventId: {}, status: {}", eventId, status);
        List<SeatDTO> seats = seatingService.getSeatsByEventId(eventId, status);
        return ResponseEntity.ok(seats);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SeatDTO> getSeatById(@PathVariable Long id) {
        log.info("GET /v1/seats/{}", id);
        SeatDTO seat = seatingService.getSeatById(id);
        return ResponseEntity.ok(seat);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<SeatDTO>> getSeatsByOrderId(@PathVariable Long orderId) {
        log.info("GET /v1/seats/order/{}", orderId);
        List<SeatDTO> seats = seatingService.getSeatsByOrderId(orderId);
        return ResponseEntity.ok(seats);
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<SeatReservationResponse> reserveSeats(
            @Valid @RequestBody SeatReservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        log.info("POST /v1/seats/reserve - request: {}, idempotencyKey: {}", request, idempotencyKey);
        
        SeatReservationResponse response = seatingService.reserveSeats(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/allocate")
    public ResponseEntity<Void> allocateSeats(
            @Valid @RequestBody SeatAllocationRequest request) {
        log.info("POST /v1/seats/allocate - request: {}", request);
        
        seatingService.allocateSeats(request);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/release")
    public ResponseEntity<Void> releaseSeats(@RequestBody List<Long> seatIds) {
        log.info("POST /v1/seats/release - seatIds: {}", seatIds);
        
        seatingService.releaseSeats(seatIds);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping
    public ResponseEntity<SeatDTO> createSeat(@Valid @RequestBody SeatDTO seatDTO) {
        log.info("POST /v1/seats - creating seat: {}", seatDTO);
        
        SeatDTO createdSeat = seatingService.createSeat(seatDTO);
        return new ResponseEntity<>(createdSeat, HttpStatus.CREATED);
    }
    
    @PatchMapping("/{id}/block")
    public ResponseEntity<Void> blockSeat(@PathVariable Long id) {
        log.info("PATCH /v1/seats/{}/block", id);
        
        seatingService.blockSeat(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockSeat(@PathVariable Long id) {
        log.info("PATCH /v1/seats/{}/unblock", id);
        
        seatingService.unblockSeat(id);
        return ResponseEntity.ok().build();
    }
}
