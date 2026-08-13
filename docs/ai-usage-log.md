# AI Usage and Engineer Traceability

AI is used as an engineering accelerator. The engineer remains responsible for problem framing, design choices, security decisions, code review, testing, and every committed output. Do not include credentials, customer data, proprietary source code from another system, or other sensitive data in AI prompts.

## Log entries

| Date | Task and prompt intent | AI contribution | Engineer decision and rationale | Validation / approval |
| --- | --- | --- | --- | --- |
| 2026-08-13 | Establish initial assignment documentation and delivery plan. | Drafted documentation structure and initial wording. | Accepted after review; placeholders retained for personal attestation data and stack-specific facts. | Manual review pending before commit. |
| YYYY-MM-DD | [Task intent and constraints] | [Generated / analyzed / debugged / reviewed] | [Accepted, edited, or rejected, and why] | [Tests, reviewer, or approval evidence] |

## Prompting discipline

For substantive work, record enough context to explain the interaction later:

1. Task intent, technical context, constraints, and acceptance criteria.
2. The prompt or a faithful summary that excludes confidential data.
3. What output was used, changed, or rejected and why.
4. The validation performed and the human who approved high-impact changes.

## High-impact change checklist

- [ ] I reviewed cryptographic/canonicalization changes and their test vectors.
- [ ] I reviewed retention or redaction behavior for privacy and evidence implications.
- [ ] I reviewed authorization, secrets, dependencies, and database migrations.
- [ ] I ran the relevant automated checks and inspected their output.
