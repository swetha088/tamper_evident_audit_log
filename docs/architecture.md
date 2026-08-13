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

## Event record model (planned)

| Field | Purpose |
| --- | --- |
| `sequence` | Server-assigned monotonically increasing chain position. |
| `eventId` | Server-generated immutable event identifier. |
| `eventType`, `actorId`, `resourceType`, `resourceId` | Queryable audit identity fields. |
| `payload` | Event-specific structured details, classified for redaction. |
| `recordedAt` | Server-assigned UTC timestamp and audit ordering time. |
| `previousHash` | Hash of the preceding protected chain entry or a defined genesis hash. |
| `contentHash` | SHA-256 of canonical protected content, including `previousHash`. |
| `status` | Active or archived state; archive metadata is retained. |

## Hash-chain design principles

- Use SHA-256 with UTF-8 canonical serialization whose field order, timestamp format, null treatment, and number handling are explicitly specified and tested.
- The content hash includes the event identity, immutable event data, canonical timestamp, sequence, prior hash, and redaction commitment metadata.
- The genesis `previousHash` is a documented fixed 64-hex-character value (all zeroes unless the implementation records a versioned alternative).
- A database transaction or equivalent serialization mechanism must prevent concurrent writers from receiving the same predecessor.
- Verification checks sequence continuity, expected previous hash, recomputed content hash, and valid archive/redaction metadata; it stops and reports the first failure.

## Retention and redaction direction

Physical deletion of an ordinary chained row would sever the chain. The preferred design is to retain a compact, immutable archive/tombstone commitment in the primary chain: original sequence, original hash, archive timestamp/policy/version, and an authenticated reference to the archived record. Verification treats this known record type as valid rather than skipping it.

For redaction, store sensitive cleartext separately from the chain-protected audit record and place a salted cryptographic commitment to each protected sensitive value in the record. Redaction destroys or replaces only the separately stored value while retaining the commitment and an immutable redaction certificate (field path, time, authority, and reason) in the protected chain representation. This proves the history changed only through the defined redaction mechanism, but does not make the original value recoverable.

This design will be finalized against the selected data model before code is written; redaction is high-impact and requires explicit engineer sign-off.

## Trade-offs

- A linear full-chain scan is simple and highly explainable, but verification cost grows with history size. Checkpoints or Merkle structures can be considered after correctness is established.
- Server-assigned timestamps protect audit ordering but do not represent client-observed event time; optional client time must be separately labeled.
- Hash chains provide tamper evidence, not prevention. Database access control, backups, append-only storage, monitoring, and external anchoring remain production requirements.
