# Deployment, operations and recovery

Version 1.0.0-rc.1. This is an operator runbook, not evidence that deployment was executed in the authoring environment. Complete the release gates before production use.

## Local evaluation

Prerequisites: Docker with Compose, Python 3, internet access for image/npm/Maven downloads. The source ZIP includes a compiled UI preview, but no compiled server JAR or container layers.

```bash
python scripts/setup.py --demo --email owner@example.test
docker compose up --build -d
docker compose ps
docker compose logs --tail 100 app
```

Read the generated `.env` privately. Log in at `http://localhost:8080` with its administrator credentials. The setup script refuses to overwrite an existing file and uses random secrets. On Windows, restrict its file ACL. The script does not ask a browser to store secrets or commit anything to GitHub.

A normal empty workspace is created by omitting `--demo`. Bootstrap runs only if there is no organization. Restarting does not reset passwords or recreate users. Never switch a database containing fictional demo records into production by merely changing `DEMO_DATA=false`; provision a separate clean database.

## Topology and privileges

The image contains the compiled frontend and a Spring Boot executable JAR, served on the same origin. PostgreSQL has no host port in the default Compose file. The application port is bound to `127.0.0.1` only. The application runs as UID/GID 10001 with dropped capabilities, a read-only root filesystem and bounded `/tmp`. PostgreSQL 18 storage is mounted at `/var/lib/postgresql`.

Initialization creates separate `pruefpilot_owner` (migration/DDL) and `pruefpilot_app` (DML) roles. The application role cannot alter schema, disable the audit trigger or truncate tables under the supplied grants. The bootstrap `postgres` role remains privileged and must not be used by application code. Migration credentials are available to the app during startup in this starter deployment; a stricter production architecture moves migrations to a separate release job and omits those credentials from the runtime, setting `MIGRATIONS_ENABLED=false` after migrations are applied.

`infra/init-db.sh` runs only on an empty PostgreSQL volume. Editing `.env` does not change existing PostgreSQL role passwords. Rotate them deliberately with privileged SQL and update the secret source atomically. Do not delete a volume as a password-reset procedure.

The application is single-node: in-memory sessions, per-process login/export limits and one provisioned workspace. Do not add replicas without shared sessions, global rate limiting and concurrency/load testing. Tenant keys are a defensive data boundary, not a complete self-service multi-tenant SaaS control plane.

## Configuration reference

| Variable | Meaning / production rule |
|---|---|
| `DATABASE_URL` | JDBC URL; injected to the private DB service by Compose |
| `DATABASE_USER`, `DATABASE_PASSWORD` | Runtime DML role and secret |
| `MIGRATION_USER`, `MIGRATION_PASSWORD` | Schema-owner connection for Flyway, preferably release-job only |
| `POSTGRES_ADMIN_PASSWORD` | Initial PostgreSQL bootstrap superuser secret; not passed to the app |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | One-time account provisioning, not ongoing password reset |
| `ORGANIZATION_NAME` | Initial workspace name |
| `ORGANIZATION_TIMEZONE` | Initial IANA zone; default `Europe/Berlin` |
| `PUBLIC_URL` | Public URL used in production startup validation; actual QR links use the browser origin |
| `APP_PORT` | Local host port, default 8080 |
| `DEMO_DATA`, `DEMO_PASSWORD` | Fictional seed toggle and generated sample-user password; forbidden in production |
| `PRODUCTION` | Enables startup guard requiring HTTPS public URL, secure cookies and no demo |
| `COOKIE_SECURE` | Must be true over public HTTPS; false only for local HTTP |
| `MIGRATIONS_ENABLED` | Spring setting defaults true; apply separately managed migrations before setting false |

A secure-cookie flag on plain HTTP will prevent expected login behavior. Production requires real TLS, not only a string beginning with `https://`.

## Public hosting checklist

Use an appropriately secured host and database region agreed with the customer. Terminate TLS at a maintained reverse proxy, route only the intended public host to the loopback application port and redirect HTTP to HTTPS. Set `PRODUCTION=true`, `COOKIE_SECURE=true`, `DEMO_DATA=false`, and the correct HTTPS `PUBLIC_URL`. Configure trusted forwarded headers only from your proxy, never from arbitrary clients. Spring's `SERVER_FORWARD_HEADERS_STRATEGY=framework` may be supplied after the proxy is configured to replace untrusted forwarding headers; the loopback/private binding is part of that trust boundary.

At the edge, enforce HSTS after testing, a request-body limit slightly above the 11 MiB multipart cap, reasonable request timeouts, an IP-based login limiter and request logging without cookies or bodies. The process-local login limiter sees the actual connection address; behind a proxy, configure a trusted client-address strategy or rely on the edge limiter. Do not accept arbitrary `X-Forwarded-For` as trustworthy.

Keep `/actuator/health` available for monitoring; its response contains no database details. Other exposed actuator paths require authentication. Avoid publishing logs, `.env`, backups or source maps to unrelated public sites. The provided static build includes source maps for portfolio/debugging; remove `.map` files from a hardened public release after checking support needs.

Generate/review the npm lockfile and pin container/action digests after the first successful release build. Use an approved secret manager, restricted host permissions, encrypted disks/backups, patch management and alerting. The example Compose file is a bounded single-host starting point, not an HA, disaster-recovery or compliance certification.

## Health and monitoring

```bash
docker compose ps
curl --fail http://localhost:8080/actuator/health
docker compose logs --tail 200 app
docker compose logs --tail 100 db
```

Monitor health status, failed logins, error rate, latency, DB connections, disk/WAL growth, evidence volume and backup age. No production latency/throughput SLO has been measured here. Establish workload-specific thresholds with a pilot rather than relying on invented benchmarks.

The container has a 1 GiB memory budget and a 128 MiB temporary filesystem. Report exports load bounded evidence into memory and allow two concurrent exports; total historical row counts still affect report size. Load-test with representative evidence and history before raising limits. Never treat the 250-asset cap as a capacity benchmark.

## Backups and restore drills

Evidence is stored in PostgreSQL with metadata, so a consistent database backup covers both. On Linux/macOS/Git Bash:

```bash
sh scripts/backup.sh
```

On PowerShell, avoid older text-redirection behavior corrupting binary `pg_dump` output; use the shell script in a POSIX shell or a vetted backup tool. The script writes a custom-format dump into ignored `backups/`. It is not encrypted by the script. Encrypt, copy off-host, verify completion and apply the agreed retention schedule. Back up the separate secret/configuration source securely; database dumps do not constitute a backup of environment secrets or global roles.

Restore into an **isolated recovery deployment** initialized with the same application/migration roles. Confirm the Compose project and target host before running:

```bash
sh scripts/restore.sh backups/example.dump --confirm-overwrite
```

The script stops the app and invokes `pg_restore --clean --if-exists --exit-on-error`. This overwrites target database objects. It deliberately does not restart the app. Inspect migration version, row counts, original file hashes, accounts and a sample report before `docker compose up -d app`. Rotate credentials if the restore is for incident recovery. Document actual recovery time and recovery-point loss; neither has been measured here.

Use separate Compose project names and volumes for restore drills. Never run the integration test suite against a live customer database: it creates test workspaces and intentionally attempts invalid writes.

## Updates and rollback

Back up first, test the new release on a restored staging database, review migration SQL, build immutable images and record their digests. Flyway applies versioned migrations; never edit a migration already used by a customer. Add a new migration instead. Prefer forward fixes. A code rollback is safe only if the previous version supports the new schema; otherwise restore the tested backup with an explicit data-loss decision.

`docker compose down` stops containers but preserves the database volume. `docker compose down -v` is destructive and belongs only in disposable test environments.

## Account recovery and retention

There is no email password reset. Maintain two named administrators and a protected operational recovery procedure. For emergency recovery, an authorized operator can replace a password hash using a separately verified BCrypt tool and a narrowly scoped, audited SQL change. Never copy a plaintext password into SQL or store it in an issue. After recovery, require a user password change and review audit/host logs. A self-service recovery feature should be added before broad unattended SaaS sales.

There is no automated retention or erasure job. Decide lawful retention, legal holds, data-subject request handling, pseudonymization and deletion before customer onboarding. Append-only audit does not excuse indefinite personal-data retention. An authorized migration/maintenance procedure may be needed; test it on a copy and do not defeat audit controls casually. Obtain qualified privacy/legal review.

## Before accepting customer data

Complete [product release gates](PRODUCT.md#release-gates), actual CI/container runs, security review, malware-scanning decision, restore drill, real-device inspection walkthrough, qualified template review, hosting/DPA terms, support process and operator training. Only then label a deployment production-ready for its agreed scope.
