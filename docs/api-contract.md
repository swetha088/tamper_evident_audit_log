# Draft API Contract

This is an implementation-neutral contract baseline. The final implementation will publish an executable OpenAPI specification with exact schemas, status codes, and examples.

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
| `occurredAt` | No | Caller-observed UTC event time; not the chain ordering timestamp. |

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

When broken, `firstViolation` includes at least the sequence/event identifier and a machine-readable violation type, such as `SEQUENCE_GAP`, `PREVIOUS_HASH_MISMATCH`, `CONTENT_HASH_MISMATCH`, or `INVALID_ARCHIVE_BOUNDARY`.

## `GET /audit/export`

Exports a self-contained, verifiable bundle. Exactly one of `actorId` or `resourceId` is required. An optional time range may narrow results.

The bundle includes selected records, their sequence/hash fields, chain context sufficient to establish their original positions, export format version, and the verification algorithm/canonicalization version. The final design will specify whether it emits a contiguous chain segment, signed checkpoints, or inclusion proofs for non-contiguous filtered results.

## Error model and controls

The final API will use a consistent error body with a stable code, human-readable message, and correlation ID. It will reject invalid fields, malformed timestamps/cursors, unsupported filter combinations, and unauthorized access without returning protected payload data.
