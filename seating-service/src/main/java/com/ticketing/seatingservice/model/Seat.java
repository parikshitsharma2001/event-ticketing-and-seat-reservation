package com.ticketing.seatingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_event_status", columnList = "event_id,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    
    @NotNull
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;
    
    @NotNull
    @Column(name = "row_number", nullable = false)
    private String rowNumber;
    
    @NotNull
    @Column(name = "section", nullable = false)
    private String section;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type = SeatType.REGULAR;
    
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;
    
    @Column(name = "reserved_by")
    private Long reservedBy; // User ID who reserved
    
    @Column(name = "order_id")
    private Long orderId; // Order ID if allocated
    
    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;
    
    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;
    
    @Version
    @Column(name = "version")
    private Long version; // For optimistic locking
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
