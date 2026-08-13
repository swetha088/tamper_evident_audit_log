# Validation, Security, and Risks

## Quality gates

| Gate | Evidence required before handoff |
| --- | --- |
| Compilation and tests | Maven compilation and all H2-backed integration tests pass. |
| Unit tests | Validation, canonicalization, hash calculation, pagination, archive/redaction semantics, and failure classification. |
| Integration tests | Write, query, verify, retention/redaction, export, and compliance-reporting paths against a real test store. |
| Tampering tests | Directly modify protected columns in the store; verification identifies the first inconsistency. |
| Security review | Input validation, injection resistance, secret handling, error disclosure, authorization boundary, and dependency review. |
| Performance check | Measure append, filtered query, and full verification on a documented representative dataset. |
| Documentation review | Setup works from a clean clone; all assumptions and limitations remain current. |

## Primary risks and controls

| Risk | Control / limitation |
| --- | --- |
| Canonicalization differs across code paths | One versioned serializer, golden-vector tests, and no ad-hoc JSON serialization for hashes. |
| Concurrent appends fork the chain | Transactional sequencing and a uniqueness constraint on sequence/predecessor as appropriate to the store. |
| Privileged store access rewrites all hashes | Hash chains detect unsophisticated tampering only; use least privilege, immutable backups, monitoring, and external signed checkpoints in production. |
| Redaction destroys evidence or leaks sensitive data | Separate sensitive storage, commitments, authorized workflow, and tests proving no cleartext remains in normal responses/logs. |
| Archive process causes false integrity failures | Preserve an authenticated archive boundary/tombstone and test it end to end. |
| Large histories make verification slow | Start with full verification for clarity; document limits and consider checkpoints/async verification. |
| APIs expose records to unauthorized callers | Local access is deliberately open; production requires HTTP Basic credentials supplied through the environment. Replace the single in-memory principal with enterprise identity/authorization before real-data use. |
| H2 differs from MySQL | H2 runs in MySQL compatibility mode locally; validate schema, transaction behavior, and performance against MySQL before production release. |

## Non-goals for the prototype

- A complete identity and access-management solution.
- Legal or regulatory certification.
- Multi-region replication, immutable object-store integration, key-management service integration, or external notarization.
- Guaranteed exactly-once ingestion across external producers.

## Current validation evidence

On 2026-08-13, `mvn test` passed 12 Java 21 H2-backed integration tests covering direct JPA/database modification, tail deletion, redaction/archival, exports, compliance reporting, local HTTP access, and production-profile authentication. Re-run `mvn test` before each commit or handoff.
