# Validation, Security, and Risks

## Quality gates

| Gate | Evidence required before handoff |
| --- | --- |
| Compilation and tests | `mvn verify` passes with zero failures/errors; the MySQL-only class is explicitly skipped unless enabled. |
| Unit tests | Validation, canonicalization, hash calculation, pagination, archive/redaction semantics, export verification, and failure classification. |
| Integration tests | Write, query, verify, retention/redaction, export, compliance reporting, role authorization, and a MySQL 8.4 CI job. |
| Tampering tests | Directly modify protected columns in the store; verification identifies the first inconsistency. |
| Security review | Input validation, injection resistance, secret handling, error disclosure, authorization boundary, and dependency review. |
| Performance check | A documented 1,000-event H2 append/full-verification baseline; target-topology load testing remains required. |
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
| APIs expose records to unauthorized callers | Local access is deliberately open; production validates credentials and applies separate reader/writer/privacy/retention roles. Replace HTTP Basic with enterprise identity/authorization before real-data use. |
| H2 differs from MySQL | H2 runs in MySQL compatibility mode locally; a MySQL 8.4 CI job validates Flyway plus append/verify. Validate target-topology transaction behavior, recovery, and performance before production release. |
| A privileged application host can access the checkpoint HMAC key | Keep `AUDIT_INTEGRITY_CHECKPOINT_KEY` in a production secret manager, rotate it with a checkpoint migration plan, and add an external immutable checkpoint service for stronger non-repudiation. |
| Performance behavior is inferred from a developer machine | The documented 1,000-event H2 baseline is reproducible but single-process only. Establish latency, throughput, and concurrency targets and run a production-MySQL load test before release. |

## Non-goals for the prototype

- A complete identity and access-management solution.
- Legal or regulatory certification.
- Multi-region replication, immutable object-store integration, key-management service integration, or external notarization.
- Guaranteed exactly-once ingestion across external producers.

## Current validation evidence

On 2026-08-13, `mvn verify` passed 37 reported Java 21 tests with zero failures/errors, one intentionally skipped MySQL-only class, and 93.71% JaCoCo line coverage. Coverage includes direct persisted modification, tail deletion, lifecycle-evidence signing, redaction/archival, downloaded-bundle verification, compliance reporting, local HTTP access, production authentication/authorization, timestamp precision, and the non-web performance profile. Re-run `mvn verify` before each commit or handoff.
