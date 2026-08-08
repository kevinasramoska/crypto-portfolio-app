# Crypto Portfolio Tracker

A full-stack crypto portfolio tracker for recording crypto `BUY` and `SELL` transactions, viewing current holdings, tracking market value, and calculating realised and unrealised profit/loss.

The app is transaction-driven. Users write transactions, the backend updates stored holdings as the source of truth for open positions, transactions remain the audit log and realised P/L source, and portfolio snapshots capture performance history.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 26, Spring Boot 4.1.0 |
| API | Spring MVC REST controllers |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL, Spring Data JPA, Hibernate, Flyway |
| Market data | CoinGecko-compatible API via `RestTemplate` |
| Backend tests | JUnit 5, Spring Boot Test, MockMvc, Mockito, H2 |
| Frontend | Next.js 16.2.9, React 19.2.7, TypeScript |
| Styling | Tailwind CSS |
| Frontend tests | Vitest, Testing Library, Playwright config |
| Local services | Docker Compose, PostgreSQL, pgAdmin |

## Current Features

- Register and log in with email/password.
- Store JWT auth in the frontend and clear stale sessions on protected API `401` or `403`.
- Record `BUY` and `SELL` transactions.
- Prevent selling more than the current stored holding quantity.
- Maintain stored holdings as the source of truth for open positions.
- Calculate average buy price, invested value, current value, realised P/L, unrealised P/L, and total P/L.
- Fetch public watchlist prices and backend-supported CoinGecko symbol mappings.
- Show unsupported market-data states instead of treating missing prices as real zero values.
- Paginate transaction history and export all transactions as CSV.
- Show portfolio summary, holdings, asset allocation, transaction summary, and performance history.
- Create portfolio snapshots after transaction writes and from the scheduled snapshot job.

## Project Structure

```text
.
├── README.md
├── REPO_OVERVIEW.md
├── tasklist.MD
├── .env.example
├── backend/
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/kevinas/crypto_portfolio_backend/
│       ├── main/resources/application.yaml
│       ├── main/resources/db/migration/
│       └── test/
└── frontend/
    ├── .env.example
    ├── package.json
    └── src/
        ├── app/
        ├── components/
        ├── hooks/
        └── lib/
```

## Local Setup

### 1. Install Prerequisites

- Java 26
- Docker Desktop or compatible Docker runtime
- Node.js 20.19+ recommended for the current frontend tooling

### 2. Configure Environment

Frontend:

```bash
cd frontend
cp .env.example .env.local
```

Optional backend shell env from the root example:

```bash
cp .env.example .env.local
set -a
source .env.local
set +a
```

Spring Boot also has local defaults in `backend/src/main/resources/application.yaml`, so the backend can run without sourcing a `.env` file when using the default Docker database.

### 3. Start PostgreSQL

```bash
cd backend
docker compose up -d
```

| Service | URL/Port | Default Login |
|---|---|---|
| PostgreSQL | `localhost:5432` | `postgres` / `postgres`, database `cryptodb` |
| pgAdmin | `http://localhost:5050` | `admin@admin.com` / `admin123` |

### 4. Run Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend default URL: `http://localhost:8080`

### 5. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend default URL: `http://localhost:3000`

## Local Commands

| Area | Command |
|---|---|
| Start database | `cd backend && docker compose up -d` |
| Stop database | `cd backend && docker compose down` |
| Run backend | `cd backend && ./mvnw spring-boot:run` |
| Backend tests | `cd backend && ./mvnw test` |
| Backend build | `cd backend && ./mvnw package` |
| Run frontend | `cd frontend && npm run dev` |
| Frontend tests | `cd frontend && npm test` |
| Frontend lint | `cd frontend && npm run lint` |
| Frontend build | `cd frontend && npm run build` |
| Playwright browsers | `cd frontend && npx playwright install chromium` |
| E2E test script | `cd frontend && npm run test:e2e` |

## Environment Variables

### Backend

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cryptodb` | JDBC database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `JWT_SECRET` | `change_this_in_real_env` | JWT HMAC signing secret; use at least 32 bytes |
| `JWT_EXPIRATION_SECONDS` | `3600` | JWT lifetime |
| `CRYPTO_API_BASE_URL` | `https://api.coingecko.com/api/v3` | CoinGecko-compatible market API base URL |
| `SPRING_JPA_SHOW_SQL` | `true` | SQL logging toggle |
| `PORTFOLIO_SNAPSHOT_FIXED_DELAY_MS` | `3600000` | Scheduled snapshot interval |
| `PORTFOLIO_SNAPSHOT_INITIAL_DELAY_MS` | `300000` | Scheduled snapshot startup delay |

### Frontend

| Variable | Example | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Browser-visible backend API base URL |

## API Reference

Protected endpoints require:

```text
Authorization: Bearer <accessToken>
```

| Method | Endpoint | Auth | Request | Response |
|---|---|---:|---|---|
| `POST` | `/api/auth/register` | No | `{ "email": string, "password": string }` | `{ "accessToken": string }` |
| `POST` | `/api/auth/login` | No | `{ "email": string, "password": string }` | `{ "accessToken": string }` |
| `GET` | `/api/market/prices?symbols=BTC,ETH` | No | Query `symbols` list | `{ "BTC": 60000.00 }` |
| `GET` | `/api/market/supported-coins` | No | None | `[{ "symbol": "BTC", "name": "Bitcoin", "coinGeckoId": "bitcoin" }]` |
| `GET` | `/api/portfolio/holdings` | Yes | None | `HoldingResponse[]` |
| `GET` | `/api/portfolio/summary` | Yes | None | `PortfolioSummaryResponse` |
| `GET` | `/api/portfolio/performance/history?range=30d` | Yes | Query `range`: `7d`, `30d`, `90d` | `{ "range": "30d", "history": PerformancePoint[] }` |
| `POST` | `/api/transactions` | Yes | `TransactionRequest` | `TransactionResponse` |
| `GET` | `/api/transactions` | Yes | None | `TransactionResponse[]` |
| `GET` | `/api/transactions/paginated?page=0&size=20` | Yes | Query `page`, `size` | `PaginatedTransactionsResponse` |
| `GET` | `/api/transactions/summary` | Yes | None | `{ "totalBuyVolumeUsd": number, "totalSellVolumeUsd": number, "totalRealisedProfitUsd": number }` |

### Transaction Request

```json
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "type": "BUY",
  "quantity": 0.5,
  "priceUsd": 45000
}
```

`type` must be `BUY` or `SELL`. `quantity` must be greater than `0`; `priceUsd` must be `0` or greater. A `SELL` that exceeds the stored holding quantity returns a conflict response.

### Holding Response

```json
{
  "id": 1,
  "symbol": "BTC",
  "name": "Bitcoin",
  "quantity": 0.5,
  "averageBuyPriceUsd": 45000.00,
  "currentPriceUsd": 60000.00,
  "investedValueUsd": 22500.00,
  "currentValueUsd": 30000.00,
  "profitLossUsd": 7500.00,
  "marketPriceAvailable": true
}
```

### Portfolio Summary Response

```json
{
  "totalInvestedUsd": 22500.00,
  "totalCurrentValueUsd": 30000.00,
  "totalUnrealisedProfitLossUsd": 7500.00,
  "totalRealisedProfitLossUsd": 0.00,
  "totalProfitLossUsd": 7500.00,
  "hasUnsupportedMarketData": false,
  "holdings": [
    {
      "symbol": "BTC",
      "name": "Bitcoin",
      "quantity": 0.5,
      "averageBuyPriceUsd": 45000.00,
      "currentPriceUsd": 60000.00,
      "investedValueUsd": 22500.00,
      "currentValueUsd": 30000.00,
      "unrealisedProfitLossUsd": 7500.00,
      "marketPriceAvailable": true
    }
  ]
}
```

### Paginated Transactions Response

```json
{
  "content": [
    {
      "id": 1,
      "symbol": "BTC",
      "name": "Bitcoin",
      "type": "BUY",
      "quantity": 0.5,
      "priceUsd": 45000.00,
      "totalValueUsd": 22500.00,
      "realisedProfitUsd": 0.00,
      "createdAt": "2026-06-19T20:00:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

## Market Data

Supported market symbols are served by `GET /api/market/supported-coins`.

Current backend-supported symbols:

```text
BTC, ETH, SOL, LINK, DOGE, ADA, XRP, DOT, MATIC, AVAX, UNI, ATOM, XLM, LTC, TRX, BCH, ALGO, ICP, NEAR, VET, BNB
```

Unsupported symbols can still be recorded manually, but market-value fields are flagged with `marketPriceAvailable=false` and portfolio totals set `hasUnsupportedMarketData=true`.

## Troubleshooting

| Problem | Check |
|---|---|
| Backend cannot connect to DB | Confirm `docker compose up -d` is running in `backend`, port `5432` is free, and `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` match Docker defaults. |
| Flyway or schema startup errors | Confirm the database is the expected local `cryptodb`. `V2__transaction_constraints.sql` is intentionally a no-op and should remain versioned. |
| Port `8080` already in use | Stop the other process or run backend with `SERVER_PORT=8081`; then update `NEXT_PUBLIC_API_BASE_URL`. |
| Port `3000` already in use | Run `npm run dev -- --port 3001`. Backend CORS currently allows `http://localhost:3000`, so changing frontend ports may require a CORS config update. |
| Registration/login fails with JWT key errors | Use a `JWT_SECRET` at least 32 bytes long. Very short local secrets fail JJWT HMAC validation. |
| Prices are missing or zero | Check CoinGecko availability, rate limits, and whether the symbol exists in `/api/market/supported-coins`. Unsupported symbols are intentionally marked unavailable. |
| Frontend cannot reach backend | Confirm `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`, backend is running, and browser requests are not blocked by CORS. |
| E2E tests cannot launch browser | Run `cd frontend && npx playwright install chromium`. |

## Known Limitations

- The app is configured for local development, not production deployment.
- Backend CORS currently allows only `http://localhost:3000`.
- Supported market symbols are fixed in backend code and exposed through `/api/market/supported-coins`.
- Unsupported symbols can be recorded but are excluded from known market-value totals.
- Portfolio snapshots are best-effort after transaction writes and scheduled by the backend process; missed app uptime means missed scheduled snapshots.
- There is no seed data.
- The full-stack happy-path E2E task is still open; the Playwright script exists, but this repository still needs a final verified end-to-end run.
- `V2__transaction_constraints.sql` is intentionally a no-op placeholder to keep existing Flyway version history stable.
- The same project may exist in more than one local checkout; confirm you are working in `/Users/kevinasramoska/Desktop/crypto-portfolio-app` before committing.
