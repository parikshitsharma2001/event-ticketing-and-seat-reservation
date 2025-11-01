# Event Ticketing System - Microservices

## Overview
This project implements a microservices-based Event Ticketing & Seat Reservation System as part of the scalable systems assignment. The system follows microservices best practices with database-per-service pattern, inter-service communication, and comprehensive monitoring.

## Architecture

### Services Implemented
1. **User Service** (Port 8081) - Authentication and user management
2. **Seating Service** (Port 8082) - Seat inventory, reservations, and allocations

### Additional Services (To Be Implemented)
3. **Catalog Service** - Event and venue management
4. **Order Service** - Order processing and ticket generation
5. **Payment Service** - Payment processing with idempotency

## Technology Stack

### Backend
- **Java 11** - Programming language
- **Spring Boot 2.7.18** - Application framework
- **Spring Data JPA** - Data persistence
- **PostgreSQL** - Relational database
- **Redis** - Distributed caching and locking
- **JWT** - Authentication tokens

### DevOps & Monitoring
- **Docker** - Containerization
- **Docker Compose** - Local orchestration
- **Kubernetes/Minikube** - Container orchestration
- **Prometheus** - Metrics collection
- **Grafana** - Visualization dashboards

## Project Structure

```
.
├── user-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/ticketing/userservice/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   ├── model/
│   │       │   ├── dto/
│   │       │   ├── security/
│   │       │   ├── config/
│   │       │   └── exception/
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── README.md
│
├── seating-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/ticketing/seatingservice/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   ├── model/
│   │       │   ├── dto/
│   │       │   ├── config/
│   │       │   └── exception/
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   ├── pom.xml
│   └── README.md
│
├── docker-compose.yml
├── prometheus.yml
└── README.md
```

## Database Design

### Database-Per-Service Pattern
Each microservice has its own dedicated database:

1. **userdb** - User Service database
   - Users table
   
2. **seatingdb** - Seating Service database
   - Seats table

## Key Features

### User Service
- User registration with validation
- JWT-based authentication
- Profile management (CRUD)
- User search and filtering
- Account status management
- Password encryption

### Seating Service
- Real-time seat availability
- Temporary reservations (15-min TTL)
- Automatic expiration cleanup
- Pessimistic locking for concurrency
- Seat allocation for orders
- Comprehensive availability reports

## Running the Application

### Prerequisites
- Java 11
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (if running locally)
- Redis (if running locally)

### Option 1: Docker Compose (Recommended)

1. Build and start all services:
```bash
docker-compose up --build
```

2. Access services:
- User Service: http://localhost:8081
- Seating Service: http://localhost:8082
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

3. Stop services:
```bash
docker-compose down
```

### Option 2: Local Development

1. Start databases:
```bash
# PostgreSQL
createdb userdb
createdb seatingdb

# Redis
redis-server
```

2. Build and run User Service:
```bash
cd user-service
mvn clean install
mvn spring-boot:run
```

3. Build and run Seating Service (in new terminal):
```bash
cd seating-service
mvn clean install
mvn spring-boot:run
```

## API Documentation

### User Service Endpoints
- `POST /v1/users/register` - Register new user
- `POST /v1/users/login` - Authenticate user
- `GET /v1/users/{id}` - Get user by ID
- `PUT /v1/users/{id}` - Update user
- `DELETE /v1/users/{id}` - Delete user

### Seating Service Endpoints
- `GET /v1/seats/availability?eventId={id}` - Get availability
- `POST /v1/seats/reserve` - Reserve seats
- `POST /v1/seats/allocate` - Allocate seats
- `POST /v1/seats/release` - Release seats

For detailed API documentation, see individual service README files.

## Testing the System

### 1. Register a User
```bash
curl -X POST http://localhost:8081/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 2. Check Seat Availability
```bash
curl -X GET "http://localhost:8082/v1/seats/availability?eventId=1"
```

### 3. Reserve Seats
```bash
curl -X POST http://localhost:8082/v1/seats/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "seatIds": [1, 2],
    "userId": 1
  }'
```

## Monitoring

### Health Checks
- User Service: http://localhost:8081/actuator/health
- Seating Service: http://localhost:8082/actuator/health

### Metrics
- User Service Metrics: http://localhost:8081/actuator/prometheus
- Seating Service Metrics: http://localhost:8082/actuator/prometheus

### Custom Metrics

**User Service:**
- `user_registrations_total` - Total registrations
- `user_logins_total` - Total logins

**Seating Service:**
- `seat_reservations_total` - Successful reservations
- `seat_reservations_failed` - Failed reservations
- `expired_reservations_released` - Auto-released seats

### Prometheus Queries
Access Prometheus at http://localhost:9090 and try:
- `rate(user_registrations_total[5m])`
- `seat_reservations_total`
- `seat_reservations_failed`

## Kubernetes Deployment

### Prerequisites
- Minikube installed
- kubectl configured

### Deploy to Minikube

1. Start Minikube:
```bash
minikube start
```

2. Build images in Minikube's Docker daemon:
```bash
eval $(minikube docker-env)
docker build -t user-service:latest ./user-service
docker build -t seating-service:latest ./seating-service
```

3. Create Kubernetes manifests (see k8s/ directory)

4. Apply configurations:
```bash
kubectl apply -f k8s/
```

5. Access services:
```bash
minikube service user-service --url
minikube service seating-service --url
```

## Inter-Service Communication

### Reservation Flow
1. User authenticated via User Service
2. Seating Service reserves seats (15-min hold)
3. Order Service creates order with reserved seats
4. Payment Service processes payment
5. On success: Seating Service allocates seats
6. On failure: Seats auto-expire after 15 minutes

### Data Consistency
- Each service owns its data
- No direct database access between services
- Communication via REST APIs
- Eventual consistency where appropriate

## Design Patterns Used

1. **Database-per-Service**: Each service has dedicated database
2. **API Gateway Pattern**: (To be implemented)
3. **Saga Pattern**: For distributed transactions
4. **Circuit Breaker**: For resilience
5. **Repository Pattern**: Data access layer
6. **DTO Pattern**: Data transfer between layers

## Security Considerations

1. **Authentication**: JWT tokens with expiration
2. **Password Storage**: BCrypt hashing
3. **SQL Injection**: Parameterized queries via JPA
4. **CORS**: Configured for cross-origin requests
5. **Rate Limiting**: (To be implemented)

## Future Enhancements

### Technical
- [ ] API Gateway (Spring Cloud Gateway)
- [ ] Service Discovery (Eureka)
- [ ] Config Server (Spring Cloud Config)
- [ ] Message Queue (RabbitMQ/Kafka)
- [ ] Circuit Breaker (Resilience4j)
- [ ] Distributed Tracing (Zipkin/Jaeger)
- [ ] Log Aggregation (ELK Stack)

### Business Features
- [ ] Email notifications
- [ ] Payment integration
- [ ] Refund processing
- [ ] Event recommendations
- [ ] User preferences
- [ ] Loyalty program
- [ ] Mobile app support

## Troubleshooting

### Common Issues

1. **Port already in use**:
```bash
# Kill process on port
lsof -ti:8081 | xargs kill -9
```

2. **Database connection failed**:
```bash
# Check PostgreSQL is running
pg_isalive
```

3. **Redis connection failed**:
```bash
# Check Redis is running
redis-cli ping
```

4. **Docker build fails**:
```bash
# Clean Docker cache
docker system prune -a
```

## Contributing

1. Create feature branch
2. Implement changes with tests
3. Update documentation
4. Submit pull request

## License

This project is created for educational purposes as part of a scalable systems assignment.

## Authors

- [Your Team Name]
- [Team Members]

## Acknowledgments

- Spring Boot Documentation
- Microservices Patterns by Chris Richardson
- Assignment guidelines and requirements
