# Final Engineering Summary

## Outcome

Delivered a locally runnable Java 21 / Spring Boot 4.1.0 audit-log prototype. It uses H2 locally and MySQL configuration in the `prod` profile, provides append-only audit events, SHA-256 hash-chain verification, filtered cursor pagination, soft archival, commitment-based redaction, verifiable exports, and a constrained client-account access report.

## Plan and rationale

The implementation was delivered in reviewable increments: establish the Spring core, add profile-based security and MySQL production settings, implement export, implement the Scenario C report, then harden retention and tail-deletion detection. Spring MVC and Spring Data JPA provide the HTTP and persistence boundaries. The local profile permits unauthenticated developer access; production requires HTTP Basic credentials provided through environment variables.

## Delivered artifacts

- API contract: `docs/openapi.yaml` and `docs/api-contract.md`.
- Implementation: `src/main/java/com/schwab/interview/audit/`.
- Tests: `src/test/java/com/schwab/interview/audit/`; run `mvn test`.
- Setup and example request: `README.md`.
- AI traceability: `docs/ai-usage-log.md`.

## Assumptions and scope decisions

The server assigns the authoritative chain timestamp; callers may provide a separate observed event time. Scenario C is intentionally limited to `CLIENT_DATA_READ` events on `CLIENT_ACCOUNT` resources. Exports include selected records, their protected canonical representations, and full-chain hash witnesses. A persisted chain-head record detects direct deletion of the current tail. The production security profile is a minimal HTTP Basic boundary rather than enterprise identity and authorization.

## Risks, trade-offs, and limitations

Verification and export scan the full chain, which is clear but not efficient at scale. Hash chains and the local chain-head table are tamper-evident, not tamper-proof against a privileged attacker able to rewrite all data. Production needs explicit schema migrations, least-privilege database accounts, immutable backups, monitoring, and externally signed checkpoints. Soft archival retains the primary row; redaction certificates remain lifecycle metadata rather than separately chained events. H2 compatibility mode does not replace testing against MySQL.

## Validation evidence

On 2026-08-13, `mvn test` completed successfully on Java 21.0.12 with 12 H2-backed Spring integration tests. Coverage includes direct content modification, tail deletion, redaction/archival verification, export selector validation, Scenario C report filtering, local HTTP access, and production-profile HTTP Basic authentication.
