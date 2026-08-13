# Tamper-Evident Audit Log Service

A production-minded prototype of an append-only audit log service. The service will record security-relevant events, make direct data-store tampering detectable with a cryptographic hash chain, and provide query, verification, retention/redaction, and verifiable-export capabilities.

> Status: the complete prototype scope is implemented: append/query/verification, redaction/retention, verifiable export, client-account reporting, and profile-specific security. Production deployment hardening remains documented as follow-up work.

## Goals

- Provide an append-only write API for audit events.
- Query events by actor, resource, event type, time range, and pagination.
- Detect altered historical records through a SHA-256 hash chain.
- Support retention, privacy-preserving redaction, and independently verifiable exports.
- Demonstrate a clarified compliance-reporting use case for access to client account data.

## Documentation

- [Requirements and scope](docs/requirements.md)
- [Execution plan](docs/execution-plan.md)
- [Architecture and design decisions](docs/architecture.md)
- [Draft API contract](docs/api-contract.md)
- [OpenAPI definition](docs/openapi.yaml)
- [Validation, security, and risks](docs/validation-and-risks.md)
- [Testing and validation evidence](docs/testing.md)
- [AI usage traceability](docs/ai-usage-log.md)
- [Final engineering summary](docs/final-engineering-summary.md)

## Local setup

Prerequisites: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn verify
mvn spring-boot:run
```

`mvn test` automatically runs every success and exception scenario. For an IDE launch that explicitly runs the full test set, run `AuditApplicationTestSuite`.

`mvn verify` enforces a JaCoCo bundle line-coverage threshold of **85%**. The local HTML report is written to `target/site/jacoco/index.html`; CI uploads it with the build reports artifact.

The service listens on `http://localhost:8080` and persists a local H2 database under `data/`. The H2 URL runs in MySQL compatibility mode to reduce local/production differences.
Flyway owns the database schema. A first run creates the schema from `src/main/resources/db/migration`; an existing local schema is baselined at version 1 so it can be adopted without destructive changes.

Interactive API documentation is available at `http://localhost:8080/swagger-ui.html` after startup.

For production, use the `prod` profile with a MySQL JDBC URL, database credentials, and HTTP Basic credentials. These values must come from a secret manager or the deployment environment; they are never stored in this repository:

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:AUDIT_DB_URL='jdbc:mysql://host:3306/audit_log'
$env:AUDIT_DB_USERNAME='audit_app'
$env:AUDIT_DB_PASSWORD='replace-with-a-secret-manager-value'
$env:AUDIT_SECURITY_USERNAME='audit_api'
$env:AUDIT_SECURITY_PASSWORD='replace-with-a-secret-manager-value'
mvn spring-boot:run
```

The full CI workflow also validates Flyway and the append/verify path against MySQL 8.4. To run only that test locally, provide a disposable MySQL database and set `RUN_MYSQL_INTEGRATION=true`, `AUDIT_TEST_DB_URL`, `AUDIT_TEST_DB_USERNAME`, and `AUDIT_TEST_DB_PASSWORD`, then run:

```powershell
mvn -Dtest=MySqlAuditIntegrationTest test
```

The default local profile permits unauthenticated requests only for developer convenience. The `prod` profile requires HTTP Basic authentication, disables Swagger/OpenAPI endpoints, and applies route-level roles: `AUDIT_READER`, `AUDIT_WRITER`, `AUDIT_PRIVACY_ADMIN`, and `AUDIT_RETENTION_ADMIN`. Configure least-privilege access through `AUDIT_SECURITY_ROLES`; the documented default grants all four roles only as a deployment bootstrap convenience.

Every current chain-head checkpoint is HMAC-SHA-256 signed. Local development uses a non-secret development value; production requires `AUDIT_INTEGRITY_CHECKPOINT_KEY` (and optionally `AUDIT_INTEGRITY_KEY_ID`). Store the production key in a secret manager, never in source control. This detects database-only checkpoint tampering but is not a substitute for externally immutable checkpoints.

For a repeatable single-process baseline, activate the `performance` profile. It includes the local profile, appends 1,000 probe events, verifies the full chain, and logs append and verification durations. It intentionally does not run in ordinary environments and is not a concurrent or production-MySQL load test:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=performance"
```

Override the count with `--audit.performance.event-count=5000` when invoking the application.

`mvn verify` enforces Java/Maven versions, code formatting, the 85% coverage gate, tests, and a CycloneDX SBOM under `target/`. GitHub Actions repeats that gate for pushes and pull requests; pull requests also receive GitHub dependency review and Dependabot updates Maven and GitHub Action dependencies weekly.

In the `prod` profile, retention is scheduled hourly by default. Set `AUDIT_RETENTION_SCHEDULER_ENABLED=false` to disable it or `AUDIT_RETENTION_SCHEDULER_CRON` to provide an approved Quartz cron expression. Local development leaves the scheduler disabled.

Example write request:

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/audit/events -ContentType 'application/json' -Body '{"eventType":"CLIENT_DATA_READ","actorId":"advisor-7","resourceType":"CLIENT_ACCOUNT","resourceId":"acct-42","payload":{"authorizationOutcome":"ALLOW","requestId":"r-1","sourceApplication":"portal"}}'
```

Then inspect integrity with `Invoke-RestMethod http://localhost:8080/audit/verify`.

To validate a saved export without querying the audit database, submit the saved JSON bundle to `POST /audit/export/verify`, or use `AuditExportVerifier` in a separate Java process. The verifier checks the export format, ordered chain witnesses, selected-record membership, commitment-backed consistency between the displayed and protected payloads, and recomputed content hashes.

## Repository conventions

- Commits should be small and describe the engineering decision or verified change.
- AI-assisted work is recorded in `docs/ai-usage-log.md`; the engineer reviews and owns every accepted change.
- High-impact changes (hash construction, chain verification, retention/redaction semantics, and security controls) require explicit human review before merge.
