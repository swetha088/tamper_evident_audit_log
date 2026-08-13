# Implementation Milestones

| Milestone | Scope | Evidence of completion | Status |
| --- | --- | --- | --- |
| M0 — Engineering baseline | Requirements, architecture, risks, AI traceability, attestation, and repository hygiene. | Requirements/architecture/risk documents, completed attestation, and incremental Git history. | Complete |
| M1 — Runnable core | Local HTTP service, persistent store, append-only event API, filters/pagination, SHA-256 chain, and verifier. | Automated tests including direct-store tampering. | Complete |
| M2 — Privacy and lifecycle | Soft archival, structured redaction using commitments, and verifiable filtered export. | Automated lifecycle/redaction/export tests. | Complete |
| M3 — Compliance reporting | Constrained client-account access report built from the audit APIs. | Report filter and scope documentation/tests. | Complete |
| M4 — Handoff readiness | Maven validation, setup verification, security profile review, and final summary. | `mvn verify`: 35 reported tests, one intentionally skipped MySQL-only class, 95.32% line coverage, format/SBOM/coverage gates. | Complete |
| M5 — Production-prototype hardening | Flyway migrations, HMAC checkpoint, least-privilege HTTP roles, offline export verification, MySQL CI path, and repeatable performance baseline. | Flyway V1/V2, role tests, downloaded-bundle tests, MySQL 8.4 GitHub Actions job, and 1,000-event H2 benchmark. | Complete for prototype |

## Decision log

| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-08-13 | Use Java 21, Maven, Spring Boot 4.1.0, Spring Data JPA, H2 locally, and MySQL in production. | Spring profiles isolate local convenience configuration from production database credentials and schema validation. |
| 2026-08-13 | Use server-assigned UTC `recordedAt` for chain ordering; retain optional client `occurredAt` separately. | Prevents callers from controlling audit sequence timing while preserving event-time context. |
