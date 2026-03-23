# Crypto Portfolio Tracker

A production-style backend system for tracking cryptocurrency portfolios, including holdings, live valuations, and profit/loss calculations.

Built with Java 21, Spring Boot, PostgreSQL, JWT authentication, Docker, and external market data integration.

---

## Overview

This application enables users to:

* securely register and authenticate
* manage crypto holdings across multiple assets
* track portfolio value and performance in real time
* calculate profit/loss based on market data

The project focuses on **real-world backend design**, including stateless authentication, database migrations, and clean service architecture.

---

## Architecture

```
Controller → Service → Repository → Database
             ↓
        Security (JWT Filter)
             ↓
     External API (CoinGecko)
```

### Design Principles

* **Stateless authentication** (JWT)
* **Layered architecture** for separation of concerns
* **Database versioning** via Flyway
* **Containerized environment** using Docker
* **External API integration** for live pricing

---

## Features

### Authentication

* User registration and login
* BCrypt password hashing
* JWT-based stateless security

### Portfolio Management

* Create, update, and delete holdings
* Per-user data isolation
* Average buy price tracking

### Market Integration

* Live price lookup via CoinGecko API

### Portfolio Analytics

* Total invested value
* Current portfolio value
* Profit / loss calculation

### Infrastructure

* PostgreSQL persistence
* Docker-based local environment
* Swagger/OpenAPI documentation
* Global exception handling and validation

---

## Tech Stack

| Layer          | Technology             |
| -------------- | ---------------------- |
| Backend        | Java 21, Spring Boot 3 |
| Security       | Spring Security + JWT  |
| Database       | PostgreSQL             |
| ORM            | Hibernate / JPA        |
| Migrations     | Flyway                 |
| Infrastructure | Docker, Docker Compose |
| API Docs       | Swagger / OpenAPI      |
| External Data  | CoinGecko API          |

---

## Running the Project

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the backend

```bash
./mvnw spring-boot:run
```

---

## Configuration

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cryptodb
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=validate

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## API Endpoints

### Auth (Public)

* `POST /api/auth/register`
* `POST /api/auth/login`

---

### Portfolio (Protected)

* `GET /api/portfolio/holdings`
* `POST /api/portfolio/holdings`
* `PUT /api/portfolio/holdings/{id}`
* `DELETE /api/portfolio/holdings/{id}`
* `GET /api/portfolio/summary`

---

### Market (Public)

* `GET /api/market/prices`

---

## Example Response

```json
{
  "id": 1,
  "symbol": "BTC",
  "name": "Bitcoin",
  "quantity": 0.50000000,
  "averageBuyPriceUsd": 45000.00,
  "currentPriceUsd": 60000.00,
  "investedValueUsd": 22500.00,
  "currentValueUsd": 30000.00,
  "profitLossUsd": 7500.00
}
```

---

## Database Schema

Core tables:

* users
* user_roles
* coins
* holdings
* transactions

Managed via Flyway migrations:

```
src/main/resources/db/migration
```

---

## Key Engineering Decisions

| Decision                                   | Reason                               |
| ------------------------------------------ | ------------------------------------ |
| JWT authentication                         | scalable, stateless API design       |
| Flyway migrations                          | safe and controlled schema evolution |
| Hibernate validation (`ddl-auto=validate`) | prevents silent schema drift         |
| Dockerized database                        | consistent local development         |
| Layered architecture                       | maintainability and testability      |

---

## Future Improvements

* Portfolio aggregation endpoint optimization
* Integration testing (Testcontainers)
* Redis caching for price data
* Pagination and filtering
* Rate limiting and API hardening

---

## Author

Kevinas Ramoska

---

## Status

Backend core complete. Actively expanding features and improving production readiness.
