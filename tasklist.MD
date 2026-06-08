# Project Task List

Use this as a working backlog for the crypto portfolio tracker. Tasks are grouped by urgency and type so you can pick them off gradually.

## Must Fix

- [ ] Stabilize the current working tree
  - Description: Review all modified, deleted, and untracked files and decide what should be committed.
  - Why it matters: The repo needs a clean checkpoint before more work.
  - Complexity: Low
  - Risk: Medium
  - Priority: P0
  - Status: Reviewed; package-lock noise was removed. Remaining action is to review and commit the intended changes.
  - Files likely involved: `frontend/frontend/src/**`, `REPO_OVERVIEW.md`, `frontend/frontend/package-lock.json`

- [x] Run full verification
  - Description: Run backend tests, frontend lint, and frontend build.
  - Why it matters: Confirms the current app is usable before adding more features.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Commands:
    - `cd backend/crypto-portfolio-backend && ./mvnw test`
    - `cd frontend/frontend && npm run lint`
    - `cd frontend/frontend && npm run build`

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
  - Files likely involved: `frontend/frontend/package-lock.json`

- [x] Move frontend API base URL to environment config
  - Description: Replace hardcoded `http://localhost:8080/api` with a Next.js environment variable.
  - Why it matters: Makes local setup and future deployment easier.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `frontend/frontend/src/lib/api.ts`, `frontend/frontend/.env.example`, docs

- [x] Update `REPO_OVERVIEW.md`
  - Description: Refresh the overview so it reflects the current transaction-based frontend.
  - Why it matters: Keeps onboarding docs accurate.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `REPO_OVERVIEW.md`

## Should Improve

- [ ] Improve transaction form coin selection
  - Description: Replace free-form symbol/name entry with supported symbol presets and an editable fallback.
  - Why it matters: Reduces invalid coin data and unsupported price lookups.
  - Complexity: Medium
  - Risk: Low
  - Priority: P1
  - Files likely involved: `AddTransactionForm.tsx`, `MarketDataServiceImpl.java`, `types.ts`

- [ ] Show unsupported price states clearly
  - Description: Display a clear “unsupported/no market data” state when a price cannot be fetched.
  - Why it matters: Prevents misleading zero-value portfolio data.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `PriceTable.tsx`, `CryptoCard.tsx`, `PortfolioSummaryCards.tsx`, `PortfolioServiceImpl.java`

- [ ] Improve expired/invalid auth handling
  - Description: Clear stale tokens/profiles and redirect or prompt login when authenticated requests fail.
  - Why it matters: Prevents confusing dashboard states after JWT expiration.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `auth.ts`, `api.ts`, `Navbar.tsx`, `dashboard/page.tsx`

- [ ] Add explicit CORS config
  - Description: Configure the backend to allow the local frontend origin.
  - Why it matters: Makes local frontend/backend integration predictable.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `SecurityConfig.java` or a new backend config class

- [ ] Improve dashboard empty states
  - Description: Make first-run states clearer for no transactions, no holdings, and no snapshots.
  - Why it matters: New users need guidance on what to do first.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `dashboard/page.tsx`, `PriceTable.tsx`, `TransactionsTable.tsx`, `PerformanceHistory.tsx`

- [ ] Separate dashboard loading and error states
  - Description: Let portfolio, transactions, and history fail independently instead of one broad load failure.
  - Why it matters: One failed endpoint should not blank the whole dashboard.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P2
  - Files likely involved: `dashboard/page.tsx`, `api.ts`

## Nice To Have

- [ ] Add portfolio refresh timestamp
  - Description: Show when portfolio data was last refreshed.
  - Why it matters: Helps users trust dashboard values.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `dashboard/page.tsx`

- [ ] Improve home page
  - Description: Replace the basic landing page with a cleaner app entry screen.
  - Why it matters: Current home page is minimal and visually separate from the dashboard.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `frontend/frontend/src/app/page.tsx`

- [ ] Add transaction pagination
  - Description: Paginate backend and frontend transaction history.
  - Why it matters: Prevents large responses once transaction history grows.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `TransactionController.java`, `TransactionRepository.java`, `TransactionsTable.tsx`, `api.ts`

- [ ] Add asset allocation summary
  - Description: Show each holding’s share of total portfolio value.
  - Why it matters: Useful portfolio insight from existing holdings data.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `dashboard/page.tsx`, new component

## Feature Ideas

- [ ] Add portfolio performance chart
  - Description: Visualize `totalCurrentValueUsd` and `totalProfitLossUsd` over time.
  - Why it matters: Makes the existing snapshot feature more useful.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `PerformanceHistory.tsx`, new chart component

- [ ] Add realised vs unrealised P/L breakdown
  - Description: Improve the summary UI with a clearer visual comparison of realised and unrealised profit/loss.
  - Why it matters: Backend already exposes both values.
  - Complexity: Low
  - Risk: Low
  - Priority: P3
  - Files likely involved: `PortfolioSummaryCards.tsx`

- [ ] Add supported coin list endpoint
  - Description: Expose backend-supported CoinGecko mappings to the frontend.
  - Why it matters: Prevents frontend/backend supported-symbol drift.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `MarketDataController.java`, `MarketDataServiceImpl.java`, `api.ts`, `AddTransactionForm.tsx`

- [ ] Add CSV export for transactions
  - Description: Let the user download transaction history as CSV.
  - Why it matters: Practical for personal finance records.
  - Complexity: Medium
  - Risk: Low
  - Priority: P3
  - Files likely involved: `TransactionsTable.tsx`, possibly a backend endpoint

- [ ] Add scheduled portfolio snapshots
  - Description: Create portfolio snapshots periodically, not only after transactions.
  - Why it matters: Makes performance history useful even without frequent trades.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `PriceUpdateScheduler.java`, `SchedulerConfig.java`, `PortfolioServiceImpl.java`

## Documentation

- [ ] Rewrite root README
  - Description: Document full-stack setup, actual endpoints, current frontend features, and local commands.
  - Why it matters: Current README is backend-focused and partly outdated.
  - Complexity: Medium
  - Risk: Low
  - Priority: P0
  - Files likely involved: `README.md`

- [ ] Add environment examples
  - Description: Add example env files for backend and frontend without real secrets.
  - Why it matters: Makes setup repeatable.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `.env.example`, `frontend/frontend/.env.example`, docs

- [ ] Add API reference table
  - Description: Document actual endpoints, methods, auth requirements, and request/response shapes.
  - Why it matters: Prevents frontend/backend contract drift.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `README.md`, `REPO_OVERVIEW.md`

- [ ] Add local troubleshooting guide
  - Description: Document DB connection issues, port conflicts, JWT secret problems, and CoinGecko limitations.
  - Why it matters: Helps restart the project quickly after time away.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `README.md`

- [ ] Document known limitations
  - Description: List supported symbols, missing frontend tests, snapshot behavior, and local-only assumptions.
  - Why it matters: Makes gaps explicit.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `README.md`, `REPO_OVERVIEW.md`

## Testing

- [ ] Run backend test suite
  - Description: Run all backend tests and fix failures.
  - Why it matters: Backend tests cover important auth, transaction, portfolio, and market behavior.
  - Complexity: Low
  - Risk: Low
  - Priority: P0
  - Files likely involved: `backend/crypto-portfolio-backend/src/test/**`

- [ ] Add frontend smoke tests
  - Description: Add tests for dashboard render, auth forms, and transaction form validation.
  - Why it matters: Frontend currently has no tests.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `frontend/frontend/src/**`, `frontend/frontend/package.json`

- [ ] Add API client tests
  - Description: Test auth error handling and response parsing in `api.ts`.
  - Why it matters: Protects the frontend/backend API contract.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `frontend/frontend/src/lib/api.ts`

- [ ] Add backend test for unsupported market symbols
  - Description: Confirm unknown symbols return expected behavior.
  - Why it matters: Clarifies how the UI should handle unsupported coins.
  - Complexity: Low
  - Risk: Low
  - Priority: P2
  - Files likely involved: `MarketDataServiceImplTest.java`

- [ ] Add backend performance history integration test
  - Description: Confirm snapshots are created after transactions and returned by range.
  - Why it matters: Snapshot creation is best-effort and can silently fail.
  - Complexity: Medium
  - Risk: Low
  - Priority: P2
  - Files likely involved: `PortfolioPerformanceHistoryIntegrationTest.java`, `TransactionControllerIntegrationTest.java`

- [ ] Add full-stack happy-path test
  - Description: Register, login, create transaction, and verify dashboard data.
  - Why it matters: Highest confidence that the app works as a product.
  - Complexity: High
  - Risk: Medium
  - Priority: P3
  - Files likely involved: frontend test setup, backend test/dev server

## Refactoring

- [ ] Consolidate backend configuration
  - Description: Decide whether `application.yaml` or `application.properties` is the source of truth.
  - Why it matters: Overlapping config makes runtime behavior confusing.
  - Complexity: Low
  - Risk: Medium
  - Priority: P1
  - Files likely involved: `application.yaml`, `application.properties`

- [x] Use configured CoinGecko base URL
  - Description: Replace hardcoded CoinGecko URL in `MarketDataServiceImpl` with `crypto.api.base-url`.
  - Why it matters: Makes the existing config meaningful.
  - Complexity: Low
  - Risk: Low
  - Priority: P1
  - Files likely involved: `MarketDataServiceImpl.java`, `application.yaml`

- [ ] Remove or use WebClient config
  - Description: Either switch market calls to `WebClient` or remove the unused WebClient setup/dependency.
  - Why it matters: Reduces unused abstractions and dependencies.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P2
  - Files likely involved: `WebClientConfig.java`, `pom.xml`, `MarketDataServiceImpl.java`

- [ ] Clarify holdings source of truth
  - Description: Decide whether holdings should remain stored state or be derived entirely from transactions.
  - Why it matters: Current code stores holdings and transactions, which can drift.
  - Complexity: High
  - Risk: High
  - Priority: P2
  - Files likely involved: `HoldingRepository.java`, `TransactionServiceImpl.java`, `PortfolioServiceImpl.java`, migrations, tests

- [ ] Clean up empty migration
  - Description: Document why `V2__transaction_constraints.sql` is empty or handle it safely.
  - Why it matters: Empty migrations confuse future schema work.
  - Complexity: Low
  - Risk: Medium
  - Priority: P2
  - Files likely involved: `V2__transaction_constraints.sql`, docs

- [ ] Extract dashboard data loading if it grows
  - Description: Move dashboard loading logic into smaller hooks/helpers once the page becomes harder to maintain.
  - Why it matters: Keeps dashboard code manageable.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `dashboard/page.tsx`, possible hooks/helpers

- [ ] Align frontend dependency versions
  - Description: Review Next, React type packages, and `eslint-config-next` versions.
  - Why it matters: Avoids tooling friction.
  - Complexity: Medium
  - Risk: Medium
  - Priority: P3
  - Files likely involved: `frontend/frontend/package.json`, `frontend/frontend/package-lock.json`
