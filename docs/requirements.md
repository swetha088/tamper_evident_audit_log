# Requirements, Clarification, and Scope

## Normalized problem statement

Build a locally runnable audit-log API that accepts immutable event records, stores them in append order, and detects modification of stored history by linking each event cryptographically to its predecessor. The API must support filtered, paginated retrieval and a full-chain verification report. The prototype must also demonstrate a defensible approach to retention, payload redaction, verifiable exports, and compliance reporting for access to client account data.

## Scenario A: Core service acceptance criteria

1. A write endpoint accepts `eventType`, `actorId`, `resourceType`, `resourceId`, structured `payload`, and a timestamp.
2. The server assigns the canonical timestamp. A caller-supplied timestamp may be retained as metadata only if needed; this avoids clients backdating audit order. Persisted timestamps are normalized to UTC microsecond precision before chain hashing.
3. A successful write persists an immutable sequence number, a previous-record hash, and a SHA-256 content hash based on a canonical serialization of the protected record fields.
4. The public API exposes no update or delete operation for audit records.
5. A query endpoint supports any combination of actor ID, resource type and ID, event type, and inclusive time range, with deterministic cursor pagination.
6. `GET /audit/verify` checks every retained chain entry and reports either an intact chain or the first inconsistent sequence number and violation category.
7. Direct alteration of a stored record is detected by verification tests.

## Scenario B: extension acceptance criteria

1. A configurable retention policy archives eligible records without representing a legitimate archive boundary as tampering.
2. Sensitive payload values can be redacted without exposing their original cleartext and without breaking tamper evidence.
3. An export filtered by `resourceId` or `actorId` includes records plus sufficient membership proofs/chain metadata for independent verification of the exported records.

## Scenario C: clarified compliance-reporting requirement

### Source need

Regulators need to audit access to client account data.

### Ambiguities to resolve with product/compliance

- Which actions count as access: successful reads only, attempted reads, writes, exports, administration, or all of these?
- Which resources are client accounts, and what stable identifier may be disclosed in a report?
- What actor identity, authentication context, authorization decision, source system, and reason-for-access must be captured?
- What reporting period, format, delivery channel, retention period, and access controls apply?
- Which jurisdictions, regulatory rules, privacy classifications, and legal holds govern the data?
- Is audit-event ingestion required synchronously before access is granted, or is bounded asynchronous delivery acceptable?

### Working assumption for this prototype

The service records every attempted client-account data read with actor ID, client-account resource ID, UTC server timestamp, authorization outcome, request/correlation ID, source application, and optional reason code. Reports return `CLIENT_DATA_READ` records for a specified client account and time range. The local profile permits access for developer convenience; the production profile requires HTTP Basic authentication with environment-supplied credentials. Enterprise identity-provider integration, report delivery workflow, legal holds, and regulatory record-layout certification remain out of scope.

### Scope boundary

The implementation provides audit-data primitives, an API-level reporting/export path, and a minimal production authentication boundary. It does not claim to be a complete compliance reporting platform or enterprise authorization system.
