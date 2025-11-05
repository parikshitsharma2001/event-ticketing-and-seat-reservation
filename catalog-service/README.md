# Catalog Service

## Overview

Catalog Service manages events, venues, and seats for the Event Ticketing System. It provides CRUD operations, event discovery, and seat management. Built to be modular and Docker-ready, it is intended to be used alongside other services (seating, order, payment, notification).

## Technologies

* Node.js 21
* NestJS 10 (or plain TypeScript modules compatible with Nest structure)
* TypeORM
* PostgreSQL
* prom-client (optional for metrics)
* Docker

## Features

* CRUD operations for venues and events
* Bulk seat creation for events
* Event filtering & search by city, type, status (`ON_SALE`, `SOLD_OUT`, `CANCELLED`)
* Join event listings with venue metadata
* PostgreSQL persistence via TypeORM (entities: Venue, Event, Seat)
* Command / Query repository separation (CQRS style)
* Health and basic metrics endpoint support
* Docker-based deployment

## API Endpoints

### Venues

* `POST /v1/venues` - Create a venue
* `GET /health` - Health check

### Events

* `POST /v1/events` - Create an event
* `POST /v1/events/:eventId/seats` - Create multiple seats for an event (bulk)
* `GET /v1/events` - List events (optional filters: `city`, `type`, `status`)
* `GET /v1/events/:id` - Get event details including seats

## Database Schema

### Venues Table

```sql
CREATE TABLE venues (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  city TEXT,
  address TEXT,
  capacity INT
);
```

### Events Table

```sql
CREATE TABLE events (
  id SERIAL PRIMARY KEY,
  venue_id INT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  type TEXT,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP,
  status TEXT NOT NULL DEFAULT 'ON_SALE',
  description TEXT
);
```

### Seats Table

```sql
CREATE TABLE seats (
  id SERIAL PRIMARY KEY,
  event_id INT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  seat_code TEXT NOT NULL,
  row TEXT,
  number INT,
  seat_type TEXT,
  price_cents INT NOT NULL,
  UNIQUE (event_id, seat_code)
);
```

## Configuration

### Environment Variables

| Variable          | Description              |
| ----------------- | ------------------------ |
| DATABASE_HOST     | PostgreSQL hostname      |
| DATABASE_PORT     | PostgreSQL port          |
| DATABASE_USERNAME | PostgreSQL username      |
| DATABASE_PASSWORD | PostgreSQL password      |
| DATABASE_NAME     | PostgreSQL database name |
| PORT              | Service port             |

> NOTE: Do **not** commit secrets. Provide these values via environment or Docker Compose.

## Running Locally

### Prerequisites

* Node.js 21+
* npm
* PostgreSQL (or run via Docker)

### Steps

1. Install dependencies:

```bash
npm install
```

2. Build the application:

```bash
npm run build
```

3. Run the application:

```bash
npm run start
```

## Running with Docker

### Build Docker image:

```bash
docker build -t catalog-service:latest .
```

### Run container:

```bash
docker run -p 4001:4001 \
  -e DATABASE_HOST=host.docker.internal \
  -e DATABASE_PORT=5432 \
  -e DATABASE_NAME=catalogdb \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  catalog-service:latest
```

### Running with Docker Compose

Add a `catalogdb` service (postgres image) and a `catalog-service` entry in your root `docker-compose.yml` and run:

```bash
docker-compose up catalogdb catalog-service
```

## Testing API

### 1. Create a Venue

```bash
curl -X POST http://localhost:4001/v1/venues \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Hall",
    "city": "Mumbai",
    "address": "123 Main St",
    "capacity": 5000
  }'
```

### 2. Create an Event

```bash
curl -X POST http://localhost:4001/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "venue_id": 1,
    "title": "Indie Music Fest",
    "type": "Music",
    "start_time": "2025-12-10T18:00:00Z",
    "end_time": "2025-12-10T22:00:00Z",
    "status": "ON_SALE",
    "description": "An evening of indie bands."
  }'
```

### 3. Create Seats (Bulk)

```bash
curl --location 'http://localhost:4001/v1/events/1/seats' \
--header 'Content-Type: application/json' \
--data '{
    "seats": [
        {
            "event_id": 1,
            "seat_code": "A1",
            "row": "A",
            "number": 1,
            "seat_type": "VIP",
            "price_cents": 5000
        },
        {
            "event_id": 2,
            "seat_code": "A2",
            "row": "A",
            "number": 2,
            "seat_type": "VIP",
            "price_cents": 5000
        }
    ]
}'
```

### 4. List Events (with filters)

```bash
curl -X GET "http://localhost:4001/v1/events?city=Mumbai&type=Music&status=ON_SALE"
```

### 5. Get Event by ID

```bash
curl -X GET http://localhost:4001/v1/events/1
```

### 6. Health Check

```bash
curl -X GET http://localhost:4001/health
```

## Monitoring Metrics

* The service exposes a `/health` endpoint for basic liveness/readiness checks.
* You can add `prom-client` metrics (response times, counts) and expose `/metrics` for Prometheus scraping.

## Security

* Validate and sanitize inputs before persisting.
* Protect admin endpoints (create/update/delete) with auth in production.
* Use TLS for inter-service communication and protect DB credentials via secrets.

## Future Enhancements

* Pagination for `GET /v1/events`
* Venue endpoints for update/list/delete
* Event publishing (webhooks) when status changes (e.g., `CANCELLED`)
* Prometheus metrics + Grafana dashboards
* Integration with search engine (Elasticsearch) for advanced filtering and full-text search
* Input validation via DTOs and class-validator (if using NestJS)
