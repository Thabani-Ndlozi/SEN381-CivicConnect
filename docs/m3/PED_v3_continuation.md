# Part D — Milestone 3: Controlled Construction, Integration, Quality & Release Readiness

## D0. Document continuity and submission baseline

Version 3.0 continues the approved CivicConnect Project Engineering Document (PED) v2.0. The original 31-page PED v2.0 has been preserved intact in the middle of this expanded PDF, retaining its M1/M2 requirements, engineering decisions, risk history, RTM and architecture rationale. New Part D records M3 design/implementation and points to authentic local source and future CI evidence. The M3 source snapshot is not an authenticated GitHub release and none of the future GitHub actions have been performed.

Team: Thaban Ndlozi (602341); Anele Nkayi (577168); Lebogang Masia (601562). Version history: v1.0 dated 9 Sep 2026; v2.0 dated 26 Sep 2026; current continued v3.0 drafted 12 Oct 2026 and pending team sign-off.

Submission block (fill only after repository governance and review): existing GitHub repository URL = PENDING; assessed M3 release tag/SHA = PENDING (temporary local label m3-rc1-pending-git-tag is not a Git tag); live RTM link = PENDING reconciliation, working continued copy at docs/registers/RTM_CONTINUED.csv; CI/test evidence = .github/workflows/ci.yml and evidence/ with actual run URL PENDING; simulated staging = http://localhost:8081 on the demonstrator machine after Docker startup; API health/version at http://localhost:8082/actuator/health and /api/release; submitted date = PENDING.

## D1. Milestone 3 overview and integrated product scope

The original M2 modular monolith, React/TypeScript/Vite client, Spring Boot 4.1.1 / Java 25 server, Flyway PostgreSQL 18.6 persistence and three roles are retained. A requester may submit a categorised validated request, view the request list and an in-app lifecycle timeline. Staff may see all requests, accept a submitted unowned request, and change a request only when authorised as current owner; changes carry optimistic versions and comments. Management may inspect request status totals, category/status breakdowns and accountable actor-aware histories. Browser searches, filters and sorting are provided for visible requests. Refer to README.md, backend/src, frontend/src, docs/m3/m3-baseline-and-scope.md and docs/api.md.

The submitted source is only a candidate for integrated execution: this workspace lacks Java 25/Maven, Docker and dependency downloads, so no full-stack success is asserted. A-005 (approved category/status/overdue business rules), NFR-001 human usability confirmation and GitHub governance remain open. The defined overdue classification is not fabricated.

## D2. M2 baseline conformance and controlled evolution

Conformance: modular-monolith modules for requests, audit, reporting, security and frontend still correspond to ADR-M2-ARCH-001. PostgreSQL and Flyway V1 remain the persistence baseline; atomic status/audit writes and after-commit feedback event retain ADR-M2-DES-001. Java 25, Spring Boot 4.1.1, React 19, TypeScript/Vite, Node 24, PostgreSQL 18.6 and container-ready delivery follow ADR-M2-TECH-001, ADR-M2-DEP-001 and ADR-M2-CI-001.

ADR-M3-001 repairs missing version in the M2 prototype status command, aligning it with ADR-M2-DES-002 and returning 409 on stale updates. ADR-M3-002 exposes constrained requester feedback and management actor audit without adding unauthorised data access or a third-party messaging dependency. ADR-M3-003 selects a separated loopback-only Compose simulated staging configuration, not an unapproved cloud provider. Refer to docs/registers/ADR_Change_Log_M3.md for original decision, observed gap, revised decision, consequences and trace.

## D3. Construction, collaboration and secure configuration

`backend/` retains the same Spring domain/application/web/persistence modules; `frontend/` extends the submitted role UI; PostgreSQL remains schema-controlled by Flyway V1. `ChangeStatusCommand` includes a mandatory version. `RequestService` enforces ownership and allowed transition, requires a reason on rejection/resolution/closure, persists actions and audits together, and maps history to role-filtered responses. The controller layer provides requester history, staff details, manager activity and grouped status counts.

The working GitHub Actions definition at .github/workflows/ci.yml uses PR/push quality gates. GitHub main branch protection, actual PR history, issues, two substantive non-author peer approvals and a real CI run have not yet been performed, by team request. `GITHUB_AFTER_READY.md` gives procedures that preserve the existing repository history and do not manufacture evidence. Credentials are injected via environment. Staging .env.staging is generated locally and git-ignored. Dev/staging Basic authentication is strictly nonproduction; public deployment is blocked pending approved identity and HTTPS controls.

## D4. CI, automated verification and static/security controls

The prepared CI pipeline runs Maven verify/JUnit, npm test and TypeScript/Vite compilation, Python limited static/source scanning, TruffleHog verified secret scanning, npm production-dependency audit, a disposable Docker Compose simulated staging stack, live system/API tests, a modest management load exercise and JSON/report artifact upload. Mandatory build/test/check failures block progression. Backend JUnit Surefire XML and JSON staging outputs must be linked from real CI runs; no run links yet exist.

At least five unit/component cases appear in ServiceRequestRulesTest, RequestServiceM3Test and TransitionDecisionTableTest; at least five black-box equivalents/boundaries in CreateRequestValidationTest; at least six planned live API cases and two cross-module E2E/system journeys in scripts/smoke.py. See docs/m3/test-plan-and-case-matrix.md for exact input, oracle, technique, risk, test ID and evidence location. Risk priority is identity/ownership integrity, status/audit consistency and requester privacy. The lightweight local Python syntax/secret-pattern check did pass; see evidence/local-static-check.txt. Do not conflate this with a successful compile or integration run.

## D5. Black-box, API/integration, E2E and acceptance

Black-box input design uses equivalence partitioning (valid/nonvalid title/category), boundary analysis (title lengths 1/160/161 and description 4000/4001), a decision/state table for allowed/forbidden status transitions, and negative auth and stale version behaviour. These techniques were selected because they map to real validation, privacy and lifecycle risks. API01-API06 exercise real Spring Security and database-bound HTTP once staged, including expected 401, 403, 400 and 409. E2E01 spans create->list->accept->in progress->resolved->closed->requester audit; E2E02 spans requester create->staff action->manager audit/counts/breakdown. They must be executed on the identified actual staged app before acceptance is claimed. Manual browser role journeys are required to assess NFR-001; the API-only automation cannot establish accessibility or ease of use.

## D6. Performance, reliability and staging

PERF-01 targets GET /api/management/summary: as activity grows it repeatedly aggregates persistent request counts and may influence dashboard responsiveness. The proposed workload is 40 authenticated requests with 4 concurrent workers to a dedicated local staging API, collecting p50/p95 response time, error count, throughput and environment/time. A preliminary internal smoke hypothesis is zero errors and p95 below 1 second in that small environment; no stakeholder performance SLO has been approved and **no actual latency/throughput has been measured yet**. See scripts/performance.py, evidence/performance-results.json (generated only after running), and docs/m3/staging-deployment-and-recovery.md.

Staging is a separate Spring profile, database volume and generated credential set. The API release/version endpoint and exact final SHA/tag form the release identity check. A human can demonstrate on http://localhost:8081 (local computer only). Backup/rollback requires a prior verified image/tag, compatible schema and a tested PostgreSQL restore; a recovery drill was not done. The simulation does not prove production-scale use, uptime, third-party availability or secure public access.

## D7. Defects, quality evidence and traceability

Continue the original FR-001..010 and NFR-001..004 RTM history with M2 component/ADR trace and M3 acceptance oracles, test IDs, evidence status, defects and residual risks. The supplied ZIP did not contain the actual live editable team RTM, so docs/registers/RTM_CONTINUED.csv is an honest PDF-derived continuation that the team must reconcile with the live original in GitHub; do not overwrite authentic history. Full original acceptance wording is in preserved historical PED v2 Sec 6.

Fixes present in candidate source: DEF-001 inadvertent ownership takeover, DEF-002 missing optimistic request version, DEF-003 missing requester/manager history presentation, DEF-006 missing resolution reasons. They are not marked verified or closed until actual regression evidence exists. DEF-004 (overdue rule) and DEF-005 (human UI acceptance) are open. A passing Java test is narrower than DB transaction proof; a green CI is narrower than business acceptance. See docs/registers/Defect_Register_M3.csv and docs/m3/test-plan-and-case-matrix.md.

## D8. Updated registers, AI accountability and residual risks

Original RISK-001..008 and M1/M2 decision records are preserved in the historical PED. Updated M3 risk notes are in docs/registers/Risk_Register_M3.md; high risks include RISK-009 Basic auth on isolated staging, RISK-010 unapproved A-005 business rules, RISK-011 no runtime evidence and RISK-012 inability to simulate production reliability. Technical debts DEBT-001..007 include authentication, final policy, client-side scalability, no push notifications and recovery drills. The AI usage register documents the AI-assisted preparation and expressly records unavailable verification; students must own correctness, review fixes and reject unsupported evidence. See docs/registers/Technical_Debt_Register_M3.csv, AI_Usage_Register_M3.md and ADR_Change_Log_M3.md.

## D9. Release-quality review and final judgement

Classification on 12 October 2026: **NOT READY** for formal M3 evidence sign-off, because Maven/JUnit, npm/Vitest/build, live DB/API/E2E, performance results, GitHub CI/reviews, final release tag and stakeholder acceptance are unverified or unapproved in this environment. Source-level design continuity and the limited local static check are evidenced. Consult docs/m3/release-evidence-summary.md for gate-by-gate status and remediation. Never relabel until the exact commit passes required CI, staging E2E, measured load and human acceptance with recorded defects/risks.

Residual risk persists after passing tests: final category/status/overdue decisions, staging-vs-production identity/TLS/backup, insufficient representative load, and client-side filters. Real production release would require materially stronger hardening, stakeholder rules, identity verification, backups/restore and operational checks. A M3 student-capacity release candidate can become CONDITIONALLY READY when core evidence actually passes and nonblocking constraints are agreed.

## D10. Traceability, demo, submission and defence index

Pedagogic trace pattern: STK-01/02/03/04 -> original FR/NFR and AC (historical PED Sec 6) -> ADR-M2/ADR-M3 -> implementation class/endpoint -> identified testcase -> actual CI/JUnit/JSON result -> defect/risk -> release judgement. Example FR-010: manager accountability AC-FR-010 -> ADR-M2-DES-001 -> RequestAudit/ManagementController/AuditView -> E2E02 expected actor/action/time -> real staging output pending -> RISK-011 -> NOT READY. Example FR-007: controlled transitions AC-FR-007 -> ADR-M2-DES-002 + ADR-M3-001 -> RequestService/Policy/version -> decision-table JUnit and API04 invalid 409 -> execution pending -> DEF-002 -> release gate pending.

For the live team demo show the actual final Git tag and full commit, review discussions with two non-author approvals, CI results, staging health/release identity, role workflows, negative access, API/system tests, actual load results, quality gates and risk rationale. Every student must explain one complete trace, change, failure and release judgement, not only a scripted role. Read docs/m3/team-demo-and-individual-defence.md and docs/m3/m3-submission-checklist.md. Formal Moodle submission normally consists of the *continued PED v3.0 PDF*, actual GitHub URL, assessed RC commit/tag and staging/demo information, not a detached source ZIP.

---

## Post-public-stage editorial note (16 October 2026; for PED v3.1 drafting)

The Part D narrative above is the historical 12 October v3.0 preparation view. Since then, the team reported public Render/Neon/Firebase/Brevo staging working, a later responsive/Profile update deployed, and 47/47 backend plus 16/16 frontend automated tests run successfully on a local workstation. Do not portray this as evidence of GitHub CI, formal acceptance, measured performance, RLS V9 or all Firebase system scenarios; those need actual logged execution and traceability. The submission repository is `https://github.com/Thabani-Ndlozi/SEN381-CivicConnect` (distinct from staging repo). See `docs/m3/M3_PED_AUTHOR_HANDOFF.md`, `docs/m3/M3_TEST_EXECUTION_EVIDENCE_ADDENDUM.md` and `docs/m3/M3_MANUAL_ACCEPTANCE_CASES.csv` and issue a newly versioned PED PDF after confirmation.
