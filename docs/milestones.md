# Implementation Milestones

| Milestone | Scope | Evidence of completion | Status |
| --- | --- | --- | --- |
| M0 — Engineering baseline | Requirements, architecture, risks, AI traceability, attestation template, and repository hygiene. | Documentation reviewed; placeholders explicitly retained in attestation. | Complete |
| M1 — Runnable core | Local HTTP service, persistent store, append-only event API, filters/pagination, SHA-256 chain, and verifier. | Automated tests including direct-store tampering. | Complete |
| M2 — Privacy and lifecycle | Soft archival, structured redaction using commitments, and verifiable filtered export. | Automated lifecycle/redaction/export tests. | Complete |
| M3 — Compliance reporting | Constrained client-account access report built from the audit APIs. | Report filter and scope documentation/tests. | Complete |
| M4 — Handoff readiness | Quality gates, setup verification, performance observations, security review, and final summary. | Recorded command output and final review. | In progress |

## Decision log

| Date | Decision | Rationale |
| --- | --- | --- |
| 2026-08-13 | Use Java 21, Maven, Spring Boot 4.1.0, Spring Data JPA, H2 locally, and PostgreSQL in production. | Matches the requested technology stack; Spring profiles isolate local convenience configuration from production database credentials and schema validation. |
| 2026-08-13 | Use server-assigned UTC `recordedAt` for chain ordering; retain optional client `occurredAt` separately. | Prevents callers from controlling audit sequence timing while preserving event-time context. |
