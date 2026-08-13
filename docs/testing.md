# Testing and Validation Evidence

## Run commands

```powershell
mvn test
mvn verify
```

`mvn test` automatically discovers all local success and exception scenarios. `AuditApplicationTestSuite` is the equivalent single IDE entry point. `mvn verify` additionally enforces formatting, creates a CycloneDX SBOM, produces `target/site/jacoco/index.html`, and fails if bundle line coverage falls below 85%.

## Current local evidence

On 2026-08-13, `mvn verify` reported 35 tests with zero failures/errors and one intentionally skipped MySQL-only test class. JaCoCo reported 570 covered and 28 missed lines: **95.32% line coverage**.

| Assignment scenario | Validated behavior |
| --- | --- |
| Scenario A | Append, every query filter, cursor pagination, server and caller timestamps, direct persisted-content tampering, tail deletion, missing/invalid HMAC checkpoint signatures, and full-chain verification. |
| Scenario B | Redaction, archival, exported-bundle selection, downloaded-JSON verification without repositories, changed export record detection, and broken witness detection. |
| Scenario C | `CLIENT_DATA_READ` filtering for a `CLIENT_ACCOUNT`, time-range validation, and invalid account input. |
| Security | Local open-development behavior; production authentication; read-only role can verify but receives `403` for writes. |
| Utilities | SHA-256/HMAC behavior and performance-probe enabled, disabled, invalid-count, and failed-verification paths. |

## MySQL validation

`MySqlAuditIntegrationTest` runs Flyway V1/V2 plus append/verify and invalid-range scenarios against a real MySQL database when `RUN_MYSQL_INTEGRATION=true`. GitHub Actions provisions MySQL 8.4 for this job on pushes and pull requests. It is deliberately skipped by ordinary local runs when disposable MySQL credentials are not supplied.

## Performance baseline

On 2026-08-13, the non-web `performance` profile ran against an isolated H2 in-memory database with Java 21.0.12. It appended 1,000 events in **4,654.801 ms** (4.655 ms/event) and verified all 1,000 records in **92.822 ms**. This is a repeatable single-process baseline, not a throughput, concurrency, or production-MySQL capacity claim.

## Remaining validation boundaries

- Confirm the first GitHub Actions MySQL job succeeds after the push and retain its artifact/URL as release evidence.
- Before a real deployment, run load, recovery, backup/restore, and key-rotation exercises against the target MySQL topology.
- Coverage is a regression control; it does not replace threat modeling, compliance certification, or an enterprise identity-provider assessment.
