# Repository Overview

## 1. What This Project Does

This repository contains a full-stack crypto portfolio tracker.

The application lets users register, log in, record cryptocurrency buy/sell transactions, view current holdings, track live prices, and calculate portfolio value and profit/loss using market data.

The backend provides authentication, transaction recording, holdings updates, portfolio summary calculations, transaction summaries, historical portfolio snapshots, and CoinGecko price integration.

The frontend provides login/register screens and a dashboard for watchlist prices, portfolio summary, holdings, transaction entry, transaction history, transaction totals, and performance history.

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5 |
| API | Spring MVC REST controllers |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL |
| ORM | Spring Data JPA, Hibernate |
| Migrations | Flyway |
| External API | CoinGecko |
| Backend tests | JUnit 5, Spring Boot Test, MockMvc, Mockito, H2 |
| Frontend | Next.js 14 App Router, React 18, TypeScript |
| Styling | Tailwind CSS |
| Local infrastructure | Docker Compose, PostgreSQL, pgAdmin |

## 3. Folder Structure

```text
.
├── README.md
├── REPO_OVERVIEW.md
├── tasklist.MD
├── backend/
│   └── crypto-portfolio-backend/
│       ├── pom.xml
│       ├── docker-compose.yml
│       ├── src/main/java/com/kevinas/crypto_portfolio_backend/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── model/
│       │   ├── repository/
│       │   ├── scheduler/
│       │   ├── security/
│       │   ├── service/
│       │   └── CryptoPortfolioBackendApplication.java
│       ├── src/main/resources/
│       │   ├── application.yaml
│       │   ├── application.properties
│       │   ├── application-test.yaml
│       │   └── db/migration/
│       └── src/test/
└── frontend/
    └── frontend/
        ├── .env.example
        ├── package.json
        ├── next.config.js
        ├── tsconfig.json
        ├── tailwind.config.js
        ├── eslint.config.mjs
        ├── src/app/
        ├── src/components/
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
| `src/lib` | API client, auth helpers, shared TypeScript types |
| `public` | Static assets |

## 4. How The App Runs

### Backend

The backend starts from:

```text
backend/crypto-portfolio-backend/src/main/java/com/kevinas/crypto_portfolio_backend/CryptoPortfolioBackendApplication.java
```

Startup flow:

1. Spring Boot starts the application.
2. Spring loads controllers, services, repositories, security config, and JPA.
3. Flyway applies database migrations when enabled.
4. The API listens on port `8080` by default.
5. Public endpoints are available for auth, market prices, and Swagger/OpenAPI.
6. Protected endpoints require a valid JWT Bearer token.

### Frontend

The frontend is a Next.js app under:

```text
frontend/frontend
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
frontend/frontend/src/lib/api.ts
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

### Transaction Flow

1. Authenticated user submits a BUY or SELL transaction to `POST /api/transactions`.
2. Backend resolves or creates the `Coin`.
3. For BUY:
   - Creates or updates the user's `Holding`.
   - Recalculates average buy price.
4. For SELL:
   - Verifies the user has enough quantity.
   - Reduces or deletes the holding.
   - Calculates realised profit/loss.
5. Backend saves the `Transaction`.
6. Backend attempts to create a portfolio snapshot.
7. Backend returns the transaction response.

### Portfolio Flow

1. Authenticated user requests holdings or portfolio summary.
2. Backend loads the current user from the Spring Security context.
3. Backend reads transactions and/or holdings from the database.
4. Backend fetches current market prices through `MarketDataService`.
5. Backend calculates:
   - invested value
   - current value
   - unrealised profit/loss
   - realised profit/loss
   - total profit/loss
6. Backend returns portfolio DTOs.

### Dashboard Flow

1. Dashboard loads public watchlist prices from `/api/market/prices`.
2. Dashboard loads protected portfolio data with the stored JWT:
   - holdings
   - portfolio summary
   - transaction history
   - transaction summary
   - performance history
3. Creating a transaction refreshes portfolio and price data.

## 6. Important Files

| File | Why it matters |
|---|---|
| `backend/crypto-portfolio-backend/pom.xml` | Backend dependencies and Maven build config |
| `backend/crypto-portfolio-backend/docker-compose.yml` | Local PostgreSQL and pgAdmin setup |
| `backend/crypto-portfolio-backend/src/main/resources/application.yaml` | Main backend env-based runtime configuration |
| `backend/crypto-portfolio-backend/src/main/resources/application.properties` | Additional backend configuration |
| `backend/crypto-portfolio-backend/src/main/resources/db/migration/V1__init_schema.sql` | Creates main database schema |
| `backend/crypto-portfolio-backend/src/main/resources/db/migration/V3__portfolio_snapshots.sql` | Creates portfolio snapshot table |
| `CryptoPortfolioBackendApplication.java` | Backend application entry point |
| `SecurityConfig.java` | Defines JWT security rules and public/protected endpoints |
| `JwtAuthenticationFilter.java` | Reads and validates Bearer tokens |
| `AuthController.java` | Register/login endpoints |
| `PortfolioController.java` | Holdings, summary, and history endpoints |
| `TransactionController.java` | Transaction endpoints |
| `TransactionServiceImpl.java` | Core buy/sell business logic |
| `PortfolioServiceImpl.java` | Portfolio calculations and history |
| `MarketDataServiceImpl.java` | CoinGecko integration and price cache |
| `frontend/frontend/package.json` | Frontend dependencies and scripts |
| `frontend/frontend/.env.example` | Frontend API URL example |
| `frontend/frontend/src/lib/api.ts` | Frontend HTTP client |
| `frontend/frontend/src/lib/auth.ts` | Token/profile localStorage helpers |
| `frontend/frontend/src/app/dashboard/page.tsx` | Main dashboard screen |

## 7. Development Commands

### Start Local Database

```bash
cd backend/crypto-portfolio-backend
docker compose up -d
```

### Run Backend

```bash
cd backend/crypto-portfolio-backend
./mvnw spring-boot:run
```

Backend default URL:

```text
http://localhost:8080
```

### Run Frontend

```bash
cd frontend/frontend
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
cd backend/crypto-portfolio-backend
./mvnw package
```

### Build Frontend

```bash
cd frontend/frontend
npm run build
```

### Lint Frontend

```bash
cd frontend/frontend
npm run lint
```

## 8. Testing Commands

### Run Backend Tests

```bash
cd backend/crypto-portfolio-backend
./mvnw test
```

The backend tests use:

- JUnit 5
- Spring Boot Test
- MockMvc
- Mockito
- H2 test database

### Run Frontend Checks

There is currently no frontend test script in `frontend/frontend/package.json`.

Available frontend quality checks:

```bash
cd frontend/frontend
npm run lint
npm run build
```

## 9. Environment Variables

Backend environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cryptodb` | JDBC database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | Backend server port |
| `JWT_SECRET` | `change_this_in_real_env` | JWT signing secret |
| `JWT_EXPIRATION_SECONDS` | `3600` | JWT lifetime |
| `CRYPTO_API_BASE_URL` | `https://api.coingecko.com/api/v3` | Crypto market API base URL |

Frontend environment variables:

| Variable | Default example | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL used by the browser |

## 10. Common Developer Notes

- The backend is structured around a layered architecture: controller, service, repository, database.
- Authentication is stateless and JWT-based.
- Most protected backend endpoints rely on the authenticated user from Spring Security context.
- Portfolio writes happen through transactions, not direct holding CRUD.
- `GET /api/portfolio/holdings` exists, but backend `POST`, `PUT`, and `DELETE` endpoints for `/api/portfolio/holdings` are not currently present.
- Backend prices and portfolio values are USD-oriented.
- `V2__transaction_constraints.sql` exists but appears empty.
- Both `application.yaml` and `application.properties` define backend configuration, so developers should check Spring config precedence before changing runtime settings.
- Scheduler support is partially present, but scheduler classes are commented out.
- Price refresh currently happens lazily through API calls and an in-memory cache.
- No seed data is present.
- No frontend tests are currently configured.
