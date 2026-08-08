# Project Task List

Use this as a working backlog for the crypto portfolio tracker. Tasks are grouped by urgency and type so you can pick them off gradually.

## Current Improvement Roadmap

This is the recommended order for the next phase of work. The sections below preserve the historical backlog and completed work.

### Phase 1: Establish A Reliable Baseline

- [ ] Review and commit the current working tree
  - Priority: P0
  - Confirm which untracked and modified files belong to the project before starting new feature work.
  - Status: Audited and fully verified locally on 2026-08-08. Awaiting approval to stage and commit the reviewed changes.
- [x] Run the complete local verification suite
  - Priority: P0
  - Run backend tests, frontend lint, frontend tests, frontend build, and Playwright E2E.
  - Status: Completed locally on 2026-08-08: backend tests (55 tests, including PostgreSQL Testcontainers), frontend lint, Vitest (16 tests), production build, and Playwright E2E all passed.
- [x] Add a GitHub Actions CI workflow
  - Priority: P0
  - Run Maven tests, frontend lint/tests/build, and E2E on pull requests.
  - Status: Backend, frontend, and Playwright jobs are defined in `.github/workflows/ci.yml`.
- [x] Add coverage reporting
  - Priority: P1
  - Configure JaCoCo for Java and Vitest V8 coverage for TypeScript; publish reports in CI.
  - Status: Completed with JaCoCo XML/HTML reports in `backend/target/site/jacoco`, Vitest V8 reports in `frontend/coverage`, and 14-day CI artifacts for both.

### Phase 2: Harden Security And Data Integrity

- [x] Add production health and metrics endpoints
  - Priority: P1
  - Add Spring Boot Actuator with a public readiness check and protected operational metrics.
  - Status: Completed with public Actuator readiness/liveness probes and JWT-protected metrics, covered by integration tests.
- [x] Review authentication and secret handling
  - Priority: P1
  - Verify password rules, JWT expiry, secret requirements, CORS origins, error responses, and production defaults.
  - Status: Completed. Passwords remain consistently enforced at 8-72 characters and stored with BCrypt; JWT secrets and expiry validate at startup; production requires explicit JWT and CORS settings; and duplicate registration does not reveal email state.
- [x] Add rate limiting to authentication and market-data endpoints
  - Priority: P1
  - Protect login/register and external API usage from abuse and accidental request loops.
  - Status: Completed with configurable per-client in-memory limits for login, registration, and market requests; rejected requests receive `429` and `Retry-After`, with integration coverage for each route group.
- [x] Add database integration tests using PostgreSQL Testcontainers
  - Priority: P1
  - Exercise Flyway migrations, constraints, transaction rollbacks, and decimal/quantity behavior against the real database engine.
  - Status: Completed with a PostgreSQL Testcontainers suite that checks Flyway history/schema, unique constraints, rollback of an invalid sell, and persisted numeric scale. All three container-backed tests pass locally with Docker and are included in CI.
- [x] Make transaction writes and snapshot creation behavior explicit
  - Priority: P1
  - Decide whether snapshot failure should be retried, reported, or kept best-effort, then cover the chosen behavior with tests.
  - Status: Completed. Transaction and holding writes commit atomically; an `AFTER_COMMIT` listener creates a snapshot in a separate transaction. Snapshot failures are logged with transaction and user IDs, do not invalidate the committed transaction, and the existing scheduled snapshots provide later recovery.
- [ ] Add backup and migration recovery documentation
  - Priority: P2
  - Document PostgreSQL backup/restore, Flyway failure recovery, and production rollback steps.

### Phase 3: Improve The Product

- [ ] Add transaction editing and deletion with audit-safe rules
  - Priority: P1
  - Recalculate holdings and realised P/L consistently, and prevent edits that would create invalid positions.
- [ ] Add date/time to transactions
  - Priority: P1
  - Store an explicit transaction timestamp and use it in history, exports, and performance calculations.
- [ ] Improve portfolio history accuracy
  - Priority: P2
  - Define whether snapshots represent end-of-day or point-in-time values and handle missing market prices clearly.
- [ ] Add filtering and sorting to transaction history
  - Priority: P2
  - Support symbol, BUY/SELL type, date range, and amount sorting without loading unnecessary records.
- [ ] Improve market-data resilience
  - Priority: P2
  - Add timeout, retry/backoff, cache expiry, rate-limit handling, and a visible stale-price state.
- [ ] Add responsive and accessibility review
  - Priority: P2
  - Test keyboard navigation, focus states, labels, color contrast, mobile layouts, and screen-reader output.
- [ ] Add import/export workflow beyond CSV export
  - Priority: P3
  - Support validated CSV import with a preview, duplicate detection, and an error report.

### Phase 4: Delivery And Observability

- [ ] Add structured application logging
  - Priority: P2
  - Include request IDs, user-safe error context, external market-data failures, and scheduler outcomes without logging secrets or tokens.
- [ ] Add error monitoring
  - Priority: P2
  - Integrate Sentry or an equivalent service for frontend and backend exceptions.
- [ ] Add dependency and container security scanning
  - Priority: P2
  - Use Dependabot plus OWASP Dependency-Check or Snyk, and scan the backend image with Trivy.
- [ ] Document deployment and environment promotion
  - Priority: P2
  - Define staging versus production variables, database setup, CORS configuration, health checks, and rollback steps.
- [ ] Add performance checks for large portfolios
  - Priority: P3
  - Measure paginated transactions, portfolio calculations, market-data calls, and dashboard load time with realistic data volumes.

### Phase 5: Optional Expansion

- [ ] Add multiple portfolios or accounts
  - Priority: P3
  - Separate holdings, transactions, snapshots, and settings by portfolio rather than assuming one portfolio per user.
- [ ] Add additional currencies and exchange-rate handling
  - Priority: P3
  - Keep storage and calculations currency-aware instead of treating USD as implicit everywhere.
- [ ] Add alerts and scheduled reports
  - Priority: P3
  - Support configurable price/P&L alerts and periodic email or downloadable reports.

## Must Fix

- [ ] Stabilize the current working tree

  - Description: Review all modified, deleted, and untracked files and decide what should be committed.
  - Why it matters: The repo needs a clean checkpoint before more work.
  - Complexity: Low
  - Risk: Medium
  - Priority: P0
  - Status: Working tree audited and fully verified locally on 2026-08-08. Remaining action is approval to stage and commit from `/Users/kevinasramoska/Desktop/crypto-portfolio-app`.
  - Files likely involved: `backend/src/**`, `frontend/src/**`, `README.md`, `docs/REPO_OVERVIEW.md`, `docs/tasklist.MD`

- [x] Run full verification

  - Description: Run backend tests, frontend lint, and frontend build.
  - Why it matters: Confirms the current app is usable before adding more features.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Commands:
    - `cd backend && ./mvnw test`
    - `cd frontend && npm run lint`
    - `cd frontend && npm run build`

- [x] Update root README with actual endpoints

  - Description: Replace outdated direct holding CRUD documentation with the current transaction-based API.
  - Why it matters: The README currently describes endpoints the backend does not expose.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `README.md`

- [x] Decide what to do with `package-lock.json` changes

  - Description: Review the lockfile diff and decide whether to keep or revert the peer metadata changes.
  - Why it matters: Avoids noisy dependency churn.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `frontend/package-lock.json`

- [x] Move frontend API base URL to environment config

  - Description: Replace hardcoded `http://localhost:8080/api` with a Next.js environment variable.
  - Why it matters: Makes local setup and future deployment easier.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `frontend/src/lib/api.ts`, `frontend/.env.example`, docs

- [x] Update `REPO_OVERVIEW.md`
  - Description: Refresh the overview so it reflects the current transaction-based frontend.
  - Why it matters: Keeps onboarding docs accurate.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `docs/REPO_OVERVIEW.md`

## Should Improve

- [x] Improve transaction form coin selection

  - Description: Replace free-form symbol/name entry with supported symbol presets and an editable fallback.
  - Why it matters: Reduces invalid coin data and unsupported price lookups.
  - Complexity: Medium
  - Risk: Low
  - Priority: P1
  - Status: Implemented with reusable frontend supported coin presets and manual fallback.
  - Files likely involved: `AddTransactionForm.tsx`, `supportedCoins.ts`, `types.ts`

- [x] Show unsupported price states clearly

  - Description: Display a clear “unsupported/no market data” state when a price cannot be fetched.
  - Why it matters: Prevents misleading zero-value portfolio data.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Status: Implemented with `marketPriceAvailable` and `hasUnsupportedMarketData` response flags plus frontend no-market-data display.
  - Files likely involved: `PriceTable.tsx`, `CryptoCard.tsx`, `PortfolioSummaryCards.tsx`, `PortfolioServiceImpl.java`

- [x] Improve expired/invalid auth handling

  - Description: Clear stale tokens/profiles and redirect or prompt login when authenticated requests fail.
  - Why it matters: Prevents confusing dashboard states after JWT expiration.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Status: Implemented in `authorizedFetch` by clearing stale auth session on `401`/`403`.
  - Files likely involved: `auth.ts`, `api.ts`, `Navbar.tsx`, `dashboard/page.tsx`

- [x] Add explicit CORS config

  - Description: Configure the backend to allow the local frontend origin.
  - Why it matters: Makes local frontend/backend integration predictable.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Status: Implemented in `SecurityConfig.java` for `http://localhost:3000`.
  - Files likely involved: `SecurityConfig.java` or a new backend config class

- [x] Improve dashboard empty states

  - Description: Make first-run states clearer for no transactions, no holdings, and no snapshots.
  - Why it matters: New users need guidance on what to do first.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Status: Implemented in holdings, transactions, performance history, and market-data display copy.
  - Files likely involved: `dashboard/page.tsx`, `PriceTable.tsx`, `TransactionsTable.tsx`, `PerformanceHistory.tsx`

- [x] Separate dashboard loading and error states
  - Description: Let portfolio, transactions, and history fail independently instead of one broad load failure.
  - Why it matters: One failed endpoint should not blank the whole dashboard.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P2
  - Status: Implemented with separate portfolio, transaction, and performance loading/error states.
  - Files likely involved: `dashboard/page.tsx`, `api.ts`

## Nice To Have

- [x] Add portfolio refresh timestamp

  - Description: Show when portfolio data was last refreshed.
  - Why it matters: Helps users trust dashboard values.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Status: Completed. Timestamps now show both relative time ("2m ago", "1h ago") and absolute time (date + time) for both prices and portfolio data. Timestamps update in real-time without page refresh.
  - Files involved: `frontend/src/app/dashboard/page.tsx`

- [x] Improve home page

  - Description: Replace the basic landing page with a cleaner app entry screen.
  - Why it matters: Current home page is minimal and visually separate from the dashboard.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Status: Completed with a modern hero section, feature cards with icons, statistics section, and improved CTAs.
  - Files involved: `frontend/src/app/page.tsx`

- [x] Add transaction pagination

  - Description: Paginate backend and frontend transaction history.
  - Why it matters: Prevents large responses once transaction history grows.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `TransactionController.java`, `TransactionRepository.java`, `TransactionsTable.tsx`, `api.ts`
  - Status: Completed. Backend endpoints support paginated queries with page/size parameters. Frontend includes pagination controls with previous/next navigation. Default page size is 20. All tests pass.

- [x] Add asset allocation summary
  - Description: Show each holding’s share of total portfolio value.
  - Why it matters: Useful portfolio insight from existing holdings data.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `dashboard/page.tsx`, new component
  - Status: Completed. `dashboard/page.tsx` renders an `Asset allocation` section and `AssetAllocationSummary.tsx` shows each priced holding's share of known portfolio value, excluding unsupported-market-data holdings with a warning.

## Feature Ideas

- [x] Add portfolio performance chart

  - Description: Visualize `totalCurrentValueUsd` and `totalProfitLossUsd` over time.
  - Why it matters: Makes the existing snapshot feature more useful.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `PerformanceHistory.tsx`, new chart component
  - Status: Completed. Added `MiniPerformanceChart.tsx` and integrated it into `PerformanceHistory.tsx`. Chart renders both total current value (purple) and total profit/loss (amber) as SVG lines.

- [x] Add realised vs unrealised P/L breakdown

  - Description: Improve the summary UI with a clearer visual comparison of realised and unrealised profit/loss.
  - Why it matters: Backend already exposes both values.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `PortfolioSummaryCards.tsx`
  - Status: Completed. Added a "Realised vs Unrealised" card showing a stacked bar and numeric breakdown in `PortfolioSummaryCards.tsx`.

- [x] Add supported coin list endpoint

  - Description: Expose backend-supported CoinGecko mappings to the frontend.
  - Why it matters: Prevents frontend/backend supported-symbol drift.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `MarketDataController.java`, `MarketDataServiceImpl.java`, `api.ts`, `AddTransactionForm.tsx`
  - Status: Completed. Added `GET /api/market/supported-coins`, centralized supported coin mappings in the backend, and updated `AddTransactionForm.tsx` to load the backend list with a frontend fallback.

- [x] Add CSV export for transactions

  - Description: Let the user download transaction history as CSV.
  - Why it matters: Practical for personal finance records.
  - Complexity: Medium
  - Risk: Low
  - Priority: P3
  - Files likely involved: `TransactionsTable.tsx`, possibly a backend endpoint
  - Status: Completed. Added an `Export CSV` action in `TransactionsTable.tsx` and on-demand full-history CSV generation in `dashboard/page.tsx` using the existing transactions API.

- [x] Add scheduled portfolio snapshots
  - Description: Create portfolio snapshots periodically, not only after transactions.
  - Why it matters: Makes performance history useful even without frequent trades.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `PriceUpdateScheduler.java`, `SchedulerConfig.java`, `PortfolioServiceImpl.java`
  - Status: Completed. Enabled scheduling, repurposed `PriceUpdateScheduler.java` to capture daily snapshots across users, and reused `PortfolioHistoryServiceImpl` to skip duplicate same-day snapshots and users with no transaction history.

## Documentation

- [x] Rewrite root README

  - Description: Document full-stack setup, actual endpoints, current frontend features, and local commands.
  - Why it matters: Current README is backend-focused and partly outdated.
  - Complexity: Medium
  - Risk: Low
  - Priority: P0
  - Files likely involved: `README.md`
  - Status: Completed. Rewrote the root README around current full-stack setup, local commands, frontend features, API contracts, troubleshooting, environment variables, and known limitations.

- [x] Add environment examples

  - Description: Add example env files for backend and frontend without real secrets.
  - Why it matters: Makes setup repeatable.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `.env.example`, `frontend/.env.example`, docs
  - Status: Completed. Added a root `.env.example` for backend/full-stack local values and clarified the existing frontend `.env.example`.

- [x] Add API reference table

  - Description: Document actual endpoints, methods, auth requirements, and request/response shapes.
  - Why it matters: Prevents frontend/backend contract drift.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `README.md`, `docs/REPO_OVERVIEW.md`
  - Status: Completed. Added a current API reference table to the README covering auth, market, portfolio, transaction, paginated transaction, and performance-history endpoints with auth and response-shape notes.

- [x] Add local troubleshooting guide

  - Description: Document DB connection issues, port conflicts, JWT secret problems, and CoinGecko limitations.
  - Why it matters: Helps restart the project quickly after time away.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `README.md`
  - Status: Completed. Added troubleshooting entries for database connection issues, Flyway/schema confusion, port conflicts, JWT secret length, CoinGecko limitations, frontend/backend connectivity, and Playwright browser setup.

- [x] Document known limitations
  - Description: List supported symbols, missing frontend tests, snapshot behavior, and local-only assumptions.
  - Why it matters: Makes gaps explicit.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `README.md`, `docs/REPO_OVERVIEW.md`
  - Status: Completed. Updated known limitations in the README and refreshed stale overview notes around frontend tests, snapshots, config, supported symbols, and local-only assumptions.

## Testing

- [x] Run backend test suite

  - Description: Run all backend tests and fix failures.
  - Why it matters: Backend tests cover important auth, transaction, portfolio, and market behavior.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `backend/src/test/**`
  - Status: Completed locally on 2026-08-08: 55 tests across 17 suites passed, including the PostgreSQL Testcontainers suite.

- [x] Add frontend smoke tests

  - Description: Add tests for dashboard render, auth forms, and transaction form validation.
  - Why it matters: Frontend currently has no tests.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `frontend/src/**`, `frontend/package.json`
  - Status: Completed with a minimal Vitest + Testing Library setup and smoke coverage for dashboard rendering, login/register form behavior, and transaction form validation.

- [x] Add API client tests

  - Description: Test auth error handling and response parsing in `api.ts`.
  - Why it matters: Protects the frontend/backend API contract.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `frontend/src/lib/api.ts`
  - Status: Completed with `api.test.ts` coverage for 401 auth-session clearing, holdings number coercion, and transaction error-message parsing/fallback behavior.

- [x] Add backend test for unsupported market symbols

  - Description: Confirm unknown symbols return expected behavior.
  - Why it matters: Clarifies how the UI should handle unsupported coins.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `MarketDataServiceImplTest.java`
  - Status: Completed with test coverage showing unsupported symbols are omitted from `getCurrentPrices()` results and return `BigDecimal.ZERO` from `getCurrentPrice()`.

- [x] Add backend test for portfolio market-data availability flags

  - Description: Confirm unsupported symbols set `marketPriceAvailable=false` and `hasUnsupportedMarketData=true`.
  - Why it matters: Protects the new contract that prevents misleading zero-value portfolio display.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `PortfolioServiceImplTest.java`, `PortfolioSummaryIntegrationTest.java`
  - Status: Completed with unit and integration coverage confirming unsupported prices set holding-level `marketPriceAvailable=false` and portfolio-level `hasUnsupportedMarketData=true`.

- [x] Add backend performance history integration test

  - Description: Confirm snapshots are created after transactions and returned by range.
  - Why it matters: Snapshot creation is best-effort and can silently fail.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `PortfolioPerformanceHistoryIntegrationTest.java`, `TransactionControllerIntegrationTest.java`
  - Status: Completed. `PortfolioPerformanceHistoryIntegrationTest.java` already verifies that transaction creation persists a snapshot and that `/api/portfolio/performance/history?range=7d` returns it.

- [x] Add full-stack happy-path test
  - Description: Register, login, create transaction, and verify dashboard data.
  - Why it matters: Highest confidence that the app works as a product.
  - Complexity: High
  - Risk: Medium
  - Priority: P3
  - Files likely involved: frontend test setup, backend test/dev server
  - Status: Completed in `frontend/e2e/happy-path.spec.ts`; the test passes locally with the H2 backend profile and mocked market server.

## Refactoring

- [x] Consolidate backend configuration

  - Description: Decide whether `application.yaml` or `application.properties` is the source of truth.
  - Why it matters: Overlapping config makes runtime behavior confusing.
  - Complexity: Low
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `application.yaml`, `application.properties`
  - Status: Completed. Consolidated backend runtime settings into `application.yaml` and removed the overlapping `application.properties` file so YAML is the single source of truth.

- [x] Use configured CoinGecko base URL

  - Description: Replace hardcoded CoinGecko URL in `MarketDataServiceImpl` with `crypto.api.base-url`.
  - Why it matters: Makes the existing config meaningful.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `MarketDataServiceImpl.java`, `application.yaml`

- [x] Remove or use WebClient config

  - Description: Either switch market calls to `WebClient` or remove the unused WebClient setup/dependency.
  - Why it matters: Reduces unused abstractions and dependencies.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P2
  - Files likely involved: `WebClientConfig.java`, `pom.xml`, `MarketDataServiceImpl.java`
  - Status: Completed. Removed the unused `WebClientConfig.java` bean and `spring-boot-starter-webclient` dependency because market data already uses `RestTemplate` via `AppConfig.java`.

- [x] Clarify holdings source of truth

  - Description: Decide whether holdings should remain stored state or be derived entirely from transactions.
  - Why it matters: Current code stores holdings and transactions, which can drift.
  - Complexity: High
  - Risk: High
  - Priority: P2
  - Files likely involved: `HoldingRepository.java`, `TransactionServiceImpl.java`, `PortfolioServiceImpl.java`, migrations, tests
  - Status: Completed. Clarified and aligned the design so `holdings` is the source of truth for current open positions, while `transactions` remains the audit log and realised-P/L source. `PortfolioServiceImpl` now reads current positions from `HoldingRepository` instead of rebuilding them from transaction history.

- [x] Clean up empty migration

  - Description: Document why `V2__transaction_constraints.sql` is empty or handle it safely.
  - Why it matters: Empty migrations confuse future schema work.
  - Complexity: Low
  - Risk: Medium
  - Priority: P2
  - Files likely involved: `V2__transaction_constraints.sql`, docs
  - Status: Completed. Documented `V2__transaction_constraints.sql` as an intentional no-op placeholder retained to keep existing Flyway version history stable.

- [x] Extract dashboard data loading if it grows

  - Description: Move dashboard loading logic into smaller hooks/helpers once the page becomes harder to maintain.
  - Why it matters: Keeps dashboard code manageable.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `dashboard/page.tsx`, possible hooks/helpers
  - Status: Completed. Extracted the dashboard's backend data loading, pagination, refresh, transaction-save, and CSV export logic into `useDashboardData.ts`, leaving `dashboard/page.tsx` focused on UI composition and local watchlist/filter state.

- [x] Align frontend dependency versions
  - Description: Review Next, React type packages, and `eslint-config-next` versions.
  - Why it matters: Avoids tooling friction.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `frontend/package.json`, `frontend/package-lock.json`
  - Status: Completed. Reviewed the installed frontend dependency graph, kept the React 19 runtime/types alignment, and pinned `next` to `16.2.9` so it stays in lockstep with `eslint-config-next@16.2.9`.
