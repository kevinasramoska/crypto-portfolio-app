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
- Java 25 LTS
- Spring Boot 4.1.2
- Spring MVC REST controllers
- Spring Security
- JWT (jjwt 0.12.5)
- BCrypt
- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway
- JUnit Jupiter 6
- Spring Boot Test
- MockMvc (spring-boot-webmvc-test)
- Mockito
- Testcontainers with PostgreSQL
- H2 for tests

Frontend:
- Next.js 16.4.3
- React 19.2.8
- TypeScript
- Tailwind CSS

Local infrastructure:
- Docker Compose
- PostgreSQL
- pgAdmin

## Repository Structure

Backend:
- `backend`

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
- `scheduler` = enabled scheduled portfolio snapshot capture
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
- Active transaction revisions determine the ledger state; stored holdings are its current-position read model, rebuilt atomically during create/edit/delete replay.
- Selling more than the current holding quantity must be prevented.
- BUY transactions increase holdings and recalculate average buy price.
- SELL transactions reduce holdings and calculate realised profit/loss.
- Portfolio snapshots are created in a separate transaction after transaction and holding writes commit.
- Portfolio summary uses current market prices and user holdings.
- Market prices are USD-based.

## Market Data Rules

- CoinGecko is used for market data.
- Supported symbols should be handled consistently across frontend and backend.
- Backend uses `CoinGeckoSymbolMapper` in `util/` to map symbols to CoinGecko string IDs such as `bitcoin`.
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
cd backend
./mvnw test
```

**Backend test patterns:**
- Integration tests use `@SpringBootTest` with `@AutoConfigureMockMvc` (Spring Boot 4 feature).
- Most integration tests activate the `test` profile, which combines `application-test.yaml` and the test-resource overrides in `application-test.properties`: H2 in-memory database with `ddl-auto=create-drop` and Flyway disabled for isolation.
- `PostgresDatabaseIntegrationTest` uses PostgreSQL Testcontainers with Flyway enabled and requires a Docker-compatible runtime.
- `IntegrationTestSupport` provides the `@MockitoBean MarketDataService` override to prevent external market API calls. `@Import(TestConfig.class)` provides the Jackson 2 `ObjectMapper` used by existing test fixtures.
- Use `ObjectMapper` for JSON serialization in assertions.
- Schema lifecycle follows the Spring test context or Testcontainers fixture; it is not recreated automatically for every test method. Use isolated fixture data and explicit cleanup where needed.

## Current Audit

- Read `PROJECT_AUDIT.md` and the dated priorities in `tasklist.MD` before selecting new work. Confirm findings against current code before implementation; this audit is a point-in-time assessment.
