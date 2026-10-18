# Risk register — M3 continuation (original RISK-001..RISK-008 inherited)

Prior risk IDs RISK-001 to RISK-008, team probability/impact 1–5 scoring and owner names remain in **historical PED v2.0 Sec 8 / Part B updates**. Do not overwrite historical review or falsely mark earlier risks closed. Below are new/changed M3 statuses and actions; team should reconcile them with the existing live register.

| ID | Description, cause, effect | Probability × impact | Mitigation / fallback | Owner | Status |
|---|---|---|---|---|---|
| RISK-001 | M1/M2 stakeholder/criteria misunderstandings persist | 3×4=12 High (working re-estimate) | Confirm lifecycle/catalogue/overdue; update RTM and ADR | Team stakeholder liaison | Open |
| RISK-005 | Java 25 / Spring Boot 4.1.1 / PostgreSQL 18.6 environment incompatibility | 3×4=12 High (working re-estimate) | GitHub clean runner + Compose staging; revert experimental change if proof fails | Backend owner | Open |
| RISK-007 | Two-reviewer PR/branch control cannot be shown until GitHub complete | 4×4=16 Critical (working re-estimate) | Protect main and gather two substantive non-author reviews per PR | Team lead | Open; GitHub deferred |
| RISK-008 | AI-generated code/document inaccuracies may be accepted | 4×4=16 Critical (working re-estimate) | Each member review, tests, manual validation and AI register; reject unsupported claims | All team members | Open |
| RISK-009 | Dev/staging-only HTTP Basic on loopback is not production authentication | 4×5=20 Critical for public deployment | Isolate loopback, generated credentials; replace with approved identity/auth and TLS before any public exposure | Security owner | **Production blocker** |
| RISK-010 | A-005 final categories, transition rules and overdue policy unapproved | 4×4=16 Critical | Stakeholder sign-off and tests for final rules; do not claim overdue readiness | Product/requirements owner | **Acceptance blocker** |
| RISK-011 | CI, integration and load tests not executed here | 4×4=16 Critical | Run PR/push checks; retain artifacts; evaluate measured load before declaring READY | QA owner | **M3 evidence blocker** |
| RISK-012 | One-machine Compose environment cannot simulate production scale, backup, real auth or mail/SMS | 3×4=12 High | Bound conclusions, exercise recovery, future prod-like tests | Deployment owner | Residual |

*Working risk scores are suggestions pending team re-evaluation; do not present them as earlier approved M1/M2 scores.*
