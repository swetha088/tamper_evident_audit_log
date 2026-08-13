# AI Usage and Engineer Traceability

## Purpose and ownership

This log records how AI assisted this repository. It is evidence of the development process, not a substitute for engineering judgment. The candidate must review this record, correct anything that is inaccurate, and approve every output before committing it.

The candidate owns the requirements interpretation, design decisions, security posture, source-code review, test execution, and final submission. AI was not given credentials, customer information, production data, proprietary source code from another system, or the candidate's attestation details.

## Interaction record

| ID | Date | Task context, constraints, and acceptance criteria | AI input / prompt intent | Output and disposition | Engineer decision required | Validation evidence |
| --- | --- | --- | --- | --- | --- | --- |
| AI-001 | 2026-08-13 | Turn the assignment into a reviewable engineering baseline before implementation. Required artifacts: requirements clarification, decomposition, architecture, risks, API contract, milestones, final summary, and attestation template. Do not invent personal attestation data. | Summarized the assignment and asked for an initial repository documentation set. | **Generated and edited:** `README.md` plus the documents under `docs/`. **Preserved as placeholders:** full name, email, and submission date in `ATTESTATION.md`. | Confirm the normalized requirements, Scenario C assumptions, and personal attestation values. Reject or revise any assumption that does not match the intended solution. | Repository inspection; `git diff --check` passed at the time of creation. |
| AI-002 | 2026-08-13 | Define a hash-chain design that supports append-only records, tampering detection, future archival, and privacy redaction. Constraints: deterministic canonical serialization, server-authoritative ordering time, no update/delete API, and explainable failure types. | Asked for an implementation/design proposal for the audit service. | **Generated and edited:** SHA-256 chain design; a fixed genesis hash; `recordedAt` versus optional `occurredAt`; protected sensitive-field commitments; soft-archive lifecycle approach; and explicit limitations in `docs/architecture.md`. | High-impact sign-off: independently review canonicalization rules, hash input fields, genesis value, concurrency behavior, and redaction semantics before accepting this design. | Design review against `docs/requirements.md` and `docs/architecture.md`; automated tampering/redaction checks added later in AI-005. |
| AI-003 | 2026-08-13 | Create an initial runnable local prototype and tests for write, query, verification, retention/redaction, export, and compliance reporting. | Asked to proceed with application development after milestones were created. | **Generated:** an initial dependency-free prototype. **Rejected/replaced:** the Node implementation was removed when the requested technology stack changed to Java 21/Maven/Spring Boot. Its initial test results are historical only and are not used as evidence for the current implementation. | Confirm that replacing, rather than maintaining, the previous runtime is the desired repository history. No Node source remains in the working application. | Historical prototype tests passed before migration; superseded by the Java test evidence below. |
| AI-004 | 2026-08-13 | Migrate to the requested Java 21, Maven, Spring Boot 4.1.0, H2 local, PostgreSQL production stack. Preserve the API intent and local tampering demonstration. | Asked to replace the implementation with the specified Java/Spring stack. | **Generated and edited:** `pom.xml`; Spring MVC controller; JPA entity/repository/service; H2 default and PostgreSQL `prod` profile; integration tests; and Java-specific setup documentation. **Research-assisted correction:** Spring Boot 4 defaults to Jackson 3 (`tools.jackson.*`), so obsolete Jackson 2 imports were replaced. | Review dependency choices, Spring profile configuration, and the production database credential handoff. In particular, production schema migrations and secrets-manager integration remain required work. | Java 21.0.12 and Maven 3.9.16 available; `mvn test` completed successfully after compilation fixes. |
| AI-005 | 2026-08-13 | Debug a failing integrity test without weakening valid tamper evidence. The observed failure was `SEQUENCE_GAP` after a test transaction rollback. | Provided the failing Maven test output and asked for diagnosis/fix. | **Generated diagnosis; accepted after code change:** database identity/sequence values can legitimately skip after rolled-back transactions. The verifier no longer interprets physical primary-key gaps as tampering; it verifies hash linkage and recomputed content hashes instead. | High-impact sign-off: decide whether a separately assigned, gap-free chain position is required for the target design. Current behavior is documented as a deliberate trade-off. | `mvn test` passed: direct persisted `actorId` modification reports `CONTENT_HASH_MISMATCH`; redaction and archival preserve verification. |
| AI-006 | 2026-08-13 | Prepare reviewable final documentation without claiming unperformed work. | Asked to update documentation following the Java migration and validation. | **Generated and edited:** Java/H2/PostgreSQL setup instructions, OpenAPI server URL, risks, AI traceability, and final-summary evidence. **Not claimed:** PostgreSQL integration test, production migration, load test, authentication/authorization, external checkpointing, or manual candidate review. | Read every final artifact and replace any remaining draft language. Complete the attestation and this log's approval section before commit. | `mvn test` passed; no commit was created by AI. |
| AI-007 | 2026-08-13 | Establish the existing Spring Boot implementation as a reviewable baseline before delivering the remaining features in separate commits. Preserve assignment traceability; do not commit the confidential assignment PDF. | Asked to inspect outstanding assignment requirements, then deliver missing work incrementally with a matching attestation and AI-log update per push. | **Accepted:** staged the current Spring MVC/JPA implementation as a standalone baseline commit and corrected the README status. **Deferred:** export, compliance reporting, MySQL production configuration, Spring Security, retention hardening, and broader test coverage. | Review the baseline implementation and each follow-on commit before final submission. | Maven is not installed in the current environment, so this exact baseline could not be rerun here; earlier recorded test results are historical evidence only. |

## Rejected or changed AI output

| Item | Why it changed | Final treatment |
| --- | --- | --- |
| Node.js/SQLite implementation | The candidate selected Java 21, Maven, Spring Boot 4.1.0, H2, and PostgreSQL instead. | Removed from the executable application; replaced by Spring MVC/JPA code. |
| Jackson 2 imports | Spring Boot 4.1.0 uses Jackson 3 by default, so the original imports did not compile. | Replaced with `tools.jackson.*` imports and validated with Maven. |
| Primary-key gap as a chain violation | Rollbacks can consume database identity values without committing an event, producing legitimate gaps. | Removed as a verifier failure condition; hash linkage remains the integrity check. |
| Auto-filled attestation identity | AI does not know the candidate's true name, email, or submission date. | Explicit placeholders remain for the candidate to complete. |

## Secure AI usage guardrails

- Do not prompt AI with secrets, database credentials, regulated customer data, access tokens, or proprietary source from unrelated systems.
- Treat all generated code as untrusted until reviewed, tested, and security-checked by the candidate.
- Keep prompts focused on a defined task with constraints and acceptance criteria.
- Record high-impact output changes, including rejected suggestions and the reason for rejection.
- Require explicit candidate approval for cryptography, retention/redaction, authorization, dependency, database, and production-configuration changes.

## Candidate approval before submission

Complete this section only after your personal review. Do not check an item merely because AI generated it.

- [x] I reviewed the requirement assumptions and scope boundaries.
- [x] I reviewed the hash-input/canonicalization design and tampering tests.
- [x] I reviewed retention/redaction privacy implications and limitations.
- [x] I reviewed dependencies, database profiles, and secret-handling approach.
- [x] I ran `mvn test` locally and inspected the result.
- [x] I verified the documentation accurately represents my own process and AI usage.
- [x] I completed `ATTESTATION.md` with my own information.
