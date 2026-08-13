# Architecture and Design Decisions

## Intended components

```text
HTTP API -> request validation -> audit service -> repository -> persistent store
                                  |                 |
                                  |                 +-> append-only event rows
                                  +-> canonical hash-chain builder

Verification API -> verifier -> ordered event rows -> integrity report
Export API       -> export builder -> records + chain metadata/proofs
```

## Event record model

| Field | Purpose |
| --- | --- |
| `sequence` | Server-assigned monotonically increasing chain position. |
| `eventId` | Server-generated immutable event identifier. |
| `eventType`, `actorId`, `resourceType`, `resourceId` | Queryable audit identity fields. |
| `payload` | Event-specific structured details, classified for redaction. |
| `recordedAt` | Server-assigned UTC timestamp and audit ordering time. |
| `previousHash` | Hash of the preceding protected chain entry or a defined genesis hash. |
| `contentHash` | SHA-256 of canonical protected content, including `previousHash`. |
| `status` | Active, redacted, or archived lifecycle state; lifecycle metadata is retained. |

## Hash-chain design principles

- Use SHA-256 with UTF-8 canonical serialization whose field order, timestamp format, null treatment, and number handling are explicitly specified and tested.
- The content hash includes the event identity, immutable event data, canonical timestamp, sequence, prior hash, and redaction commitment metadata.
- The genesis `previousHash` is a documented fixed 64-hex-character value (all zeroes unless the implementation records a versioned alternative).
- A database transaction or equivalent serialization mechanism must prevent concurrent writers from receiving the same predecessor.
- Verification checks ordered hash linkage and recomputed content hashes; it stops and reports the first failure. Database primary-key values may have ordinary gaps after rolled-back transactions, so the service does not treat such gaps as integrity failures.
- A separately persisted, transactionally updated chain-head record stores the latest sequence, event ID, and content hash. Verification compares the scanned tail to this state, detecting a direct deletion of the latest row that a linear hash chain alone would otherwise not reveal. Its current value is HMAC-SHA-256 signed with a key stored outside the database (`AUDIT_INTEGRITY_CHECKPOINT_KEY` in production), so a database-only attacker cannot forge a valid current checkpoint. This is still not an externally immutable trust anchor.

## Retention and redaction direction

Physical deletion of an ordinary chained row would sever the chain. This prototype therefore uses soft archival: the complete chained row stays in the primary store and only its lifecycle metadata changes to `ARCHIVED`, with the archive timestamp and policy retained. The default archival window is configurable as `audit.retention.default-window` (`P90D` locally), while an explicit cutoff may be supplied to the prototype endpoint. The hash covers immutable event content and is deliberately independent of this lifecycle metadata, so a legitimate archive transition cannot cause a false verification failure. A production archive-to-cold-storage workflow would retain an immutable archive/tombstone commitment and authenticated external reference.

For redaction, this prototype hashes a salted commitment for each declared sensitive payload path into the chain-protected representation. The live payload initially retains the value. A redaction replaces that value with `[REDACTED]` and records a certificate (field path, time, authority, and reason); verification then uses the retained commitment rather than the discarded cleartext. The certificate is lifecycle metadata rather than chain content in this prototype, a limitation that must be addressed with a separately chained redaction event or signature in production. This proves the core history remains intact, but does not make the original value recoverable.

This design will be finalized against the selected data model before code is written; redaction is high-impact and requires explicit engineer sign-off.

## Trade-offs

- A linear full-chain scan is simple and highly explainable, but verification cost grows with history size. Checkpoints or Merkle structures can be considered after correctness is established.
- Server-assigned timestamps protect audit ordering but do not represent client-observed event time; optional client time must be separately labeled.
- Hash chains provide tamper evidence, not prevention. Database access control, backups, append-only storage, monitoring, and external anchoring remain production requirements.
