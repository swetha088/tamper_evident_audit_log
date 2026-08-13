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
- [AI usage traceability](docs/ai-usage-log.md)
- [Final engineering summary](docs/final-engineering-summary.md)

## Local setup

Prerequisites: Java 21 and Maven 3.9+.

```powershell
mvn test
mvn spring-boot:run
```

The service listens on `http://localhost:8080` and persists a local H2 database under `data/`. The H2 URL runs in MySQL compatibility mode to reduce local/production differences.

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

The default local profile permits unauthenticated requests only for developer convenience. The `prod` profile requires HTTP Basic authentication for every route.

Example write request:

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/audit/events -ContentType 'application/json' -Body '{"eventType":"CLIENT_DATA_READ","actorId":"advisor-7","resourceType":"CLIENT_ACCOUNT","resourceId":"acct-42","payload":{"authorizationOutcome":"ALLOW","requestId":"r-1","sourceApplication":"portal"}}'
```

Then inspect integrity with `Invoke-RestMethod http://localhost:8080/audit/verify`.

## Repository conventions

- Commits should be small and describe the engineering decision or verified change.
- AI-assisted work is recorded in `docs/ai-usage-log.md`; the engineer reviews and owns every accepted change.
- High-impact changes (hash construction, chain verification, retention/redaction semantics, and security controls) require explicit human review before merge.
