# Final Engineering Summary

## Outcome

Delivered a locally runnable Java 21 / Spring Boot 4.1.0 audit-log prototype. It uses H2 locally and a real-MySQL CI validation path, provides append-only audit events, SHA-256 hash-chain verification, filtered cursor pagination, soft archival, commitment-based redaction, independently verifiable exports, and a constrained client-account access report.

## Plan and rationale

The implementation was delivered in reviewable increments: establish the Spring core, add profile-based security and MySQL production settings, implement export, implement the Scenario C report, then harden retention, tail-deletion detection, schema migration, coverage, offline export verification, and production roles. Spring MVC and Spring Data JPA provide the HTTP and persistence boundaries. The local profile permits unauthenticated developer access; production uses environment-supplied HTTP Basic credentials with reader/writer/privacy/retention roles.

## Delivered artifacts

- API contract: `docs/openapi.yaml` and `docs/api-contract.md`.
- Implementation: `src/main/java/com/schwab/interview/audit/`.
- Tests and evidence: `src/test/java/com/schwab/interview/audit/` and `docs/testing.md`; run `mvn verify`.
- Setup and example request: `README.md`.
- AI traceability: `docs/ai-usage-log.md`.

## Assumptions and scope decisions

The server assigns the authoritative chain timestamp; callers may provide a separate observed event time, normalized to microsecond precision before hashing. Scenario C is intentionally limited to `CLIENT_DATA_READ` events on `CLIENT_ACCOUNT` resources. Exports include selected records, protected canonical representations, full-chain hash witnesses, and a database-independent verifier. A persisted HMAC-signed chain-head record detects direct deletion of the current tail. The production security profile is a configurable role-based HTTP Basic boundary rather than enterprise identity and authorization.

## Risks, trade-offs, and limitations

Verification and export scan the full chain, which is clear but not efficient at scale. The local chain-head is HMAC-SHA-256 signed using a production environment secret, which blocks a database-only attacker from forging the current checkpoint; it is not an externally immutable trust anchor. Production still needs least-privilege database accounts, immutable backups, monitoring, HMAC-key rotation, externally managed asymmetric export signatures, and an enterprise identity provider. Soft archival retains the primary row; redaction certificates remain lifecycle metadata rather than separately chained events. The repository now validates a basic MySQL path in CI, but that does not replace target-topology load and recovery testing.

## Validation evidence

On 2026-08-13, `mvn verify` completed successfully on Java 21.0.12 with 37 reported tests, zero failures/errors, one intentionally skipped MySQL-only test class, and 93.71% JaCoCo line coverage. Evidence covers direct content modification, tail deletion, lifecycle-evidence signing, timestamp normalization, HMAC checkpoint failures, redaction/archival, downloaded-bundle verification, Scenario C reporting, role authorization, and the non-web performance profile. A 1,000-event isolated H2 benchmark recorded 4.655 ms average append time and 92.822 ms full verification time.
