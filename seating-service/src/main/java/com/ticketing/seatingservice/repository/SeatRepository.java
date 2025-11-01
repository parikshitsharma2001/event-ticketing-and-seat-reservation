package com.ticketing.seatingservice.repository;

import com.ticketing.seatingservice.model.Seat;
import com.ticketing.seatingservice.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByEventId(Long eventId);
    
    List<Seat> findByEventIdAndStatus(Long eventId, SeatStatus status);
    
    @Query("SELECT s FROM Seat s WHERE s.eventId = :eventId AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatsByEventId(@Param("eventId") Long eventId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    List<Seat> findByIdInWithLock(@Param("seatIds") List<Long> seatIds);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.eventId = :eventId AND s.status = :status")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") SeatStatus status);
    
    @Query("SELECT s FROM Seat s WHERE s.status = 'RESERVED' AND s.reservationExpiresAt < :now")
    List<Seat> findExpiredReservations(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.reservedBy = null, " +
           "s.reservedAt = null, s.reservationExpiresAt = null " +
           "WHERE s.status = 'RESERVED' AND s.reservationExpiresAt < :now")
    int releaseExpiredReservations(@Param("now") LocalDateTime now);
    
    List<Seat> findByOrderId(Long orderId);
    
    @Query("SELECT s FROM Seat s WHERE s.eventId = :eventId AND s.section = :section " +
           "AND s.status = 'AVAILABLE' ORDER BY s.rowNumber, s.seatNumber")
    List<Seat> findAvailableSeatsByEventIdAndSection(
            @Param("eventId") Long eventId, 
            @Param("section") String section);
}
