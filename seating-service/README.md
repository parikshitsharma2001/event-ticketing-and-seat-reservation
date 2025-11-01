# Seating Service

## Overview
Seating Service manages seat inventory, availability, reservations, and allocations for events in the ticketing system. It implements robust concurrency control to prevent double bookings and handles temporary seat reservations with automatic expiration.

## Technologies
- Java 11
- Spring Boot 2.7.18
- PostgreSQL
- Redis (for distributed locking)
- Redisson
- Spring Data JPA
- Optimistic & Pessimistic Locking
- Scheduled Tasks
- Micrometer (Prometheus metrics)
- Docker

## Features
- Real-time seat availability tracking
- Temporary seat reservations (15-minute TTL)
- Automatic expiration of reservations
- Pessimistic locking for concurrent requests
- Seat allocation for confirmed orders
- Seat release mechanism
- Seat blocking/unblocking for maintenance
- Comprehensive availability reports
- Prometheus metrics for monitoring

## API Endpoints

### Seat Availability
- `GET /v1/seats/availability?eventId={id}` - Get detailed availability for an event
- `GET /v1/seats?eventId={id}&status={status}` - Get seats by event and status
- `GET /v1/seats/{id}` - Get seat by ID
- `GET /v1/seats/order/{orderId}` - Get seats allocated to an order

### Seat Operations
- `POST /v1/seats/reserve` - Reserve seats temporarily (15 min hold)
- `POST /v1/seats/allocate` - Permanently allocate reserved seats
- `POST /v1/seats/release` - Release seats back to available
- `POST /v1/seats` - Create new seat
- `PATCH /v1/seats/{id}/block` - Block a seat
- `PATCH /v1/seats/{id}/unblock` - Unblock a seat

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Database Schema

### Seats Table
```sql
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    row_number VARCHAR(10) NOT NULL,
    section VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    reserved_by BIGINT,
    order_id BIGINT,
    reserved_at TIMESTAMP,
    reservation_expires_at TIMESTAMP,
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_event_status (event_id, status)
);
```

## Seat Status Flow
```
AVAILABLE → RESERVED → ALLOCATED
    ↓           ↓
    ← ← ← ← ← ←  (Release)
    
AVAILABLE → BLOCKED → AVAILABLE
```

### Status Definitions
- **AVAILABLE**: Seat is available for booking
- **RESERVED**: Temporarily held (15 min TTL)
- **ALLOCATED**: Permanently assigned to confirmed order
- **BLOCKED**: Blocked for maintenance/VIP/special purposes

## Concurrency Control

### Pessimistic Locking
The service uses pessimistic locking for seat reservations to prevent race conditions:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
List<Seat> findByIdInWithLock(@Param("seatIds") List<Long> seatIds);
```

### Optimistic Locking
Version field for detecting concurrent modifications:
```java
@Version
private Long version;
```

## Reservation Expiration

Automatic cleanup runs every minute to release expired reservations:

```java
@Scheduled(fixedRate = 60000)
public void releaseExpiredReservations() {
    // Releases seats where reservation_expires_at < NOW()
}
```

## Configuration

### application.yml
```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/seatingdb
    username: postgres
    password: postgres
  
  redis:
    host: localhost
    port: 6379
```

## Running Locally

### Prerequisites
- Java 11
- Maven
- PostgreSQL
- Redis

### Steps
1. Start PostgreSQL and Redis:
```bash
createdb seatingdb
redis-server
```

2. Build the application:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Running with Docker

### Build Docker image:
```bash
docker build -t seating-service:latest .
```

### Run with Docker Compose:
```bash
docker-compose up seating-service
```

## API Examples

### Get Seat Availability
```bash
curl -X GET "http://localhost:8082/v1/seats/availability?eventId=1"
```

Response:
```json
{
  "eventId": 1,
  "totalSeats": 100,
  "availableSeats": 85,
  "reservedSeats": 10,
  "allocatedSeats": 5,
  "availableSeatsList": [...],
  "availabilityBySection": {
    "A": 30,
    "B": 25,
    "C": 30
  }
}
```

### Reserve Seats
```bash
curl -X POST http://localhost:8082/v1/seats/reserve \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "eventId": 1,
    "seatIds": [1, 2, 3],
    "userId": 100
  }'
```

Response:
```json
{
  "success": true,
  "message": "Seats reserved successfully",
  "reservedSeats": [...],
  "totalPrice": 150.00,
  "expiresAt": "2025-10-26T15:30:00",
  "reservationId": "uuid-here"
}
```

### Allocate Seats
```bash
curl -X POST http://localhost:8082/v1/seats/allocate \
  -H "Content-Type: application/json" \
  -d '{
    "seatIds": [1, 2, 3],
    "orderId": 500
  }'
```

### Release Seats
```bash
curl -X POST http://localhost:8082/v1/seats/release \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

### Create Seat
```bash
curl -X POST http://localhost:8082/v1/seats \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "seatNumber": "A1",
    "rowNumber": "1",
    "section": "A",
    "type": "VIP",
    "price": 100.00
  }'
```

## Monitoring Metrics

Custom metrics exposed:
- `seat_reservations_total` - Total successful reservations
- `seat_reservations_failed` - Failed reservation attempts
- `expired_reservations_released` - Auto-released expired reservations

Access at: `http://localhost:8082/actuator/prometheus`

## Inter-Service Communication

### Integration with Order Service
1. Order Service calls `/reserve` to temporarily hold seats
2. Order Service processes payment
3. On success: Order Service calls `/allocate` to confirm seats
4. On failure: Seats auto-expire or Order Service calls `/release`

### Idempotency
- Support for `Idempotency-Key` header on reservation endpoints
- Prevents duplicate reservations from retry logic

## Error Handling

### SeatNotAvailableException (409 Conflict)
Thrown when:
- Seats are already reserved/allocated
- Attempting to allocate non-reserved seats

### ResourceNotFoundException (404 Not Found)
Thrown when:
- Seats don't exist
- Event has no seats

## Performance Considerations

1. **Database Indexes**: Created on event_id and status for fast queries
2. **Pessimistic Locking**: Used only during reservation to minimize lock duration
3. **Batch Operations**: Multiple seats processed in single transaction
4. **Scheduled Cleanup**: Runs every minute to free expired reservations

## Future Enhancements
- Seat map visualization
- Dynamic pricing based on demand
- Waitlist for sold-out events
- Bulk seat operations API
- Seat hold extension API
- Integration with message queue for event-driven architecture
