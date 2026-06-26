# Payment Wallet System

A production-ready digital payment wallet system built with Spring Boot 3.x, featuring JWT authentication, Redis caching, Kafka event streaming, and MySQL persistence.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Database | MySQL 8.0 + Spring Data JPA |
| Cache | Redis 7.2 |
| Messaging | Apache Kafka |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven |
| Container | Docker + Docker Compose |

## Features

- **User Registration & JWT Authentication** — Secure sign-up/login with BCrypt password hashing
- **Wallet Management** — Auto-created wallet per user, activate/deactivate support
- **Money Transfers** — Atomic transfers between wallets with pessimistic locking
- **Add Money** — Credit wallet balance with transaction records
- **Transaction History** — Paginated, sortable transaction ledger per wallet
- **Async Notifications** — Kafka events fired on every payment action
- **Redis Caching** — Wallet balance cached with TTL-based eviction
- **Global Exception Handling** — Consistent error responses across all endpoints
- **Swagger UI** — Interactive API documentation at `/swagger-ui.html`

## Project Structure

```
src/main/java/com/paymentwallet/
├── PaymentWalletApplication.java
├── config/
│   ├── SecurityConfig.java      # Spring Security + JWT filter chain
│   ├── RedisConfig.java         # Cache manager and Redis template
│   ├── KafkaConfig.java         # Topic definitions and listener factory
│   └── SwaggerConfig.java       # OpenAPI metadata and security scheme
├── controller/
│   ├── AuthController.java      # POST /register, POST /login
│   ├── WalletController.java    # GET /wallet/me, GET /wallet/{number}
│   └── TransactionController.java # POST /transfer, POST /add-money, GET /history
├── service/
│   ├── AuthService.java
│   ├── WalletService.java
│   └── TransactionService.java
├── entity/            # JPA entities: User, Wallet, Transaction
├── dto/               # Request/Response DTOs
├── repository/        # Spring Data JPA repositories
├── security/          # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
├── kafka/             # PaymentEvent, Producer, Consumer
├── exception/         # GlobalExceptionHandler + custom exceptions
└── enums/             # TransactionType, TransactionStatus
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

## Quick Start

### Option 1: Docker Compose (Recommended)

Starts all infrastructure (MySQL, Redis, Kafka, Kafka UI) plus the application:

```bash
# From the project root
docker-compose up -d
```

The app starts on `http://localhost:8080`.
Kafka UI is available at `http://localhost:8090`.

### Option 2: Run Locally with Infrastructure via Docker

1. Start only the infrastructure:

```bash
docker-compose up -d mysql redis zookeeper kafka
```

2. Run the Spring Boot app:

```bash
mvn spring-boot:run
```

### Option 3: Full local Maven build

```bash
mvn clean package
java -jar target/payment-wallet-system-1.0.0.jar
```

## API Documentation

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## API Reference

### Authentication

#### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phone": "+1234567890"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "john@example.com",
    "fullName": "John Doe",
    "walletNumber": "WLTABC123456789"
  }
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

### Wallet (requires Bearer token)

#### Get My Wallet
```http
GET /api/v1/wallet/me
Authorization: Bearer <token>
```

#### Get Wallet by Number
```http
GET /api/v1/wallet/{walletNumber}
Authorization: Bearer <token>
```

#### Deactivate / Activate Wallet
```http
PUT /api/v1/wallet/deactivate
PUT /api/v1/wallet/activate
Authorization: Bearer <token>
```

### Transactions (requires Bearer token)

#### Add Money
```http
POST /api/v1/transactions/add-money
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 500.00,
  "description": "Wallet top-up"
}
```

#### Transfer Money
```http
POST /api/v1/transactions/transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "destinationWalletNumber": "WLTXYZ987654321",
  "amount": 200.00,
  "description": "Payment for services"
}
```

#### Transaction History
```http
GET /api/v1/transactions/history?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

#### Get Transaction by Reference ID
```http
GET /api/v1/transactions/{referenceId}
Authorization: Bearer <token>
```

## Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient balance. Available: 100.00, Requested: 500.00",
  "path": "/api/v1/transactions/transfer",
  "timestamp": "2025-06-26T12:00:00"
}
```

Validation errors also include a `fieldErrors` array:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "fieldErrors": [
    { "field": "email", "message": "Invalid email format" },
    { "field": "password", "message": "Password must be at least 8 characters" }
  ]
}
```

## Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=AuthServiceTest

# With coverage report
mvn verify
```

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `app.jwt.secret` | (64-char hex) | HMAC-SHA256 signing key |
| `app.jwt.expiration` | 86400000 | Token TTL in milliseconds (24h) |
| `spring.cache.redis.time-to-live` | 600000 | Cache TTL in milliseconds (10m) |
| `app.kafka.topic.payment-notification` | payment-notifications | Notification topic |
| `app.kafka.topic.payment-completed` | payment-completed | Completion topic |

## Kafka Topics

| Topic | Partitions | Description |
|-------|-----------|-------------|
| `payment-notifications` | 3 | Fired immediately on every transaction |
| `payment-completed` | 3 | Fired after successful balance update |

## Database Schema

```
users
  id, full_name, email (unique), password, phone (unique), enabled, created_at, updated_at

wallets
  id, wallet_number (unique), balance, active, user_id (FK → users), created_at, updated_at

transactions
  id, reference_id (unique), amount, type (CREDIT/DEBIT/TRANSFER), status (PENDING/SUCCESS/FAILED),
  description, source_wallet_id (FK), destination_wallet_id (FK), balance_after_transaction, created_at
```
