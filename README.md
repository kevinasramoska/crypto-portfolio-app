# Crypto Portfolio Tracker

A full-stack crypto portfolio tracker for recording buy/sell transactions, viewing current holdings, tracking portfolio value, and calculating realised/unrealised profit and loss.

The app is transaction-driven: users record `BUY` and `SELL` transactions, and the backend updates holdings, calculates summaries, stores transaction history, and creates portfolio snapshots.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 26, Spring Boot 4.1.0 |
| API | Spring MVC REST controllers |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL |
| ORM | Spring Data JPA, Hibernate |
| Migrations | Flyway |
| Market data | CoinGecko |
| Backend tests | JUnit 5, Spring Boot Test, MockMvc, Mockito, H2 |
| Frontend | Next.js 16.2.9, React 19.2.7, TypeScript |
| Styling | Tailwind CSS |
| Local infrastructure | Docker Compose, PostgreSQL, pgAdmin |

## Features

### Authentication

- Register and log in with email/password
- Password hashing with BCrypt
- JWT-based stateless authentication
- Protected portfolio and transaction endpoints
- Stale frontend auth state is cleared when protected API calls return `401` or `403`

### Portfolio Tracking

- Record crypto `BUY` and `SELL` transactions
- Prevent selling more than the current holding quantity
- Maintain current holdings per user
- Calculate average buy price
- Calculate current value, invested value, and profit/loss
- Flag holdings whose symbols do not have supported market data

### Dashboard

- Watchlist price cards
- Portfolio summary cards
- Holdings table
- Transaction creation form with supported coin presets and custom/manual fallback
- Transaction summary cards
- Transaction history table
- Portfolio performance history by `7d`, `30d`, or `90d`
- Independent loading and error states for portfolio, transactions, and performance history
- Clear empty states for first-run accounts and missing snapshot history
- Clear unsupported/no-market-data states instead of misleading zero-value display

### Market Data

- Public price lookup endpoint
- CoinGecko integration for supported symbols
- In-memory price cache with a 60-second TTL
- Configurable CoinGecko-compatible base URL through `CRYPTO_API_BASE_URL`

Supported market symbols are currently:

```text
BTC, ETH, SOL, ADA, XRP, DOGE, DOT, AVAX, MATIC, LINK, LTC, BNB
```

## Project Structure

```text
.
├── backend/
│   └── crypto-portfolio-backend/
│       ├── pom.xml
│       ├── docker-compose.yml
│       └── src/
│           ├── main/java/com/kevinas/crypto_portfolio_backend/
│           │   ├── controller/
│           │   ├── dto/
│           │   ├── exception/
│           │   ├── model/
│           │   ├── repository/
│           │   ├── security/
│           │   └── service/
│           ├── main/resources/db/migration/
│           └── test/
├── frontend/
│   └── frontend/
│       ├── package.json
│       └── src/
│           ├── app/
│           ├── components/
│           └── lib/
├── REPO_OVERVIEW.md
└── tasklist.MD
```

## Local Setup

### 1. Start PostgreSQL

```bash
cd backend/crypto-portfolio-backend
docker compose up -d
```

This starts:

| Service | URL/Port |
|---|---|
| PostgreSQL | `localhost:5432` |
| pgAdmin | `http://localhost:5050` |

Default database credentials from `docker-compose.yml`:

| Setting | Value |
|---|---|
| Database | `cryptodb` |
| Username | `postgres` |
| Password | `postgres` |

### 2. Run The Backend

```bash
cd backend/crypto-portfolio-backend
./mvnw spring-boot:run
```

Backend default URL:

```text
http://localhost:8080
```

### 3. Configure The Frontend

```bash
cd frontend/frontend
cp .env.example .env.local
npm install
```

Default frontend API config:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

### 4. Run The Frontend

```bash
cd frontend/frontend
npm run dev
```

Frontend default URL:

```text
http://localhost:3000
```

## API Endpoints

### Public

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Log in and receive a JWT |
| `GET` | `/api/market/prices?symbols=BTC,ETH` | Get current prices for supported symbols |

### Protected

Protected endpoints require:

```text
Authorization: Bearer <token>
```

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/portfolio/holdings` | Get current user holdings |
| `GET` | `/api/portfolio/summary` | Get portfolio totals and per-asset summary |
| `GET` | `/api/portfolio/performance/history?range=7d` | Get historical portfolio snapshots |
| `POST` | `/api/transactions` | Create a BUY or SELL transaction |
| `GET` | `/api/transactions` | Get current user transaction history |
| `GET` | `/api/transactions/summary` | Get buy/sell/realised profit totals |

### Example Transaction Request

```json
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "type": "BUY",
  "quantity": 0.5,
  "priceUsd": 45000
}
```

### Example Holding Response

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
  "profitLossUsd": 7500.00,
  "marketPriceAvailable": true
}
```

If a holding has no supported market data, `marketPriceAvailable` is `false`. The numeric market-value fields remain present for compatibility, but the frontend treats them as unavailable rather than a real zero valuation.

### Example Portfolio Summary Additions

```json
{
  "totalCurrentValueUsd": 30000.00,
  "totalProfitLossUsd": 7500.00,
  "hasUnsupportedMarketData": false
}
```

## Build And Test

### Backend Tests

```bash
cd backend/crypto-portfolio-backend
./mvnw test
```

### Backend Build

```bash
cd backend/crypto-portfolio-backend
./mvnw package
```

### Frontend Lint

```bash
cd frontend/frontend
npm run lint
```

### Frontend Build

```bash
cd frontend/frontend
npm run build
```

## Environment Variables

### Backend

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cryptodb` | JDBC database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | Backend server port |
| `JWT_SECRET` | `change_this_in_real_env` | JWT signing secret |
| `JWT_EXPIRATION_SECONDS` | `3600` | JWT lifetime |
| `CRYPTO_API_BASE_URL` | `https://api.coingecko.com/api/v3` | Crypto market API base URL |

### Frontend

| Variable | Default | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL used by the browser |

## Known Limitations

- The frontend currently has lint/build checks but no dedicated test suite.
- The backend supports a fixed set of symbols for CoinGecko price lookup.
- Portfolio performance snapshots are currently created after transaction writes, not on a schedule.
- Scheduler classes exist but are commented out.
- CORS is currently configured for the local frontend origin `http://localhost:3000`.
- The repository may exist in multiple local folders. The implemented changes described here are in `/Users/kevinasramoska/Desktop/crypto-portfolio-app`.
- Both `application.yaml` and `application.properties` define overlapping backend configuration.
- `V2__transaction_constraints.sql` exists but is currently empty.
- The app is configured for local development, not production deployment.
