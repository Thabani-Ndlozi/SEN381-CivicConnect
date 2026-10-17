# CivicConnect M3 - test execution and evidence addendum (16 October 2026)

> This addendum accompanies the historical PED v3.0 PDF dated 12 October. It does **not** rewrite the historical baseline or assert that unexecuted tests passed. The PED author must update the final PDF after authentic evidence collection.

## Source and submission repository

- Team submission repository: `https://github.com/Thabani-Ndlozi/SEN381-CivicConnect` (existing M1/M2 baseline; not the separately deployed Render staging repository).
- Source snapshot: user-supplied `CivicConnect-main.zip` (16 October 2026), partitioned into three ordered PR overlays.
- Staging site separately deployed by the team: `https://civicconnect-web-staging.onrender.com`, API: `https://civicconnect-api-staging.onrender.com`.
- The staging repository and submission repository are **not** the same. Do not claim the submission repo's CI or PRs have passed merely because staging worked.

## Results that have actually been reported in this conversation

| Evidence category | Reported result | Evidence quality / limitation |
|---|---|---|
| Spring Boot backend test suite | **47 passed, 0 failed, 0 errors, 0 skipped**, Maven `BUILD SUCCESS`; executed by project member on 16 October 2026 | User-provided terminal excerpt, run on workstation before submission repo PRs. Obtain Surefire XML/complete saved log and record exact commit/branch. |
| React frontend | `npm run build` successful; **16 Vitest cases passed**, 5 files | User-provided terminal excerpt, before this QA PR adds regression tests. Capture local log and run on final submission SHA. |
| Backend image build | `docker build -t civicconnect-staging-review ./backend` completed | User-provided Docker terminal output; build command deliberately skips Maven tests, which were separately run. |
| Public Render API | Live, Spring Boot started, CORS preflight HTTP 200, Firebase Superuser login, Neon current runtime role unprivileged | User-provided Render/PowerShell output during earlier staging rollout. Not a full penetration or system test. |
| Stage functionality / Brevo | Team reported working Brevo email and other workflows; user reported responsive update works after correct PR merge | Team/user feedback, not an independently captured signed acceptance test record or individual-case execution log. |
| Flyway V9 | User reports successful backend deployment after merging updated source | Retrieve actual V9 row using `SELECT version, success FROM public.flyway_schema_history ORDER BY installed_rank DESC LIMIT 1` as evidence. |

**No new programmatic test runs were performed as part of creating this package.** The additional auth-message regression test file included in PR3 is prepared but is **not** marked passed until the team's npm/CI run confirms it.

## What exists in the supplied source

- Backend: 9 Java test files under `backend/src/test/java/com/civicconnect/`. The user's recorded Maven run reports 47 tests. Covers rules, transitions, creation validation/boundaries, API/controller contracts, location coordinates, media, ownership/audit.
- Frontend: 5 existing Vitest files (`api.test.ts`, `authErrors.test.ts`, `location.test.ts`, `media.test.ts`, `statusLabel.test.ts`), with 16 tests in the reported pre-package run.
- PR3 prepares a **sixth** frontend test file, `authErrors.regression.test.ts`, adding six auth-message regression cases. New total is *expected* to be 22 but is NOT CONFIRMED until executed.
- Database: `database/tests/rls.mjs` (requires a matching test database/tool setup). Earlier `evidence/roles-and-rls-validation.txt` refers to 14 assertions on an older release; do not treat as proof against this V9 release without rerun.
- Simulated staging scripts: `scripts/smoke.py` defines six API-negative/contract tests plus two API E2E journeys. `scripts/performance.py` defines a small management-summary load run. Neither has a current observed result attached to this ZIP.
- CI: `.github/workflows/ci.yml` defines backend, frontend, static/secret and isolated staging gates. Results are PENDING on the target submission repository until PR workflows complete.

## Gaps requiring real execution or formal acceptance

1. **Public Firebase system acceptance.** `scripts/smoke.py` uses HTTP Basic auth for **isolated Compose staging**. It cannot establish Firebase browser/API authentication correctness on public Render. Capture manual Firebase role journeys and/or add a dedicated Firebase integration harness.
2. **New responsive/profile features.** Provide test-case evidence for mobile cards/modals, 5-second toast, form reset, Help navigation, fixed header, status section ownership gate, change password (correct/incorrect old password), self-deactivation, protected Superuser, and no delete action. See `M3_MANUAL_ACCEPTANCE_CASES.csv`.
3. **Notifications and uploads.** Capture Brevo delivery/invitation/status log evidence with recipients masked; test photo durability after Render restart. Render Free local filesystem may not be persistent; don't claim persistence unless verified.
4. **Security and privacy.** Test negative cross-user data access under Firebase + Neon RLS, CORS allowed-origin behavior, revocation after deactivation; run security/dependency and secret audits and preserve results.
5. **Performance and recovery.** Execute the defined workload with actual environment, percentiles, errors and interpretation; do not supply invented timings. Rehearse backup/restore or explicitly document risk as accepted/pending.
6. **Traceability and governance.** Reconcile the original live RTM, current FR/NFR acceptance criteria, role matrix and unresolved A-005 business rule with M3 cases and actual CI artifacts. Each PR needs genuine substantive review, author/tester attribution and a final signed approval where required by rubric.

## Evidence capture template

For each case fill the `M3_MANUAL_ACCEPTANCE_CASES.csv` columns: execution status (`PASS` / `FAIL` / `BLOCKED` / `NOT RUN`), actual observed result, real tester, timestamp and timezone, environment, screenshot/log URL, issue number, exact commit SHA. Keep tokens/passwords, Firebase service JSON, Brevo keys, Neon credentials and private user information **out of the evidence**.

## PED correction / editorial action

The packaged historical `PED_v3.0_CivicConnect.pdf`, `docs/m3/PED_v3_continuation.md`, and `docs/m3/release-evidence-summary.md` were prepared on 12 October, before the reported 16 October test/deployment results. Their old 'NOT RUN' and 'NOT DEPLOYED' statements are period-specific, not an up-to-date submission verdict. Incorporate this dated addendum into a **new PDF edition with review/version history**, citing real proof and explicitly retaining open evidence gaps. Avoid silently revising an approved historical PED, and don't label the final release READY until all required gates genuinely pass.
