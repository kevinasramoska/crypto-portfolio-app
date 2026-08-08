# AGENTS.md

## Project

This is a full-stack crypto portfolio tracker.

The app lets users:
- Register and log in
- Record crypto BUY and SELL transactions
- View holdings
- Track portfolio value
- Calculate realised and unrealised profit/loss
- View transaction history
- View portfolio performance history

## Tech Stack

Backend:
- Java 26
- Spring Boot 4.1.0
- Spring MVC REST controllers
- Spring Security
- JWT (jjwt 0.12.5)
- BCrypt
- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway
- JUnit 5
- Spring Boot Test
- MockMvc (spring-boot-webmvc-test)
- Mockito
- Testcontainers (for future use)
- H2 for tests

Frontend:
- Next.js 16.2.9
- React 19.2.7
- TypeScript
- Tailwind CSS

Local infrastructure:
- Docker Compose
- PostgreSQL
- pgAdmin

## Repository Structure

Backend:
- `backend/crypto-portfolio-backend`

Frontend:
- `frontend`

Important backend folders:
- `controller` = REST endpoints
- `service` = business logic
- `repository` = Spring Data JPA access
- `model` = JPA entities/enums
- `dto` = request/response objects
- `security` = JWT and Spring Security
- `exception` = global error handling
- `config` = Spring configuration (server, security, database, API clients)
- `scheduler` = scheduled tasks (currently disabled)
- `util` = utilities like CoinGeckoSymbolMapper for symbol-to-ID mappings
- `db/migration` = Flyway migrations

Important frontend folders:
- `src/app` = Next.js App Router pages
- `src/components` = reusable UI components
- `src/lib` = API client (`api.ts`), auth helpers (`auth.ts`), shared types (`types.ts`), and supported coins list (`supportedCoins.ts`)

## Architecture Rules

- Backend follows controller -> service -> repository.
- Controllers should stay thin.
- Business logic belongs in services.
- Database access belongs in repositories.
- DTOs should be used for API input/output.
- Do not expose JPA entities directly unless this is already the existing pattern.
- Frontend should use existing components, API helpers, and TypeScript types.
- Keep frontend/backend API contracts consistent.
- Do not introduce new architecture unless requested.

## Transaction Domain Rules

The app is transaction-driven.

- Users record `BUY` and `SELL` transactions.
- Holdings are derived/updated from transactions.
- Selling more than the current holding quantity must be prevented.
- BUY transactions increase holdings and recalculate average buy price.
- SELL transactions reduce holdings and calculate realised profit/loss.
- Portfolio snapshots are created after transaction writes.
- Portfolio summary uses current market prices and user holdings.
- Market prices are USD-based.

## Market Data Rules

- CoinGecko is used for market data.
- Supported symbols should be handled consistently across frontend and backend.
- Backend uses `CoinGeckoSymbolMapper` in `util/` to map symbols to CoinGecko numeric IDs.
- Frontend uses `SUPPORTED_COIN_PRESETS` list in `supportedCoins.ts` to provide quick-access trading presets.
- Avoid unsupported or invalid symbol/name combinations.
- Prefer centralised mapping/list for supported symbols—do not duplicate symbol/name mappings across many files.
- Preserve existing market API behaviour unless explicitly changing it.

## Security Rules

- Protected endpoints require JWT Bearer auth.
- Use the authenticated user from Spring Security context for user-specific data.
- Never allow users to access another user's portfolio, holdings, or transactions.
- Validate all request DTOs.
- Do not log passwords, JWTs, or sensitive data.
- Preserve stateless JWT authentication.

## Frontend Rules

- Use existing API client patterns in `src/lib/api.ts`.
- Use existing auth helper patterns in `src/lib/auth.ts`.
- Use existing TypeScript types in `src/lib/types.ts`.
- Forms should handle validation and error messages.
- Dashboard changes should preserve existing refresh behaviour after transaction creation.
- Avoid large UI redesigns unless explicitly requested.

## Testing Commands

Backend tests:

```bash
cd backend/crypto-portfolio-backend
./mvnw test
```

**Backend test patterns:**
- Integration tests use `@SpringBootTest` with `@AutoConfigureMockMvc` (Spring Boot 4 feature).
- Tests activate the `test` profile which uses `application-test.properties`: H2 in-memory database with `ddl-auto=create-drop` and Flyway disabled for isolation.
- `@Import(TestConfig.class)` provides mocked beans (e.g., `MarketDataService`) via Mockito to prevent external API calls.
- Use `ObjectMapper` for JSON serialization in assertions.
- Tests are isolated per-test-method; schema is recreated for each test.
