# Tamper-Evident Audit Log Service

A production-minded prototype of an append-only audit log service. The service will record security-relevant events, make direct data-store tampering detectable with a cryptographic hash chain, and provide query, verification, retention/redaction, and verifiable-export capabilities.

> Status: documentation and design baseline. The runnable implementation is intentionally not yet present.

## Goals

- Provide an append-only write API for audit events.
- Query events by actor, resource, event type, time range, and pagination.
- Detect altered historical records through a SHA-256 hash chain.
- Support retention, privacy-preserving redaction, and independently verifiable exports.
- Demonstrate a clarified compliance-reporting use case for access to client account data.

## Documentation

- [Requirements and scope](docs/requirements.md)
- [Execution plan](docs/execution-plan.md)
- [Architecture and design decisions](docs/architecture.md)
- [Draft API contract](docs/api-contract.md)
- [Validation, security, and risks](docs/validation-and-risks.md)
- [AI usage traceability](docs/ai-usage-log.md)
- [Final engineering summary](docs/final-engineering-summary.md)

## Planned local setup

Implementation language, framework, persistence choice, and exact commands will be recorded here once selected. The completed project will include one-command local startup, API examples, and commands for all quality gates.

## Repository conventions

- Commits should be small and describe the engineering decision or verified change.
- AI-assisted work is recorded in `docs/ai-usage-log.md`; the engineer reviews and owns every accepted change.
- High-impact changes (hash construction, chain verification, retention/redaction semantics, and security controls) require explicit human review before merge.
