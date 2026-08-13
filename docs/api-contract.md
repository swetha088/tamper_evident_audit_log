# Draft API Contract

This contract describes the implemented prototype. The executable OpenAPI definition is `docs/openapi.yaml`.

## `POST /audit/events`

Appends one audit event. The service, rather than the caller, assigns the authoritative `recordedAt` timestamp, sequence, event ID, and hashes.

Request fields:

| Field | Required | Notes |
| --- | --- | --- |
| `eventType` | Yes | A validated, documented event category, such as `USER_LOGIN`. |
| `actorId` | Yes | Stable identifier of the principal or system causing the action. |
| `resourceType` | Yes | A resource classification, such as `CLIENT_ACCOUNT`. |
| `resourceId` | Yes | Stable identifier of the affected resource. |
| `payload` | Yes | Structured event detail. Sensitive paths are classified before persistence. |
| `occurredAt` | No | Caller-observed UTC event time; not the chain ordering timestamp. Stored values are normalized to UTC microsecond precision before hashing. |

Response: `201 Created` with the immutable stored event including `eventId`, `sequence`, `recordedAt`, `previousHash`, and `contentHash`.

## `GET /audit/events`

Returns a deterministically ordered page of events. All supplied filters combine with logical AND.

Query parameters: `actorId`, `resourceType`, `resourceId`, `eventType`, `from`, `to`, `cursor`, and `limit`.

Response: `200 OK` containing `items`, `nextCursor` (or `null`), and page metadata. The final contract will specify maximum page size and invalid-cursor behavior.

## `GET /audit/verify`

Walks the complete retained chain in sequence order.

Response: `200 OK` with:

```json
{
  "intact": true,
  "recordsChecked": 125,
  "firstViolation": null
}
```

When broken, `firstViolation` includes at least the sequence/event identifier and a machine-readable violation type, such as `PREVIOUS_HASH_MISMATCH`, `CONTENT_HASH_MISMATCH`, `SENSITIVE_VALUE_COMMITMENT_MISMATCH`, `CHAIN_HEAD_MISMATCH`, or `CHAIN_HEAD_SIGNATURE_MISMATCH`.

## `GET /audit/export`

Exports a self-contained, verifiable bundle. Exactly one of `actorId` or `resourceId` is required. An optional time range may narrow results.

The bundle includes selected records, their sequence/hash fields, a canonical protected payload representation, the commitment material required to derive that representation from the displayed payload, chain context sufficient to establish their original positions, export format version, and the verification algorithm/canonicalization version.

The prototype includes `chainWitnesses` for every row in the chain and canonical protected representations for selected records. This favors independent verification and clarity over export size; it is not an efficient production design for long histories.

## `POST /audit/export/verify`

Validates a previously downloaded export bundle without reading any audit repository data. It checks the declared export/hash versions, ordered full-chain witnesses, selected-record membership in that witness set, commitment-backed equivalence of the displayed and protected payload representations, and recomputed SHA-256 content hashes. An independent recipient can use `AuditExportVerifier` in a separate Java process or submit the bundle to this endpoint on an isolated verifier instance. The bundle is tamper-evident; a production non-repudiation workflow would additionally sign the export with an externally managed asymmetric key.

## `GET /audit/reports/client-account-access`

Returns only `CLIENT_DATA_READ` events whose `resourceType` is `CLIENT_ACCOUNT` and whose `resourceId` equals the required `clientAccountId`. Optional `from` and `to` filters apply to server-assigned `recordedAt` values. The response includes the matching events and a full-chain verification result. This endpoint implements the stated prototype assumption; it does not infer access from unrelated event types or replace regulatory reporting workflow controls.

## `POST /audit/events/{eventId}/redactions`

Redacts one or more previously declared sensitive payload paths. Request fields are `paths`, `actorId`, and `reason`. The path must have been declared in `sensitivePaths` at append time. The endpoint replaces the cleartext with `[REDACTED]` while retaining the chain-protected commitment.

## `POST /audit/retention/run`

Runs the prototype soft-archival policy. Request fields are `cutoff` (UTC ISO-8601 timestamp) and optional `policy`. It marks eligible active rows `ARCHIVED` while retaining their full rows and hashes.

## Error model and controls

The final API will use a consistent error body with a stable code, human-readable message, and correlation ID. It will reject invalid fields, malformed timestamps/cursors, unsupported filter combinations, and unauthorized access without returning protected payload data.
