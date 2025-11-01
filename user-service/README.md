# User Service

## Overview
User Service handles user authentication, registration, and profile management for the Event Ticketing System. It follows the microservices pattern with its own dedicated database.

## Technologies
- Java 11
- Spring Boot 2.7.18
- PostgreSQL
- JWT Authentication
- Spring Security
- Spring Data JPA
- Micrometer (Prometheus metrics)
- Docker

## Features
- User registration with validation
- User authentication with JWT tokens
- User profile management (CRUD operations)
- User search and filtering
- Account status management (active, suspended, deleted)
- Password encryption with BCrypt
- Prometheus metrics for monitoring
- Health check endpoints

## API Endpoints

### Authentication
- `POST /v1/users/register` - Register a new user
- `POST /v1/users/login` - Authenticate and get JWT token

### User Management
- `GET /v1/users/{id}` - Get user by ID
- `GET /v1/users/username/{username}` - Get user by username
- `GET /v1/users/email/{email}` - Get user by email
- `GET /v1/users?search={term}&activeOnly={boolean}` - Search users
- `PUT /v1/users/{id}` - Update user profile
- `DELETE /v1/users/{id}` - Soft delete user
- `PATCH /v1/users/{id}/suspend` - Suspend user account
- `PATCH /v1/users/{id}/activate` - Activate user account

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);
```

## Configuration

### application.yml
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: mySecretKeyForJwtTokenGenerationAndValidationPurposesOnly
  expiration: 86400000 # 24 hours
```

## Running Locally

### Prerequisites
- Java 11
- Maven
- PostgreSQL

### Steps
1. Create database:
```bash
createdb userdb
```

2. Build the application:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

Or run the JAR:
```bash
java -jar target/user-service-1.0.0.jar
```

## Running with Docker

### Build Docker image:
```bash
docker build -t user-service:latest .
```

### Run container:
```bash
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/userdb \
  user-service:latest
```

## Running with Docker Compose
```bash
docker-compose up user-service
```

## Testing API

### Register User
```bash
curl -X POST http://localhost:8081/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "city": "New York"
  }'
```

### Login
```bash
curl -X POST http://localhost:8081/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john_doe",
    "password": "password123"
  }'
```

### Get User by ID
```bash
curl -X GET http://localhost:8081/v1/users/1
```

## Monitoring Metrics

The service exposes the following custom metrics:
- `user_registrations_total` - Total number of user registrations
- `user_logins_total` - Total number of user logins

Access metrics at: `http://localhost:8081/actuator/prometheus`

## Security
- Passwords are encrypted using BCrypt
- JWT tokens expire after 24 hours
- All sensitive endpoints should be secured (currently open for testing)

## Future Enhancements
- OAuth2 integration
- Role-based access control (RBAC)
- Email verification
- Password reset functionality
- Two-factor authentication
- Rate limiting
