# Release and recovery gate

Do not deploy to real-user data until all checks below have evidence. Repository checks do not certify a live host or a backup that has never been restored.

## Before upgrading

1. Confirm CI tests, npm/Maven advisory scans, and the MEDIUM/HIGH/CRITICAL container scan are green for the exact release SHA. Do not bypass a scanner because its advisory service is unavailable.
2. Take an encrypted, access-restricted PostgreSQL backup using `pg_dump --format=custom --file=release-backup.dump` with credentials supplied through your secrets manager/PG service configuration, not checked-in files. Record database/server versions, SHA, backup timestamp, and checksum. Confirm retention and storage encryption.
3. Before V6, check normalized identity collisions:

   ```sql
   SELECT lower(trim(email)) AS normalized_email, count(*)
   FROM users GROUP BY lower(trim(email)) HAVING count(*) > 1;
   ```

   Resolve account ownership manually. Migration fails on collisions; it never deletes or merges accounts automatically. Normalization changes mixed-case JWT subjects: affected existing sessions must sign in again.
4. Rehearse V1–V7 against a restored staging copy, including transaction edit/delete, sub-cent prices, partial sells, and snapshots. V7 streams active ledger rows and recalculates cost/profit from retained inputs. Never edit applied migration files. Historical prices already rounded by old versions and original custom names discarded by shared metadata cannot be reconstructed: owners must correct affected records.

### PR #11 pre-release migration correction

The 2026-10-26 review widened the **new, unmerged** V5 numeric columns to preserve the full integer range of V4 prices and cumulative holdings. Fractional scales remain 12 for prices/averages and 20 for cost basis/profit. V1–V4 were not changed. The upgrade test includes a legacy unit price of `10000000000000000` and a cost basis of `1000000000000000000`; both must migrate without overflow. New API input limits remain unchanged.

If you already ran an earlier revision of this PR against a database, its V5 checksum/schema will differ. Do **not** blindly run Flyway repair or change migration-history records: that does not widen existing columns or replay data. Recreate only a confirmed disposable development database, or back up retained data and plan an explicit forward reconciliation of its schema and ledger before release. No live database was changed during this review.

## Deployment configuration

- Use Java 25 LTS (local verification uses Temurin 25.0.4+7), Node 24 LTS, and the committed lockfile. The runtime selects the official `25-jre-ubi10-minimal` variant with vendor updates rather than an implicit OS/full userspace; its Java patch is Temurin 25.0.4.1+1. Build images from the release SHA, record their immutable digest, and refresh/scan base images regularly. Do not claim the floating Java 25 image tag pins a particular patch.
- The backend image defaults to `prod`, runs as UID/GID 10001, and starts Java as PID 1. Supply unique random `JWT_SECRET` (at least 32 bytes), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and exact HTTPS `APP_CORS_ALLOWED_ORIGINS`. No development key/default database credentials are permitted. Mount certificates readable by UID 10001; use `sslmode=verify-full` and the provider's trusted root certificate. Do not disable certificate/hostname verification.
- Terminate public TLS at the hosting edge; keep the API/database on private networks. Do not publish local PostgreSQL/pgAdmin beyond loopback. Do not upgrade the Compose PostgreSQL major version on an existing volume without a supported database migration.
- `/actuator/health/**` stays public; other actuator endpoints require `ADMIN`. Provision operators through a controlled database/administrative workflow; public signup only creates `USER`.
- Forwarded headers are not trusted by default. Before enabling proxy-aware client addresses, configure Tomcat's trusted proxy allowlist for the actual network and ensure the edge strips client-supplied forwarding headers. Verify that two real client IPs receive separate rate-limit buckets and a spoofed header cannot bypass a limit. Multi-instance deployments require a shared edge limiter; the in-memory application limiter is per process.
- Confirm readiness, sign-in, protected data isolation, quote freshness, a buy/sell/edit/delete cycle, CSV output, and mobile layout on staging. Check logs for migration/snapshot failures. Snapshot writes are best-effort after commit; scheduled jobs page users and isolate failures. Network I/O is outside snapshot write locks but post-commit capture is still synchronous and bounded by upstream timeouts.

## Restore rehearsal and rollback

1. Create a **new, empty, isolated staging database**; never restore into the live database as a test.
2. Restore with `pg_restore --exit-on-error --no-owner --dbname=<isolated-staging-database> release-backup.dump`. Supply secrets out of band. Do not use `--clean` against production.
3. Start the release against that database with production-equivalent TLS and settings. Verify Flyway history, expected user/trade/revision counts, representative holdings/profit, authorization isolation, and snapshot quality. Record elapsed restore time and data age to establish measured RTO/RPO; none are guaranteed yet.
4. If the application release fails, stop writes and prefer a forward fix. Do not run the old numeric-scale entity mapping against V5+ schema. A database rollback requires the rehearsed backup restore into a new database plus explicit cutover approval and reconciliation of writes since backup. Preserve the failed database for investigation.

## Explicit residual product/security policy

Registration still returns a JWT on success and an error on duplicate email. This intentionally preserves the API contract and exposes whether an account exists despite generic error text. Rate limits reduce abuse but do not prevent enumeration. A non-enumerating public flow requires a uniform email-verification/registration acknowledgement, a configured mail provider, and a coordinated API/UI migration. Password recovery, verification, server-side token revocation, and account deletion remain before-public-launch product work; no mail provider or destructive account lifecycle was invented by this hardening change.

Tokens remain in browser storage; XSS prevention and dependency maintenance are essential. Session events/expiry improve UI consistency but are not server-side revocation. No penetration test, full WCAG audit, production restore, or distributed load test is implied by CI success.
