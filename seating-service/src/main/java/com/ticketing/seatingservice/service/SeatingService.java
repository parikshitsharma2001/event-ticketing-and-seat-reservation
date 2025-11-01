package com.ticketing.seatingservice.service;

import com.ticketing.seatingservice.dto.*;
import com.ticketing.seatingservice.exception.ResourceNotFoundException;
import com.ticketing.seatingservice.exception.SeatNotAvailableException;
import com.ticketing.seatingservice.model.Seat;
import com.ticketing.seatingservice.model.SeatStatus;
import com.ticketing.seatingservice.repository.SeatRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatingService {
    
    private final SeatRepository seatRepository;
    private final ModelMapper modelMapper;
    private final MeterRegistry meterRegistry;
    
    private static final int RESERVATION_TTL_MINUTES = 15;
    
    @Transactional(readOnly = true)
    public SeatAvailabilityResponse getSeatAvailability(Long eventId) {
        log.info("Fetching seat availability for event: {}", eventId);
        
        List<Seat> allSeats = seatRepository.findByEventId(eventId);
        
        if (allSeats.isEmpty()) {
            throw new ResourceNotFoundException("No seats found for event: " + eventId);
        }
        
        List<Seat> availableSeats = allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.toList());
        
        long reservedCount = allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.RESERVED)
                .count();
        
        long allocatedCount = allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.ALLOCATED)
                .count();
        
        Map<String, Long> availabilityBySection = availableSeats.stream()
                .collect(Collectors.groupingBy(Seat::getSection, Collectors.counting()));
        
        return SeatAvailabilityResponse.builder()
                .eventId(eventId)
                .totalSeats((long) allSeats.size())
                .availableSeats((long) availableSeats.size())
                .reservedSeats(reservedCount)
                .allocatedSeats(allocatedCount)
                .availableSeatsList(availableSeats.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .availabilityBySection(availabilityBySection)
                .build();
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SeatReservationResponse reserveSeats(SeatReservationRequest request) {
        log.info("Reserving seats for event: {}, seatIds: {}, userId: {}", 
                request.getEventId(), request.getSeatIds(), request.getUserId());
        
        // Fetch seats with pessimistic locking to prevent concurrent modifications
        List<Seat> seats = seatRepository.findByIdInWithLock(request.getSeatIds());
        
        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("Some seats not found");
        }
        
        // Validate all seats belong to the same event
        boolean allSameEvent = seats.stream()
                .allMatch(seat -> seat.getEventId().equals(request.getEventId()));
        
        if (!allSameEvent) {
            throw new IllegalArgumentException("All seats must belong to the same event");
        }
        
        // Check if all seats are available
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> seat.getStatus() != SeatStatus.AVAILABLE)
                .collect(Collectors.toList());
        
        if (!unavailableSeats.isEmpty()) {
            Counter.builder("seat_reservations_failed")
                    .description("Failed seat reservations")
                    .tag("reason", "seats_unavailable")
                    .register(meterRegistry)
                    .increment();
            
            throw new SeatNotAvailableException(
                    "Seats are not available: " + 
                    unavailableSeats.stream()
                            .map(s -> s.getSeatNumber())
                            .collect(Collectors.joining(", "))
            );
        }
        
        // Reserve the seats
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(RESERVATION_TTL_MINUTES);
        String reservationId = UUID.randomUUID().toString();
        
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.RESERVED);
            seat.setReservedBy(request.getUserId());
            seat.setReservedAt(now);
            seat.setReservationExpiresAt(expiresAt);
        });
        
        List<Seat> reservedSeats = seatRepository.saveAll(seats);
        
        BigDecimal totalPrice = reservedSeats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Counter.builder("seat_reservations_total")
                .description("Total seat reservations")
                .register(meterRegistry)
                .increment();
        
        log.info("Successfully reserved {} seats for user: {}", reservedSeats.size(), request.getUserId());
        
        return SeatReservationResponse.builder()
                .success(true)
                .message("Seats reserved successfully")
                .reservedSeats(reservedSeats.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .totalPrice(totalPrice)
                .expiresAt(expiresAt)
                .reservationId(reservationId)
                .build();
    }
    
    @Transactional
    public void allocateSeats(SeatAllocationRequest request) {
        log.info("Allocating seats: {} for order: {}", request.getSeatIds(), request.getOrderId());
        
        List<Seat> seats = seatRepository.findByIdInWithLock(request.getSeatIds());
        
        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("Some seats not found");
        }
        
        // Validate seats are in RESERVED status
        List<Seat> invalidSeats = seats.stream()
                .filter(seat -> seat.getStatus() != SeatStatus.RESERVED)
                .collect(Collectors.toList());
        
        if (!invalidSeats.isEmpty()) {
            throw new SeatNotAvailableException("Some seats are not in reserved status");
        }
        
        // Allocate the seats
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.ALLOCATED);
            seat.setOrderId(request.getOrderId());
            seat.setReservationExpiresAt(null);
        });
        
        seatRepository.saveAll(seats);
        
        log.info("Successfully allocated {} seats for order: {}", seats.size(), request.getOrderId());
    }
    
    @Transactional
    public void releaseSeats(List<Long> seatIds) {
        log.info("Releasing seats: {}", seatIds);
        
        List<Seat> seats = seatRepository.findByIdInWithLock(seatIds);
        
        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setReservedBy(null);
            seat.setOrderId(null);
            seat.setReservedAt(null);
            seat.setReservationExpiresAt(null);
        });
        
        seatRepository.saveAll(seats);
        
        log.info("Successfully released {} seats", seats.size());
    }
    
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void releaseExpiredReservations() {
        log.info("Checking for expired reservations");
        
        LocalDateTime now = LocalDateTime.now();
        int releasedCount = seatRepository.releaseExpiredReservations(now);
        
        if (releasedCount > 0) {
            log.info("Released {} expired reservations", releasedCount);
            
            Counter.builder("expired_reservations_released")
                    .description("Number of expired reservations released")
                    .register(meterRegistry)
                    .increment(releasedCount);
        }
    }
    
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatsByEventId(Long eventId, String status) {
        log.info("Fetching seats for event: {}, status: {}", eventId, status);
        
        List<Seat> seats;
        if (status != null && !status.isEmpty()) {
            SeatStatus seatStatus = SeatStatus.valueOf(status.toUpperCase());
            seats = seatRepository.findByEventIdAndStatus(eventId, seatStatus);
        } else {
            seats = seatRepository.findByEventId(eventId);
        }
        
        return seats.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SeatDTO getSeatById(Long seatId) {
        log.info("Fetching seat by ID: {}", seatId);
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));
        return convertToDTO(seat);
    }
    
    @Transactional(readOnly = true)
    public List<SeatDTO> getSeatsByOrderId(Long orderId) {
        log.info("Fetching seats for order: {}", orderId);
        List<Seat> seats = seatRepository.findByOrderId(orderId);
        return seats.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public SeatDTO createSeat(SeatDTO seatDTO) {
        log.info("Creating new seat for event: {}", seatDTO.getEventId());
        
        Seat seat = Seat.builder()
                .eventId(seatDTO.getEventId())
                .seatNumber(seatDTO.getSeatNumber())
                .rowNumber(seatDTO.getRowNumber())
                .section(seatDTO.getSection())
                .type(seatDTO.getType())
                .price(seatDTO.getPrice())
                .status(SeatStatus.AVAILABLE)
                .build();
        
        Seat savedSeat = seatRepository.save(seat);
        log.info("Seat created with ID: {}", savedSeat.getId());
        
        return convertToDTO(savedSeat);
    }
    
    @Transactional
    public void blockSeat(Long seatId) {
        log.info("Blocking seat: {}", seatId);
        
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));
        
        seat.setStatus(SeatStatus.BLOCKED);
        seatRepository.save(seat);
        
        log.info("Seat blocked: {}", seatId);
    }
    
    @Transactional
    public void unblockSeat(Long seatId) {
        log.info("Unblocking seat: {}", seatId);
        
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));
        
        seat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
        
        log.info("Seat unblocked: {}", seatId);
    }
    
    private SeatDTO convertToDTO(Seat seat) {
        return modelMapper.map(seat, SeatDTO.class);
    }
}
