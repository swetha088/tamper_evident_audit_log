# Final Engineering Summary

## Outcome

Delivered a locally runnable Java 21 / Spring Boot 4.1.0 prototype with an H2-backed append-only audit API and a PostgreSQL production profile. It implements SHA-256 chaining, filtered cursor-paginated queries, full verification, soft archival, commitment-based payload redaction, self-contained chain-witness exports, and a constrained client-account access report.

## Plan and rationale

Work proceeded through the milestones in `docs/milestones.md`: establish requirements/design evidence; implement the core append/query/verify path; add retention/redaction/export; implement the deliberately narrow compliance report; then test and document the result. Spring MVC and Spring Data JPA provide the HTTP and persistence boundaries. H2 is used locally in PostgreSQL compatibility mode, while the `prod` profile requires PostgreSQL connection settings.

## Delivered artifacts

- API contract: `docs/openapi.yaml` and `docs/api-contract.md`.
- Implementation: `src/main/java/com/schwab/interview/audit/`.
- Tests: `src/test/java/com/schwab/interview/audit/AuditServiceTest.java`; run `mvn test`.
- Setup and example request: `README.md`.
- AI traceability: `docs/ai-usage-log.md`.

## Assumptions and scope decisions

The server assigns the authoritative chain timestamp; callers may provide a separate observed event time. Scenario C is limited to audit records for `CLIENT_ACCOUNT` resources and assumes upstream systems emit access events. The prototype does not implement user authentication or authorization, report delivery, legal holds, or regulatory-specific record formats.

## Risks, trade-offs, and limitations

Verification and export both scan the full chain, which is intentionally clear but not efficient at scale. A hash chain detects tampering but cannot prevent privileged attackers from rewriting every row and hash; production needs database access controls, immutable backups, monitoring, and external signed checkpoints. Soft archival retains the full primary record, and redaction certificates are lifecycle metadata rather than separately chained events. H2 compatibility mode reduces but does not eliminate PostgreSQL differences. See `docs/validation-and-risks.md` for the full risk register.

## Validation evidence

On 2026-08-13, `mvn test` completed successfully on Java 21.0.12 with 2 H2-backed Spring integration tests. The test suite directly changes an `actorId` persisted by JPA and confirms verification reports `CONTENT_HASH_MISMATCH`; it also verifies that redaction and archival preserve chain validity.
