# M3 planned/implemented verification catalogue — evidence record

**Do not confuse case definitions with executions.** Case code, purposeful input/expected outcome, risks and actual executable location are listed below. Execution remains pending unless noted. Maven/JUnit/Vitest were **not runnable** in this preparation environment; a small Python syntax and source-pattern check passed locally. Live staged cases report their actual results in `evidence/smoke-results.json` only **after execution**.

| Case IDs and executable | Level/technique & risk basis | Controlled test data and expected (oracle) | Status/evidence |
|---|---|---|---|
| UT-01–UT-06 `ServiceRequestRulesTest` | Unit; initial state, ownership, assignment/negative; FR-006, NFR-002 | Submitted/unassigned; accept by owner; second owner and unassigned mutator rejected | **NOT RUN** (JUnit XML in CI) |
| UT-07–UT-15 `TransitionDecisionTableTest` | Unit + black-box decision table/state transition; FR-007, A-005 | 9 deliberately chosen allowed/forbidden state pairs; forbidden 409 exception | **NOT RUN** (JUnit XML in CI) |
| UT-16–UT-23 `RequestServiceM3Test`, `RequestServiceTest` | Component; audit, stale update, owner, history & reason; FR-001/004/006/007/008/010 | verifies audit actor/action/status; no write on invalid/foreign/stale; reason required | **NOT RUN** (JUnit XML in CI) |
| BB-01–BB-08 `CreateRequestValidationTest` | **Equivalence Partitioning + Boundary Value Analysis**; FR-001/002 | valid typical, lengths 1/160/161, whitespace, missing category, description 4000/4001; valid no constraint violations, invalid violations | **NOT RUN** (JUnit XML in CI) |
| API-C01..03 `RequestApiContractTest` | Controller black-box and error mapping; FR-001, NFR-002 | missing title/category 400; missing history ID 404 | **NOT RUN** (JUnit XML in CI) |
| FE-01..05 `frontend/src/api.test.ts` | Frontend component/API contract; FR-001/004, NFR-002 | 401/403 errors; create JSON category; requester/manager distinct URL paths | **NOT RUN** (Vitest in CI) |
| API01 `scripts/smoke.py` | Live API negative; NFR-002 | no auth GET mine -> 401 | **NOT RUN** (staging results) |
| API02 `scripts/smoke.py` | Live API role denial; NFR-002 | requester GET staff queue -> 403 | **NOT RUN** |
| API03 `scripts/smoke.py` | Live API equivalence partition; FR-001 | blank title -> 400 | **NOT RUN** |
| API04 `scripts/smoke.py` | Live API state negative; FR-007 | accepted request forced directly to closed -> 409, state preserved | **NOT RUN** |
| API05 `scripts/smoke.py` | Live API owner denial; FR-006/007, NFR-002 | manager not owner changes staff-owned request -> 403 | **NOT RUN** |
| API06 `scripts/smoke.py` | Live API privacy/audit; FR-004/010 | own history allowed, actor hidden; manager-only history forbidden requester | **NOT RUN** |
| E2E01 `scripts/smoke.py` | **System journey 1**, transaction chain; FR-001/003/004/006/007/008 | requester creates, views list; staff accepts->in-progress->resolved->closed with version and reason; requester audits all 5 states | **NOT RUN** (must execute on staged app+Postgres) |
| E2E02 `scripts/smoke.py` | **System journey 2**; FR-009/010/NFR-004 | requester creates; staff accepts->work; manager sees actor audit/counts/category breakdown | **NOT RUN** |
| PERF-01 `scripts/performance.py` | Small performance/load; management counts & database query; FR-009 | 40 GET requests, 4 workers; record time p50/p95, success/errors/throughput; aspirational internal smoke hypothesis: 0 failures / p95<1000ms, NOT approved SLO | **NOT RUN** (measured data generated on execution) |
| STATIC-01 `scripts/check_source.py` | Static and limited secret heuristic | No committed env secrets/private key patterns or unsafe code patterns | **PASS locally**: `evidence/local-static-check.txt`; limitations below |
| SEC-02 `npm audit --omit=dev --audit-level=high` | Dependency risk | No high/critical production dependency advisory in configured registry | **NOT RUN** (CI) |
| SEC-03 `trufflesecurity/trufflehog@main` | Commit secret-pattern scan | No verified secret in checked code history | **NOT RUN** (CI) |

**Test evidence record schema:** ID -> source requirement/risk -> test input/precondition -> expected/oracle -> actual observation -> status -> timestamp/runtime/environment -> evidence path/CI URL -> defect/ref -> interpretation and limits. Run `mvn -B verify`, `npm ci && npm test && npm run build`, `python3 scripts/check_source.py`, then simulated staging smoke/load tests. Preserve Maven Surefire XML, Vitest log, CI URL, JSON outputs. Many tests are overlapping by design; do not double count them as independently verified behaviours without assessor approval.

**Limitations:** Java command/record tests rely on actual Maven dependencies; standalone MockMvc is controller-boundary evidence, not proof of enforced Spring Security filters; live auth/DB/E2E are required. `scripts/check_source.py` is heuristic, not a comprehensive SAST and cannot verify uncommitted external Git history. Staging exercises API-through-DB system journeys; manual browser acceptance remains necessary to validate UI usability.

---

## Dated addendum (16 October 2026) - public Firebase and responsive UI acceptance

The table above is the historical prepared candidate test catalogue. Subsequent user-provided console evidence reports **47/47 Maven tests and 16/16 existing Vitest tests passed on 16 October 2026**, plus a successful frontend build and a live Render stage with Firebase and Brevo. These reports do not establish pass statuses for each independent E2E, RLS, performance, security, or browser case. See `M3_TEST_EXECUTION_EVIDENCE_ADDENDUM.md` for source/limits and `M3_MANUAL_ACCEPTANCE_CASES.csv` for 43 detailed execution sheets. Re-run tests on the *final submission repository SHA* and attach actual CI artifacts. Scripted `smoke.py` uses Basic authentication in isolated Compose, not Firebase live-stage authentication.
