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
| Backend tests | JUnit 5, Spring Boot Test, MockMvc, H2, PostgreSQL Testcontainers |
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
- Create portfolio snapshots after transaction commits and from the scheduled snapshot job.

## Project Structure

```text
.
├── README.md
├── .env.example
├── .github/workflows/ci.yml
├── backend/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/kevinas/crypto_portfolio_backend/
│       ├── main/resources/application.yaml
│       ├── main/resources/db/migration/
│       └── test/
├── docs/
│   ├── AGENTS.md
│   ├── REPO_OVERVIEW.md
│   └── tasklist.MD
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
| `PORT` | — | Hosting-provider HTTP port; takes precedence over `SERVER_PORT` |
| `SERVER_PORT` | `8080` | Local backend HTTP port fallback |
| `JWT_SECRET` | local development default | JWT HMAC signing secret; required in the `prod` profile, must be at least 32 bytes, and is validated at startup |
| `JWT_EXPIRATION_SECONDS` | `3600` | JWT lifetime |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated frontend origins allowed to call the API; required in the `prod` profile |
| `AUTH_RATE_LIMIT_MAX_REQUESTS` | `10` | Maximum login or registration attempts from one client address per auth window |
| `AUTH_RATE_LIMIT_WINDOW_SECONDS` | `60` | Auth rate-limit window length in seconds |
| `MARKET_RATE_LIMIT_MAX_REQUESTS` | `60` | Maximum market-data requests from one client address per market window |
| `MARKET_RATE_LIMIT_WINDOW_SECONDS` | `60` | Market-data rate-limit window length in seconds |
| `CRYPTO_API_BASE_URL` | `https://api.coingecko.com/api/v3` | CoinGecko-compatible market API base URL |
| `SPRING_JPA_SHOW_SQL` | `true` | SQL logging toggle |
| `PORTFOLIO_SNAPSHOT_FIXED_DELAY_MS` | `3600000` | Scheduled snapshot interval |
| `PORTFOLIO_SNAPSHOT_INITIAL_DELAY_MS` | `300000` | Scheduled snapshot startup delay |

Rate limits apply to `POST /api/auth/login`, `POST /api/auth/register`, and `GET /api/market/**`. Exceeded requests receive `429 Too Many Requests` with a `Retry-After` header. The limiter is in-memory and per application instance; use a shared store or edge rate limiting before scaling the API horizontally.

### Frontend

| Variable | Example | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Browser-visible backend API base URL |

## Deployment

The repository includes a basic deployment path using a Vercel frontend, Dockerized API, and managed PostgreSQL database.

### Backend

Build and run the backend image from the `backend` directory:

```bash
docker build -t crypto-portfolio-api .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL='jdbc:postgresql://<host>/<database>?sslmode=require' \
  -e DB_USERNAME='<database-user>' \
  -e DB_PASSWORD='<database-password>' \
  -e JWT_SECRET='<at-least-32-byte-secret>' \
  -e APP_CORS_ALLOWED_ORIGINS='https://<your-vercel-app>.vercel.app' \
  crypto-portfolio-api
```

The hosting platform should provide `PORT`. The public application check is `GET /api/health` and returns `{ "status": "ok" }`. Kubernetes-style readiness and liveness probes are public at `GET /actuator/health/readiness` and `GET /actuator/health/liveness`; operational metrics are available only to authenticated users at `GET /actuator/metrics`.

### Frontend

Deploy `frontend/` as a Next.js project and set this production environment variable:

```text
NEXT_PUBLIC_API_BASE_URL=https://<your-api-domain>/api
```

Set the backend's `APP_CORS_ALLOWED_ORIGINS` to the exact production frontend origin. Never commit real database credentials or JWT secrets.

## API Reference

Protected endpoints require:

```text
Authorization: Bearer <accessToken>
```

| Method | Endpoint | Auth | Request | Response |
|---|---|---:|---|---|
| `GET` | `/api/health` | No | None | `{ "status": "ok" }` |
| `GET` | `/actuator/health/readiness` | No | None | Actuator readiness status |
| `GET` | `/actuator/health/liveness` | No | None | Actuator liveness status |
| `GET` | `/actuator/metrics` | Yes | None | Available Micrometer metric names |
| `POST` | `/api/auth/register` | No | `{ "email": string, "password": string }` | `{ "accessToken": string }` |
| `POST` | `/api/auth/login` | No | `{ "email": string, "password": string }` | `{ "accessToken": string }` |
| `GET` | `/api/market/prices?symbols=BTC,ETH` | No | Query `symbols` list | `{ "BTC": 60000.00 }` |
| `GET` | `/api/market/supported-coins` | No | None | `[{ "symbol": "BTC", "name": "Bitcoin", "coinGeckoId": "bitcoin" }]` |
| `GET` | `/api/portfolio/holdings` | Yes | None | `HoldingResponse[]` |
| `GET` | `/api/portfolio/summary` | Yes | None | `PortfolioSummaryResponse` |
| `GET` | `/api/portfolio/performance/history?range=30d` | Yes | Query `range`: `7d`, `30d`, `90d` | `{ "range": "30d", "history": PerformancePoint[] }` |
| `POST` | `/api/transactions` | Yes | `TransactionRequest` | `TransactionResponse` |
| `PUT` | `/api/transactions/{id}` | Yes | `TransactionRequest` | Replacement `TransactionResponse` |
| `DELETE` | `/api/transactions/{id}` | Yes | None | `204 No Content` |
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

Edits preserve the transaction's historical ledger position by voiding the current audit row and returning a replacement row with a new ID. Edits and deletes replay the active ledger atomically; a correction that would make any later `SELL` historically invalid returns `409 Conflict` without changing transactions, holdings, or snapshots.

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
| Port `3000` already in use | Run `npm run dev -- --port 3001`, then set `APP_CORS_ALLOWED_ORIGINS=http://localhost:3001` for the backend. |
| Registration/login fails with JWT key errors | Use a `JWT_SECRET` at least 32 bytes long. Very short local secrets fail JJWT HMAC validation. |
| Prices are missing or zero | Check CoinGecko availability, rate limits, and whether the symbol exists in `/api/market/supported-coins`. Unsupported symbols are intentionally marked unavailable. |
| Frontend cannot reach backend | Confirm `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api`, backend is running, and browser requests are not blocked by CORS. |
| E2E tests cannot launch browser | Run `cd frontend && npx playwright install chromium`. |

## Known Limitations

- Supported market symbols are fixed in backend code and exposed through `/api/market/supported-coins`.
- Unsupported symbols can be recorded but are excluded from known market-value totals.
- Portfolio snapshots are best-effort after a committed transaction and scheduled by the backend process. A snapshot failure is logged but never rolls back valid transaction and holding writes; missed app uptime means missed scheduled snapshots.
- Deployment environment promotion, database backup/restore, and rollback procedures are not documented yet.
- There is no seed data.
- `V2__transaction_constraints.sql` is intentionally a no-op placeholder to keep existing Flyway version history stable.
- The same project may exist in more than one local checkout; confirm you are working in `/Users/kevinasramoska/Desktop/crypto-portfolio-app` before committing.
