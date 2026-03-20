# Crypto Portfolio Tracker

A production-style backend application for tracking cryptocurrency holdings, current valuations, and portfolio profit/loss.

Built with Java, Spring Boot, PostgreSQL, JWT authentication, Docker, and CoinGecko market data integration.

---

## Features

- User registration and login with JWT authentication
- Secure portfolio management per user
- Add, update, and delete crypto holdings
- Live market price lookup
- Portfolio analytics:
  - invested value
  - current value
  - profit/loss
- PostgreSQL persistence
- Docker-based local setup
- Swagger/OpenAPI documentation
- Validation and global exception handling

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Docker / Docker Compose
- Swagger / OpenAPI
- CoinGecko API

---

## API Endpoints

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### Portfolio
- `GET /api/portfolio/holdings`
- `POST /api/portfolio/holdings`
- `PUT /api/portfolio/holdings/{id}`
- `DELETE /api/portfolio/holdings/{id}`
- `GET /api/portfolio/summary`

### Market
- `GET /api/market/prices`

---

## Example Holding Response

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
