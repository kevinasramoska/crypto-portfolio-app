# Project Audit and Prioritized Roadmap

Updated: 2026-10-25. Code inspection, dependency queries, and isolated runtime/browser probes were performed on 2026-10-24.

## Assessment

This is a working full-stack portfolio project with substantive backend engineering: an audit-preserving transaction ledger, atomic replay, per-user write locks, PostgreSQL migration tests, and full-stack CI. These are stronger resume examples than adding more dashboard widgets.

The baseline had important correctness, security-maintenance, and frontend usability gaps. No P0 application exploit was demonstrated; dependency version matches included high/critical ratings requiring remediation and reachability review. The remediation and new regression evidence below supersede the old passing-test baseline. Remaining launch gates are explicit rather than treated as completed work.

Baseline reviewed revision: `d20deeefd3a45f5a3b66bd47578b0f1147c13d46`. [PR #10](https://github.com/kevinasramoska/crypto-portfolio-app/pull/10) was reviewed (no outstanding findings; all CI checks green) and merged into `main` as `ba67236` on 2026-10-25. PR #9, containing the Java 25 upgrade, is also merged. Deployment is not implied by either merge.

The original evidence below is retained as a baseline, not a description of the hardened branch. The user authorized a combined high/medium remediation PR after this audit; implementation status follows.

## Remediation status — `codex/audit-hardening`

| Finding | Implemented change |
|---|---|
| A01 | Boot 4.1.2 managed dependency fixes; 101 runtime packages rescanned with zero OSV matches; npm audit zero vulnerabilities. Weekly Dependabot, Maven/npm CI advisory gates, and a pinned Trivy container gate added. |
| A02/A03 | Twelve-decimal unit prices, exact remaining cost basis, high-precision realised profit, cent rounding only after aggregation/profit calculation, and small-price display. New V5–V7 migrations rebuild existing cost/profit from retained ledger inputs without deleting audit revisions. Final review added cross-position and half-cent partial-sell regressions. |
| A04 | Batched price warming, single refresh in flight, finite network timeouts, bounded retry/backoff, five-minute fallback limit, and additive quote timestamp/status endpoint. Old quotes are labelled, never timestamped as a fresh fetch. |
| A05 | Snapshots carry `valuationComplete`; stale/partial and uncertified legacy points are excluded from chart segments and labelled in tables. Sampling days use UTC. History is explicitly point-in-time, not reconstructed or cash-flow-adjusted returns. |
| A06/A07 | Mobile grid constraints, request-generation guards for ranges/pages/watchlist/session, and corrected last-row pagination refresh. Phone/tablet/desktop width assertions added to full-stack browser coverage. |
| A08/A09 | Explicit blank-price rejection while keeping intentional zero; symbol/name/email/quantity/price bounds; UTF-8 BCrypt byte limit; structured 400/409 errors; authentication 401 distinguished from permission 403 without unwanted logout. |
| A10 | Public health only; other actuator endpoints require ADMIN, with anonymous/USER/ADMIN tests. |
| A11 | Supported asset names are canonical; custom names belong to each user's transaction/holding. Race-safe shared catalog insertion and formula-safe CSV text cells. Legacy custom names already lost by the old shared table need owner correction. |
| A12 | Normalized identities, collision-safe migration, same-page/cross-tab session events, expiry updates, and stale-session response guards. **Residual:** public registration enumeration/lifecycle policy below is not solved by changing error wording. |
| A13 | Keyset user batches, isolated per-user write transactions/failures, source quotes fetched before acquiring snapshot write locks, and duplicate-day checks under lock. Post-commit capture remains synchronous with bounded upstream I/O. |
| A14 | API pages capped at 100; database totals aggregation; chunked CSV and bounded legacy history (10,000 rows); cross-user first-use concurrency and 1,000-row replay checks. Replay remains linear by design, not a claim of unlimited scale. |
| A15 | Non-root production-default image, fail-fast production secret/TLS/origin validation, loopback local database/admin ports, and explicit release/restore/trusted-proxy runbook. Live hosting/proxy configuration still requires operator verification. |

See [RELEASE_RUNBOOK.md](RELEASE_RUNBOOK.md) for migration preflight, backup/restore, exact production requirements, and residual launch gates.

### Verification evidence

- Clean Java 25 backend run: **79 tests passed**, including nine real PostgreSQL cases, cross-position/half-cent rounding, a V4→V7 legacy-data upgrade, concurrent first-use catalog insertion, 1,000 active-row replay, and dump/restore into a separate disposable database.
- Frontend: **39 tests passed**, including last-row pagination and same-tab/cross-tab expiry/logout cases; coverage generation, lint, TypeScript, and optimized Next.js build passed. The coverage report is evidence of tested paths, not a claim of full coverage.
- Chromium full-stack flow passed: registration/login, buy, holdings/summary, edit, delete, and no document overflow at **360/390/768/1440px**. The backend used a local deterministic market provider, not live CoinGecko.
- Final clean-run measurements on this local Intel Mac/Docker setup: 1,000-row replay plus committed snapshot **332ms**; synthetic dump/restore **949ms**, after application/container startup. An earlier run measured 781ms/1,666ms, demonstrating local variability. These are local samples, not production throughput or recovery guarantees.
- npm advisory scan: zero vulnerabilities; Maven OSV runtime scan: **101 packages, zero matches**, checked 2026-10-25. Boot 4.1.2 is a [maintained upstream patch release](https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/).
- Final container: official Java 25 UBI 10 minimal runtime with vendor updates, non-root UID/GID 10001; Trivy 0.76.1 reports **zero MEDIUM/HIGH/CRITICAL findings** across the Red Hat 10.2 OS and application JAR, without exceptions/suppressions. The default Ubuntu image had four HIGH matches in unused Pebble; Ubuntu 24.04 retained 25 MEDIUM matches, and vendor-updated Ubuntu 26.04 retained 44 MEDIUM matches. Selecting a supported minimal runtime avoids unused service-manager tooling and the affected Ubuntu userspace. These are scanner/vendor severity results, not proof of no possible vulnerabilities. CI enforces all three severities.
- A local lint pass initially raced with Playwright removing its generated results directory; rerunning passed. Generated browser report directories are now explicitly excluded from lint. The new regression-test setup typo and constructor-selection startup failure were fixed and rerun before submission.

The synthetic restore is not a restore of production data and does not certify a hosting provider, RTO, or RPO. No external deployment, live database migration, or real-user account changes were performed.

### Deliberately unresolved decisions

Preserving the current registration API (JWT on success, error for duplicate) permits email enumeration. A uniform acknowledgement/email-verification flow needs a configured mail provider and an agreed contract change; no provider, recovery system, destructive account deletion, or deployment was silently introduced. This remains a public-launch gate, not a completed security fix. A production backup restore, trusted-proxy proof, distributed rate limiter, penetration test, and full accessibility audit cannot be certified from local repository checks.

## Baseline scope and evidence — 2026-10-24

- Inspected backend controllers, DTOs, services, repositories, entities, migrations, security filters, configuration, snapshot scheduling, and test coverage.
- Inspected frontend routes, forms, components, authentication/API helpers, data-loading hook, tests, and build configuration.
- Read all nine existing Markdown files, including the uppercase `docs/tasklist.MD`, agent guidance, and both PR templates.
- Rechecked `npm audit`: **zero known vulnerabilities on the reviewed branch**. This result covers npm dependencies, not Java or container images.
- Resolved Maven's dependency tree and queried OSV for 100 compile/runtime dependency coordinates, excluding test/provided dependency subtrees: **8 package coordinates matched 18 distinct advisory IDs**. Version matches are not proof that every advisory is exploitable here.
- Reused validation evidence from the same revision: 62 backend tests, 22 frontend tests, lint, production build, Chromium happy path, and all six push/PR CI checks passed. Those suites were not rerun just to edit these documents.
- Independently reproduced the findings called out below using an isolated H2 backend, controlled market prices, and Chromium. The production frontend build was viewed at 1440px and 390px widths with browser API traffic redirected to that isolated backend. This did not validate a deployed environment or production CORS.
- A narrow pattern scan of tracked files found no matching private-key blocks, GitHub personal access tokens, or AWS access-key IDs. This was not a full historical secret scan.

## P1: address first

### A01 — Backend dependency advisories need remediation

**Baseline dependency matches; application exploitability varies.** The audited Boot `4.1.0` resolved the versions below. This branch upgrades to maintained Boot `4.1.2` and the new runtime scan reports zero advisory matches; this table records the pre-fix evidence.

| Component | Resolved version | Evidence and relevance |
|---|---|---|
| Tomcat embed core | 11.0.22 | OSV matches three authentication/constraint advisories. Apache also lists later fixes through 11.0.27. The matched DIGEST/FORM/container-constraint prerequisites were not found in this JWT-filter application; do not describe these as a proven JWT bypass. See [Apache advisories](https://tomcat.apache.org/security-11.html). |
| Spring Web MVC | 7.0.8 | Matches SSE-fragment and XSLT-view advisories; their relevant features were not found in these REST controllers. Spring identifies 7.0.9 as the fix for the [XSLT issue](https://spring.io/security/cve-2026-47884/). Vendor and scanner severity ratings differ. |
| Jackson core/databind, both 2.x and 3.x | 2.21.4 / 3.1.4 | Multiple matches, including resource-exhaustion issues. Review actual deserialization paths; most databind advisories require specialized target types or annotations absent from the DTOs. The [numeric-string parsing advisory](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-p6pp-m3f8-5c89) merits a bounded endpoint regression check. Listed fixes include core 2.21.7 / 3.1.7 and databind 2.21.7 / 3.1.7 for these release lines. |
| PostgreSQL JDBC | 42.7.11 | [Channel-binding advisory](https://github.com/pgjdbc/pgjdbc/security/advisories/GHSA-j92g-9f8w-j867), fixed in 42.7.12. Requires `channelBinding=require`; that setting is absent from checked-in local configuration, and deployed DB settings were not inspected. |
| Log4j API | 2.25.4 | [JSON MapMessage encoding advisory](https://logging.apache.org/security.html#CVE-2026-49844), fixed in 2.25.5 on this line. The relevant logging usage was not found; this is not a Log4Shell finding. |

**Done when:** the resolved runtime graph is rescanned, every remaining match has a documented applicability decision, and the PostgreSQL, authentication, malformed-input, and full-stack tests pass. Add recurring Maven/npm and container scanning to CI; the current workflow runs tests/builds but no security scan.

### A02 — Preserve market-price precision before valuation

**Reproduced.** In `PortfolioServiceImpl.lookupPrice()` (around line 224), `money(price)` rounds a unit price to two decimals before multiplying by quantity. With a controlled `$0.0049` quote and 100 units, the API returned `$0.00` current value and `marketPriceAvailable=true`; the correct value is `$0.49`. Prices just above a rounding boundary can also be overstated.

**Fix:** retain sufficient unit-price precision during calculations; round final USD totals at the defined boundary. Format small prices appropriately in the frontend.

**Done when:** tests cover sub-cent assets, large quantities, fractional quantities, and both rounding directions, including consistent holdings/summary/snapshot responses.

### A03 — Preserve weighted cost basis through ledger replay

**Reproduced.** `TransactionServiceImpl.applyBuy()` calculates an average with eight decimals and immediately rounds it back to two via `scaleMoney()`. Buying one unit at `$0.01` and another at `$0.02` produces average `$0.02` and remaining cost basis `$0.04`, despite `$0.03` of purchases. Subsequent realised P/L inherits this rounding error. Transaction unit prices are also limited to cents by the model/schema.

**Fix:** explicitly define unit-price, average-cost, quantity, and final-money precision. Preserve higher-precision cost basis/averages throughout replay. Use a new Flyway migration for persisted scale changes; do not edit applied migrations. Previously discarded input precision cannot be recovered automatically.

**Done when:** repeated small buys, partial/full sells, edit/delete replay, and PostgreSQL persistence preserve the chosen accounting rules without accumulating artificial gains or losses.

### A04 — Bound stale prices and network latency

**Confirmed by code inspection; outage fallback observed.** `MarketDataServiceImpl` has a 60-second freshness TTL, but `applyFallbackFromCache()` reuses cached prices without any maximum age. Responses contain no quote time or stale status. The dashboard sets “Prices just now” from response-receipt time even when the quote is old. `AppConfig` creates a plain `RestTemplate` with no explicit connect/read timeout, and portfolio pricing fetches assets individually.

**Fix:** configure finite timeouts, batch portfolio quotes, deduplicate concurrent refreshes, bound stale fallback, and return source timestamps/status. Add bounded backoff for provider throttling and failures.

**Test gap:** `getCurrentPrices_shouldFallbackToCachedValue_whenApiFails` makes its second call inside the fresh-cache TTL, so the mocked failure is never exercised. Use an injectable clock to test expiry and actual outage behavior.

**Done when:** an expired quote during a provider outage is visibly stale or unavailable, never labelled freshly fetched; slow providers cannot hold requests indefinitely.

### A05 — Preserve valuation quality in performance history

**Confirmed in the schema and API; response shape reproduced.** Current summaries flag unsupported prices, but `PortfolioSnapshot` and `PortfolioPerformancePointResponse` store/return only numeric totals. Missing prices become zero/partial values in history with no quality flag. Fixing current prices later does not repair these stored points. Historical edits intentionally delete affected snapshots and replace them with a current snapshot, not reconstructed historical market values.

**Fix:** define snapshots as point-in-time observations, persist quote completeness/staleness, and show gaps or partial values explicitly. Decide and document a UTC daily sampling policy. The scheduler currently uses server-local day boundaries and skips the day after any existing snapshot; this is not guaranteed end-of-day valuation.

**Done when:** a missing-price test cannot produce an apparently complete zero-value history point; edit/delete invalidation and missed uptime are honestly explained in the chart/UI.

## P2: usability, security hardening, and robustness

| ID | Finding and evidence | Recommended fix / acceptance check |
|---|---|---|
| A06 | **Mobile overflow reproduced:** at a 390px viewport the populated dashboard document is 878px wide; desktop is correctly 1440px wide. The holdings grid/table expands beyond the viewport. | Constrain grid children and table wrappers, retain intentional table scrolling inside its container, and verify 360/390/768px widths. Make core totals and the transaction action easy to reach on mobile. |
| A07 | **Request race reproduced:** select 7d then 90d; deliver 90d first and 7d last. The 90d button remains selected while the table/chart shows the older 7d result. `useDashboardData` has no response-generation guard or cancellation; pagination/watchlist requests have similar exposure by inspection. | Abort obsolete requests or reject responses from older generations. Add delayed-response tests, including deleting the last row of a later page. Clearing the initial timeout does not cancel requests already started. |
| A08 | **Blank price reproduced:** leaving the purchase price empty submits `priceUsd: 0` and receives `200`. `AddTransactionForm` uses `noValidate` and converts the empty string with `Number()`. | Require an explicit price before number conversion; continue allowing an intentionally entered zero if that remains the API contract. Test blank, whitespace, zero, and invalid numeric input. |
| A09 | **Backend input/error gap reproduced:** quantity `0.000000001` passes DTO positivity, rounds to zero, and the isolated API returned `403`; the same valid token still worked afterward. `authorizedFetch()` clears the session for every 403. Length/precision bounds are also missing from transaction DTOs. | Validate representable ranges before persistence; return structured 400/409 responses for domain/constraint failures. Keep validation failures distinct from authentication failures. Test overlong symbol/name/email, excessive decimals, numeric overflow, and UTF-8 BCrypt byte limits. |
| A10 | **Metrics accessible to ordinary users:** registering a `USER` and calling `/actuator/metrics` returned 200, including HTTP metric names. Existing tests explicitly permit this. | Restrict non-health operations to an operator/admin role or a private management interface; test anonymous, normal-user, and operator access. This is operational information exposure, not a demonstrated leak of passwords/JWTs. |
| A11 | **Shared coin metadata and CSV formula risk:** user A can create a coin named `=1+1`; user B recording the same symbol with a clean name receives A's stored name. `resolveCoin()` uses a shared first-writer-wins table, and CSV escaping only doubles quotes. Spreadsheet applications can interpret formula-leading text. | Use canonical metadata for supported coins and define safe ownership/naming rules for custom assets. Neutralize formula-leading text in exported textual fields. Verify two-user naming and exported content; no spreadsheet payload execution was attempted. |
| A12 | **Auth hardening remains partial:** new registration returns 200 plus a JWT while a duplicate returns 400, so generic error wording does not prevent email enumeration. Backend email normalization is inconsistent with frontend profile normalization. Navbar session state only updates on path changes. | Decide the account-enumeration policy, normalize identity consistently, and synchronize logout/expiry across the current page and tabs. Add tests for mixed-case identity, stale sessions, and registration races. Recovery, email verification, and account deletion remain product gaps before inviting real users. |
| A13 | **Snapshot job holds locks across all users:** `captureSnapshotsForAllUsers()` is one transaction, calls `findAll()`, locks users, and performs market work before committing. The post-commit listener also performs synchronous network-backed work. | Page users and isolate per-user transactions/failures, set network timeouts, and keep external I/O out of long lock windows. Verify a slow/failing user does not block unrelated users or roll back the entire batch. Exact load impact has not been benchmarked. |
| A14 | **Pagination does not bound all workload:** size `2147483647` was accepted. Full-history export, summary calculations, and every ledger replay load all active transactions; holdings are deleted/reinserted on each mutation. | Cap API page size, aggregate totals in the database where appropriate, stream/chunk large exports, and benchmark realistic histories before optimizing replay. Add concurrent first-use coin tests: a per-user lock does not serialize insertion into the global coin table across users. |
| A15 | **Production configuration needs an explicit release gate:** the default profile accepts the published development signing key; Docker does not itself enforce `prod`, runs without a non-root `USER`, and Compose publishes PostgreSQL/pgAdmin on all interfaces with demo credentials. | Require explicit production configuration in the deployment path, use unique secrets and verified DB TLS, bind local services to loopback, and run the container as a non-root user. Confirm trusted-proxy client-IP handling for rate limiting. Deployment, backup/restore rehearsal, and container scan remain unverified. |

## P3: cleanup and presentation

- Make the chart use actual timestamps for horizontal spacing, add a visible legend, distinguish portfolio value from P/L, include units, and supply an accessible chart name/description. A date-only label hides the distinction between several same-day snapshots.
- Drive watchlist suggestions from supported backend symbols: `WatchlistControls.POPULAR_SYMBOLS` currently offers assets absent from the backend mapping. Keep manual unsupported assets a clearly labelled choice.
- The relative-time labels update only when another render occurs; the old claim that both independently update in real time was too strong. Preserve a deliberately emptied watchlist on reload instead of replacing `[]` with defaults.
- Remove technical implementation wording from product copy (for example “from the backend transaction service”). Replace decorative “100%”/infinite-history claims with concrete capabilities.
- Consolidate unused `PortfolioSummary.tsx` (EUR formatting), the unused holding DTO, and empty `MarketDataServiceTest` when touching those areas. These are cleanup items, not release blockers.
- Add a license, contribution/security-reporting guidance, a supported-runtime declaration, and an architecture decision record. No license choice is assumed by this audit.
- Keep the current layered application; there is no demonstrated need for microservices, Kubernetes, a new state library, or an AI feature.

## Existing Markdown and plan validity

| File reviewed | Result |
|---|---|
| `README.md` | Setup, endpoints, and transaction workflow are substantially correct. Added this audit as an entry point and corrected JUnit version wording. Live demo, screenshots, and backup/recovery instructions remain missing. |
| `docs/tasklist.MD` | Most checked features exist. Corrected obsolete “awaiting commit approval” entries, old version/test claims, overbroad auth/real-time wording, and source-of-truth wording. Added the audit priorities without pretending their fixes are done. |
| `docs/REPO_OVERVIEW.md` | Architecture and transaction replay description remain valid. Corrected test framework wording and linked this audit; production-readiness caveats now have a single detailed reference. |
| `docs/PAGINATION_IMPLEMENTATION.md` | Pagination exists, but sorting now uses active `ledgerSequence`, not `createdAt`; loading belongs in `useDashboardData`. Updated implementation notes and removed the obsolete 27-test baseline. Missing limits/performance guarantees are tracked as A14. |
| `docs/AGENTS.md` | Layering/security conventions remain useful. Corrected enabled scheduling, string CoinGecko IDs, actual test mocking, and the unsupported claim of per-test schema recreation. |
| `frontend/AGENTS.md` | Valid generated guidance pointing to installed Next.js documentation; left unchanged. |
| `frontend/CLAUDE.md` | Valid pointer to `AGENTS.md`; left unchanged. |
| `.github/pull_request_template.md` | Valid verification/checklist template. Its unchecked boxes are a per-PR form, not unfinished project backlog. |
| `docs/pull_request_template.md` | Same content as the GitHub template; valid but duplicated. Future consolidation is optional. |

Still valid and implemented: JWT/password hashing, explicit CORS, per-instance auth/market rate limits, user-scoped portfolio/transaction access, PostgreSQL Testcontainers, Flyway migrations, audit-preserving edit/delete, rollback protection, snapshots, pagination, CSV export, dashboard summaries, allocation, charts, frontend smoke tests, CI, and coverage artifacts.

Remaining product/operations backlog after hardening: explicit trade execution date/time with an ordering/backdating policy, server-side transaction filters, complete accessibility review, production backup/recovery rehearsal, environment promotion, structured logging/error monitoring, account lifecycle, and production-scale/distributed load testing. Market resilience, automated scans, a release/restore runbook, synthetic restore, and a measured 1,000-row replay check are now implemented; see remediation evidence above.

Multiple portfolios, more currencies, imports, and alerts remain optional expansions. Defer them until the correctness and demo milestones are complete.

## Original recommended delivery sequence

| Milestone | Scope | Exit criteria |
|---|---|---|
| 1. Security maintenance | Land reviewed PR #10; remediate A01; automate scans; restrict metrics and enforce deployment settings from A10/A15. | Clean or explicitly triaged dependency results; normal users cannot read operations data; production configuration fails safely. |
| 2. Trustworthy calculations | A02/A03, representable input validation from A09, and meaningful PostgreSQL regression cases. | Sub-cent valuation and cost-basis examples are correct through create/edit/delete/reload; invalid inputs produce useful 4xx errors without logging out. |
| 3. Trustworthy data | A04/A05/A13, snapshot time/quality policy, and bounded network behavior. | Users can tell current, stale, partial, and unavailable values apart; no indefinite upstream waits or all-user snapshot transaction. |
| 4. Smooth product demo | A06/A07/A08/A11/A12 plus keyboard/mobile checks and supported watchlist suggestions. | No document overflow at phone widths; ranges/pages cannot show obsolete responses; blank prices rejected; exported text is safe. |
| 5. Portfolio presentation | Seeded demo mode, screenshots, architecture/case study, measured CI/quality evidence, and a release/restore runbook. | A reviewer can understand the project and see populated results in under two minutes. |

Each milestone can be split into small PRs. Do not combine unrelated schema, authentication, and UI changes into one broad upgrade PR.

## Highest-value portfolio and resume additions

1. **A populated demo with no signup requirement.** Use a clearly labelled synthetic portfolio with buys, sells, a corrected transaction, and meaningful history; provide an isolated reset mechanism. Keep real-user writes separate from demo data.
2. **A concise technical case study.** Explain why ledger replay, immutable revision chains, per-user locks, after-commit snapshots, and PostgreSQL-specific tests were chosen. Include one worked edit/rollback example and the precision/freshness rules after they are fixed.
3. **A strong README first screen.** Add a working demo URL when deployed, a desktop screenshot, a mobile screenshot, a short recording, CI status, and links to architecture/API docs. Keep setup commands below the project story.
4. **Evidence for backend skills.** Show meaningful tests for authorization isolation, concurrency, accounting, migrations, and upstream failure. Add measured results for a realistic large portfolio; publish hardware/dataset assumptions rather than inventing performance numbers.
5. **Evidence for frontend skills.** Fix phone layout, make the main portfolio/action visible earlier, improve chart explanation, and test keyboard/focus/error states. The React checklist informed the request-lifecycle and browser-storage review; a new component library is not required.

Resume wording supported by the current implementation:

> Built a Java/Spring Boot and Next.js crypto portfolio tracker with an audit-preserving transaction ledger, atomic holdings/P&L replay, per-user write serialization, PostgreSQL/Flyway integration tests, and GitHub Actions unit/integration/browser checks.

Add deployment, user counts, coverage percentages, or performance improvements only after measuring them. Local JaCoCo output currently combines multiple sessions, so it is not a clean single-run coverage percentage for a resume claim.

## Dependency advisory inventory

OSV package/version matches queried on 2026-10-24; advisory applicability and severity must be reviewed against upstream guidance. This inventory is a point-in-time result, not a claim of 18 exploitable application vulnerabilities.

| Package coordinate(s) | Advisory IDs |
|---|---|
| `org.apache.logging.log4j:log4j-api` | `GHSA-qv9r-c865-cp47` |
| `org.apache.tomcat.embed:tomcat-embed-core` | `GHSA-9xv2-5v5q-p794`, `GHSA-gcx9-497g-6cp6`, `GHSA-h3x4-894j-xpx5` |
| `org.springframework:spring-webmvc` | `GHSA-j9f9-w8pj-32f8`, `GHSA-pc63-qcmh-9cmg` |
| `org.postgresql:postgresql` | `GHSA-j92g-9f8w-j867` |
| `tools.jackson.core:jackson-databind` and `com.fasterxml.jackson.core:jackson-databind` | Both: `GHSA-5gvw-p9qm-jgwh`, `GHSA-cxp5-3px4-pw24`, `GHSA-gx83-3vf8-gh7j`, `GHSA-q4xh-88c3-wmh7`, `GHSA-vvgp-rfg2-7rr6`, `GHSA-wjgm-6hv5-3cvf`, `GHSA-wv8q-qhhj-9h54`. Additional 2.x matches: `GHSA-5jmj-h7xm-6q6v`, `GHSA-mhm7-754m-9p8w`. |
| `tools.jackson.core:jackson-core` and `com.fasterxml.jackson.core:jackson-core` | `GHSA-7hhh-6rmp-j9qf`, `GHSA-p6pp-m3f8-5c89` |

Not certified by local hardening: full Git-history secret scan, live hosting/proxy configuration, penetration testing, full WCAG conformance, production backup restoration, or production-scale load. Local image scanning and a disposable PostgreSQL backup/restore are covered separately above.

## Changed-file guide

Paths below are repository-relative. `B/` means `backend/src/main/java/com/kevinas/crypto_portfolio_backend/`; `T/` means the matching package under `backend/src/test/java/`.

| File | Reason for change |
|---|---|
| `.github/workflows/ci.yml` | Runtime/npm advisory gates, weekly checks, and pinned current container scanner. |
| `.github/dependabot.yml` | Weekly Maven, npm, and Actions maintenance PRs. |
| `scripts/audit-maven.mjs` | Fail-closed advisory checks over exact resolved runtime coordinates. |
| `README.md` | Current versions, precision/freshness contracts, limits, and production setup. |
| `backend/pom.xml` | Boot 4.1.2 managed security/dependency fixes. |
| `backend/Dockerfile` | Vendor-updated minimal Java runtime, non-root execution, production default, correct Java signal handling. |
| `backend/docker-compose.yml` | Bind local database/admin ports to loopback. |
| `backend/src/main/resources/application-prod.yaml` | Require explicit database credentials/URL. |
| `B/config/AppConfig.java` | Finite upstream connect/read timeouts. |
| `B/config/ProductionSafetyConfig.java` | Reject unsafe production keys, TLS, and origins. |
| `B/controller/MarketDataController.java` | Add quote metadata endpoint and cap symbol batches. |
| `B/dto/MarketQuote.java` | Quote price, timestamp, and freshness contract. |
| `B/dto/TransactionRequest.java` | Normalize symbols/names and validate lengths/decimal bounds. |
| `B/dto/LoginRequest.java` | Normalize/bound login identity. |
| `B/dto/RegisterRequest.java` | Normalize/bound registration identity. |
| `B/dto/PortfolioSummaryResponse.java` | Expose stale/unknown quote quality. |
| `B/dto/PortfolioPerformancePointResponse.java` | Expose historical valuation certification. |
| `B/exception/GlobalExceptionHandler.java` | Structured malformed-input and data-conflict responses. |
| `B/model/Holding.java` | Precise average, remaining cost basis, and owned asset name. |
| `B/model/Transaction.java` | Precise unit price/profit and transaction-owned name. |
| `B/model/PortfolioSnapshot.java` | Persist valuation completeness. |
| `B/repository/CoinRepository.java` | Concurrent-safe first-use catalog insertion. |
| `B/repository/HoldingRepository.java` | Fetch coin metadata for detached background work. |
| `B/repository/TransactionRepository.java` | Database-side active totals aggregation. |
| `B/repository/UserRepository.java` | Bounded keyset user batches. |
| `B/security/SecurityConfig.java` | ADMIN metrics, correct 401/403, safe error dispatch. |
| `B/service/AuthServiceImpl.java` | Enforce BCrypt UTF-8 byte bounds. |
| `B/service/MarketDataService.java` | Add compatible metadata access. |
| `B/service/MarketDataServiceImpl.java` | Expiring fallback, non-blocking single refresh, backoff, deterministic clock tests. |
| `B/service/PortfolioService.java` | Separate daily capture entry point. |
| `B/service/TransactionServiceImpl.java` | Exact ledger costs/profit, owned names, request/output bounds. |
| `B/service/impl/PortfolioServiceImpl.java` | Preserve price precision, aggregate totals, and prefetch snapshot quotes outside write locks. |
| `B/service/impl/PortfolioHistoryServiceImpl.java` | Page users and continue after per-user failure. |
| `B/service/impl/PortfolioSnapshotWriter.java` | Isolated locked writes and UTC daily deduplication. |
| `backend/src/main/resources/db/migration/V5__precise_cost_basis_and_valuation_quality.sql` | Add precision, owned-name, and quality schema without rewriting old migrations. |
| `backend/src/main/resources/db/migration/V6__normalized_email_identity.sql` | Normalize identities and reject unresolved collisions atomically. |
| `backend/src/main/java/db/migration/V7__rebuild_precise_positions.java` | Stream retained ledger inputs to repair existing cost/profit. |
| `T/config/ProductionSafetyConfigTest.java` | Test valid configuration and fail-safe production rejection. |
| `T/integration/AuditHardeningIntegrationTest.java` | Financial, naming, freshness, validation, and identity regressions. |
| `T/integration/OperationalEndpointsIntegrationTest.java` | Anonymous/USER/ADMIN access matrix. |
| `T/integration/CoreUserJourneyIntegrationTest.java` | Correct anonymous status expectations. |
| `T/integration/PortfolioPerformanceHistoryIntegrationTest.java` | Correct anonymous history status expectation. |
| `T/integration/TransactionControllerIntegrationTest.java` | Keep correction fixture within validated symbol bounds. |
| `T/integration/PostgresDatabaseIntegrationTest.java` | Migration/replay/concurrency/restore evidence and new decimal contract. |
| `T/service/MarketDataServiceImplTest.java` | Actually expire cache, reject expired/invalid quotes, verify backoff/single refresh. |
| `T/service/PortfolioServiceImplTest.java` | Mock the new database aggregate boundary. |
| `T/service/PortfolioHistoryServiceImplTest.java` | Exercise isolated daily writer/deduplication using UTC. |
| `T/service/SnapshotBatchTest.java` | Confirm paging and failure isolation. |
| `frontend/src/app/dashboard/page.tsx` | Quote freshness, watchlist request guards, and constrained mobile grid. |
| `frontend/src/hooks/useDashboardData.ts` | Independent request generations, session clearing, pagination correction, and bounded safe CSV. |
| `frontend/src/hooks/useDashboardData.test.tsx` | Delayed-range, logout/pending-request, and last-row pagination regressions. |
| `frontend/src/lib/api.ts` | Quote endpoint and correct session handling for 401 versus 403. |
| `frontend/src/lib/api.test.ts` | Verify permission denial and old-session response handling. |
| `frontend/src/lib/auth.ts` | Same-tab/storage events and expiry synchronization. |
| `frontend/src/lib/auth.test.ts` | Verify same-page expiry and other-tab logout notifications. |
| `frontend/src/lib/types.ts` | Add quote-quality fields without changing existing fields. |
| `frontend/src/lib/csv.ts` | Formula-safe text export escaping. |
| `frontend/src/lib/format.ts` | Preserve visible sub-cent unit prices. |
| `frontend/src/lib/csv.test.ts` | Formula, numeric, delimiter, and tiny-price display cases. |
| `frontend/src/components/AddTransactionForm.tsx` | Require explicit price and allow precise unit-price steps. |
| `frontend/src/components/AddTransactionForm.test.tsx` | Blank versus intentional-zero regression tests. |
| `frontend/src/components/EditTransactionForm.tsx` | Apply the same explicit-price/precision policy when editing. |
| `frontend/src/components/CryptoCard.tsx` | Correct USD default and precise small-price display. |
| `frontend/src/components/PriceTable.tsx` | Precise average/current unit-price display. |
| `frontend/src/components/TransactionsTable.tsx` | Preserve small trade-price display. |
| `frontend/src/components/PortfolioSummaryCards.tsx` | Clearly label stale valuations. |
| `frontend/src/components/PerformanceHistory.tsx` | Explain sampling/quality and do not present uncertified totals as valid history. |
| `frontend/src/components/MiniPerformanceChart.tsx` | Break lines at uncertified points rather than chart artificial zero drops. |
| `frontend/src/components/Navbar.tsx` | Update on current-page and cross-tab session changes. |
| `frontend/e2e/happy-path.spec.ts` | Verify populated responsive layout at four widths. |
| `frontend/eslint.config.mjs` | Exclude disposable browser report directories. |
| `docs/AGENTS.md` | Reconcile supported stack, scheduling, and test guidance. |
| `docs/REPO_OVERVIEW.md` | Reconcile ledger, versions, and auth/metrics behavior. |
| `docs/PAGINATION_IMPLEMENTATION.md` | Correct implemented ledger sorting/loading and audit caveats. |
| `docs/tasklist.MD` | Reconcile old checked claims and mark actual remediation versus launch decisions. |
| `docs/PROJECT_AUDIT.md` | Preserve baseline evidence and record fixes, verification, and this file guide. |
| `docs/RELEASE_RUNBOOK.md` | Migration preflight, safe release/restore, proxy checks, and explicit residual policy. |
