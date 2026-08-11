# Repository Overview

## 1. What This Project Does

This repository contains a full-stack crypto portfolio tracker.

The application lets users register, log in, record cryptocurrency buy/sell transactions, view current holdings, track live prices, and calculate portfolio value and profit/loss using market data.

The backend provides authentication, transaction recording, holdings updates, portfolio summary calculations, transaction summaries, historical portfolio snapshots, and CoinGecko price integration.

The frontend provides login/register screens and a dashboard for watchlist prices, portfolio summary, holdings, transaction entry, transaction history, transaction totals, and performance history.

Recent dashboard reliability work added clearer unsupported market-data display, stale-token cleanup on auth failures, explicit local CORS support, improved empty states, and separated dashboard loading/error state by data slice.

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 26, Spring Boot 4.1.0 |
| API | Spring MVC REST controllers |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL |
| ORM | Spring Data JPA, Hibernate |
| Migrations | Flyway |
| External API | CoinGecko |
| Backend tests | JUnit 5, Spring Boot Test, MockMvc, Mockito, H2, PostgreSQL Testcontainers |
| Frontend | Next.js 16.2.9 App Router, React 19.2.7, TypeScript |
| Styling | Tailwind CSS |
| Local infrastructure | Docker Compose, PostgreSQL, pgAdmin |

## 3. Folder Structure

```text
.
├── README.md
├── .env.example
├── .github/workflows/ci.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── docker-compose.yml
│   ├── src/main/java/com/kevinas/crypto_portfolio_backend/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── scheduler/
│   │   ├── security/
│   │   ├── service/
│   │   └── CryptoPortfolioBackendApplication.java
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── application-e2e.yaml
│   │   ├── application-test.yaml
│   │   └── db/migration/
│   └── src/test/
├── docs/
│   ├── AGENTS.md
│   ├── REPO_OVERVIEW.md
│   └── tasklist.MD
└── frontend/
    ├── .env.example
    ├── package.json
    ├── next.config.js
    ├── tsconfig.json
    ├── eslint.config.mjs
    ├── postcss.config.mjs
    ├── vitest.config.ts
    ├── playwright.config.ts
    ├── src/app/
    ├── src/components/
    ├── src/hooks/
    └── src/lib/
```

Important backend folders:

| Folder | Purpose |
|---|---|
| `controller` | REST API endpoints |
| `service` | Business logic |
| `repository` | Spring Data JPA database access |
| `model` | JPA entities and enums |
| `dto` | Request and response objects |
| `security` | JWT authentication and Spring Security config |
| `exception` | Global error handling |
| `config` | Spring bean and infrastructure config |
| `db/migration` | Flyway database migrations |
| `src/test` | Backend unit and integration tests |

Important frontend folders:

| Folder | Purpose |
|---|---|
| `src/app` | Next.js App Router pages and layouts |
| `src/components` | Reusable React UI components |
| `src/hooks` | Client-side data loading and reusable React hooks |
| `src/lib` | API client, auth helpers, shared TypeScript types |
| `public` | Static assets |

## 4. How The App Runs

### Backend

The backend starts from:

```text
backend/src/main/java/com/kevinas/crypto_portfolio_backend/CryptoPortfolioBackendApplication.java
```

Startup flow:

1. Spring Boot starts the application.
2. Spring loads controllers, services, repositories, security config, and JPA.
3. Flyway applies database migrations when enabled.
4. The API listens on port `8080` by default.
5. Public endpoints are available for auth, market prices, health probes, and Swagger/OpenAPI.
6. Protected endpoints, including operational metrics, require a valid JWT Bearer token.
7. CORS origins and auth/market rate limits are environment-configurable.

### Frontend

The frontend is a Next.js app under:

```text
frontend
```

The main pages are:

| Route | File |
|---|---|
| `/` | `src/app/page.tsx` |
| `/login` | `src/app/(auth)/login/page.tsx` |
| `/register` | `src/app/(auth)/register/page.tsx` |
| `/dashboard` | `src/app/dashboard/page.tsx` |

The frontend calls the backend through:

```text
frontend/src/lib/api.ts
```

The browser API base URL is configured with:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

## 5. Main Application Flow

### Authentication Flow

1. User registers through `POST /api/auth/register`.
2. Backend hashes the password with BCrypt.
3. Backend stores the user in PostgreSQL.
4. Backend returns a JWT.
5. Frontend stores the JWT in `localStorage`.
6. Future protected requests include:

```text
Authorization: Bearer <token>
```
7. If a protected frontend request receives `401` or `403`, the stale token and active profile are cleared and the dashboard prompts the user to log in again.

### Transaction Flow

1. Authenticated users create, edit, or delete ledger entries through `/api/transactions`.
2. Every mutation takes a pessimistic lock on the user's row, so creates, edits, and deletes for one user are serialized.
3. New transactions receive the next immutable per-user `ledgerSequence`.
4. Edits soft-void the active row and create a replacement with a new database ID while preserving the original `createdAt` and `ledgerSequence`; deletes soft-void without replacement.
5. Active transactions are replayed in ledger order to recalculate weighted-average buy prices, realised P/L, and stored holdings.
6. A replay that would make any historical `SELL` exceed the available position raises `409 Conflict` and rolls the entire mutation back.
7. Historical snapshots at or after the changed transaction's original `createdAt` are invalidated in the same database transaction.
8. After commit, an `AFTER_COMMIT` listener attempts a fresh portfolio snapshot in a separate transaction. Snapshot failure is logged but does not invalidate committed ledger or holding state.

### Portfolio Flow

1. Authenticated user requests holdings or portfolio summary.
2. Backend loads the current user from the Spring Security context.
3. Backend reads stored holdings for open positions and transactions for realised P/L history.
4. Backend fetches current market prices through `MarketDataService`.
5. Backend calculates:
   - invested value
   - current value
   - unrealised profit/loss
   - realised profit/loss
   - total profit/loss
6. Backend returns portfolio DTOs.
7. Holdings and portfolio summary responses include market-data availability flags so unsupported symbols do not look like real `$0.00` valuations.

### Dashboard Flow

1. Dashboard loads public watchlist prices from `/api/market/prices`.
2. Dashboard loads protected data with separate request groups:
   - holdings
   - portfolio summary
   - transaction history
   - transaction summary
   - performance history
3. Portfolio, transaction, and performance history failures are handled independently where possible.
4. Creating a transaction refreshes portfolio, transaction, performance, and price data.
5. Unsupported prices show explicit no-market-data states in watchlist cards, holdings, and summary UI.

## 6. Important Files

| File | Why it matters |
|---|---|
| `backend/pom.xml` | Backend dependencies and Maven build config |
| `backend/docker-compose.yml` | Local PostgreSQL and pgAdmin setup |
| `backend/src/main/resources/application.yaml` | Main backend env-based runtime configuration |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | Creates main database schema |
| `backend/src/main/resources/db/migration/V2__transaction_constraints.sql` | Intentional no-op placeholder retained for Flyway version history |
| `backend/src/main/resources/db/migration/V3__portfolio_snapshots.sql` | Creates portfolio snapshot table |
| `CryptoPortfolioBackendApplication.java` | Backend application entry point |
| `SecurityConfig.java` | Defines JWT security rules and public/protected endpoints |
| `JwtAuthenticationFilter.java` | Reads and validates Bearer tokens |
| `AuthController.java` | Register/login endpoints |
| `PortfolioController.java` | Holdings, summary, and history endpoints |
| `TransactionController.java` | Transaction endpoints |
| `TransactionServiceImpl.java` | Core buy/sell business logic |
| `PortfolioServiceImpl.java` | Portfolio calculations and history |
| `MarketDataServiceImpl.java` | CoinGecko integration and price cache |
| `frontend/package.json` | Frontend dependencies and scripts |
| `frontend/.env.example` | Frontend API URL example |
| `frontend/src/lib/api.ts` | Frontend HTTP client |
| `frontend/src/lib/auth.ts` | Token/profile localStorage helpers |
| `frontend/src/app/dashboard/page.tsx` | Main dashboard screen |
| `frontend/src/hooks/useDashboardData.ts` | Dashboard backend data loading, refresh, pagination, transaction save, and CSV export logic |

Current market-data response additions:

| Response | Field | Purpose |
|---|---|---|
| Holding response | `marketPriceAvailable` | Indicates whether current price/value/P&L can be trusted for the holding |
| Portfolio holding summary | `marketPriceAvailable` | Indicates whether summary values are based on supported market data |
| Portfolio summary | `hasUnsupportedMarketData` | Indicates whether totals are partial because at least one holding has no market data |

Current API endpoints:

| Method | Endpoint | Auth | Purpose |
|---|---|---:|---|
| `GET` | `/api/health` | No | Return a lightweight application health response |
| `GET` | `/actuator/health/readiness` | No | Return Actuator readiness status |
| `GET` | `/actuator/health/liveness` | No | Return Actuator liveness status |
| `GET` | `/actuator/metrics` | Yes | List available Micrometer metrics |
| `POST` | `/api/auth/register` | No | Register and receive a JWT |
| `POST` | `/api/auth/login` | No | Log in and receive a JWT |
| `GET` | `/api/market/prices?symbols=BTC,ETH` | No | Fetch current prices for supported mapped symbols |
| `GET` | `/api/market/supported-coins` | No | List backend-supported symbol/name/CoinGecko mappings |
| `GET` | `/api/portfolio/holdings` | Yes | Return current stored holdings with market-data availability flags |
| `GET` | `/api/portfolio/summary` | Yes | Return totals plus per-holding summary |
| `GET` | `/api/portfolio/performance/history?range=30d` | Yes | Return snapshot history for `7d`, `30d`, or `90d` |
| `POST` | `/api/transactions` | Yes | Create a `BUY` or `SELL` transaction |
| `PUT` | `/api/transactions/{id}` | Yes | Replace an active transaction revision and replay the ledger |
| `DELETE` | `/api/transactions/{id}` | Yes | Void an active transaction and replay the ledger |
| `GET` | `/api/transactions` | Yes | Return all current-user transactions |
| `GET` | `/api/transactions/paginated?page=0&size=20` | Yes | Return paginated current-user transactions |
| `GET` | `/api/transactions/summary` | Yes | Return buy volume, sell volume, and realised P/L totals |

## 7. Development Commands

### Start Local Database

```bash
cd backend
docker compose up -d
```

### Run Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend default URL:

```text
http://localhost:8080
```

### Run Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Frontend default URL:

```text
http://localhost:3000
```

### Build Backend

```bash
cd backend
./mvnw package
```

### Build Frontend

```bash
cd frontend
npm run build
```

### Run Frontend Tests

```bash
cd frontend
npm test
```

### Lint Frontend

```bash
cd frontend
npm run lint
```

## 8. Testing Commands

### Run Backend Tests

```bash
cd backend
./mvnw test
```

The backend tests use:

- JUnit 5
- Spring Boot Test
- MockMvc
- Mockito
- H2 test database
- PostgreSQL Testcontainers for Flyway, constraint, rollback, and decimal behavior

The Testcontainers suite requires a Docker-compatible runtime.

### Run Frontend Checks

Available frontend quality checks:

```bash
cd frontend
npm test
npm run lint
npm run build
npm run test:e2e
```

## 9. Environment Variables

Backend environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cryptodb` | JDBC database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `PORT` | none | Hosting-provider port; takes precedence over `SERVER_PORT` |
| `SERVER_PORT` | `8080` | Local backend server port |
| `JWT_SECRET` | local development default | JWT signing secret; required in `prod` and at least 32 bytes |
| `JWT_EXPIRATION_SECONDS` | `3600` | JWT lifetime |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed frontend origins; required in `prod` |
| `AUTH_RATE_LIMIT_MAX_REQUESTS` | `10` | Auth requests allowed per client window |
| `AUTH_RATE_LIMIT_WINDOW_SECONDS` | `60` | Auth rate-limit window length |
| `MARKET_RATE_LIMIT_MAX_REQUESTS` | `60` | Market requests allowed per client window |
| `MARKET_RATE_LIMIT_WINDOW_SECONDS` | `60` | Market rate-limit window length |
| `CRYPTO_API_BASE_URL` | `https://api.coingecko.com/api/v3` | Crypto market API base URL |
| `SPRING_JPA_SHOW_SQL` | `true` | SQL logging toggle |
| `PORTFOLIO_SNAPSHOT_FIXED_DELAY_MS` | `3600000` | Scheduled snapshot interval |
| `PORTFOLIO_SNAPSHOT_INITIAL_DELAY_MS` | `300000` | Scheduled snapshot startup delay |

Frontend environment variables:

| Variable | Default example | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL used by the browser |

## 10. Common Developer Notes

- The backend is structured around a layered architecture: controller, service, repository, database.
- Authentication is stateless and JWT-based.
- Most protected backend endpoints rely on the authenticated user from Spring Security context.
- Portfolio writes happen through transactions, not direct holding CRUD.
- Active transaction revisions are the audit-safe ledger; stored holdings and realised P/L are derived from replaying that ledger.
- `GET /api/portfolio/holdings` exists, but backend `POST`, `PUT`, and `DELETE` endpoints for `/api/portfolio/holdings` are not currently present.
- Backend prices and portfolio values are USD-oriented.
- `V2__transaction_constraints.sql` is intentionally a no-op placeholder retained to keep Flyway version history stable.
- `application.yaml` is the source of truth for main backend runtime configuration.
- Scheduled portfolio snapshots are enabled and configured through `PORTFOLIO_SNAPSHOT_*` environment variables.
- Price refresh currently happens lazily through API calls and an in-memory cache.
- Current supported market symbols are exposed by `GET /api/market/supported-coins`.
- Unsupported symbols can still be recorded manually, but portfolio UI flags missing market data.
- Dashboard protected API failures clear stale auth state when the backend returns `401` or `403`.
- Dashboard portfolio, transaction, and performance-history sections now load and fail independently.
- Backend CORS defaults to `http://localhost:3000`; production must provide explicit allowed origins.
- Auth and market-data routes use an in-memory, per-application-instance rate limiter.
- Transaction and holding writes commit atomically; snapshot creation is best-effort after commit and also runs on a schedule.
- Historical edits and deletes preserve voided revisions, reuse their stable ledger position, and reject corrections that would invalidate a later sell.
- PostgreSQL Testcontainers coverage requires Docker and is run by CI.
- No seed data is present.
- Frontend smoke and API-client tests are configured with Vitest and Testing Library.
- This repository also exists locally at `/Users/kevinasramoska/Documents/GitHub/crypto-portfolio-app`; make sure commits are made from the intended checkout.
