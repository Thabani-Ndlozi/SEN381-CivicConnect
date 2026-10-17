# Instructions for the person compiling the final Project Engineering Document (PED)

**Versioned addendum - 16 October 2026.** Follow `docs/m3/M3_TEST_EXECUTION_EVIDENCE_ADDENDUM.md` and `docs/m3/M3_MANUAL_ACCEPTANCE_CASES.csv`. The older packaged PED PDF contains genuinely historical statements (dated 12 Oct) and must be updated with a new version and change log rather than passed off as current.

## Final submission section checklist

1. Title/cover: team names, student IDs from approved records, module, M3 scope, final date, PED version, assessor instructions, actual submission repo URL.
2. Configuration and architecture: Spring Boot 4.1.1/Java 25, React 19/TypeScript/Vite, PostgreSQL 18/Flyway V1..V9, role-level RLS, Firebase Auth, Brevo, Google Maps, Render/Neon public staging vs local Compose; boundaries and security risks. The original 12 Oct document may still describe Basic-only auth and isolated staging; update narrative accurately.
3. Team collaboration: issue IDs, three independently reviewed PRs, review feedback with author responses, actual Git history; record AI assistance truthfully. Distinguish repository work from staged deployment in `sen381-staging/CivicConnect`.
4. Tests/QA: attach executed Maven JUnit 47 tests and frontend 16-test user run as reported history, with dates and actual screenshots/logs; rerun on final submission commit including the six prepared PR3 regression cases. Distinguish unit, boundary/equivalence, API contract, RLS, Firebase E2E, UI acceptance, performance, recovery and security tests; use table below as index.
5. Coverage and RTM: link each FR/NFR acceptance criterion to implementation, test case ID, observed evidence, defect/issue and interpretation. Current continued RTM (`docs/registers/RTM_CONTINUED.csv`) is not necessarily the team's authoritative live RTM; reconcile it rather than overwriting.
6. Defect, risk, debt, decision and AI registers: mark status based on observed fixes/retests, keep limitations and any unresolved A-005/overdue business-policy questions visible.
7. Operational release: actual public staging link, API health output, Firebase authorized domains, Brevo delivery evidence, V9 Flyway success, secret handling, free-tier constraints, photo storage behavior, recovery or outstanding drill, exact artifact SHA/tag.
8. Sign-off: human role UAT acceptance, link real CI run and reviewed PRs, final release verdict with residual risks and approvals; do not claim unrun cases passed.

## Evidence status summary (as at this packaging step)

- Reported successful by the user in prior messages: backend Maven 47/47; frontend Vitest 16/16 and build; Docker image build; working online Firebase login/Superuser page; CORS 200; notifications delivered; deployed responsive update working.
- Implemented/prepared but needs updated runs: PR3 additional frontend regression cases, GitHub Actions on new submission repo, database RLS suite, current V9 Flyway query.
- Planned/not documented as executed: scripted Basic-auth API/E2E smoke on isolated Compose, measured performance load, backup/restore drill, formal mobile accessibility/browser acceptance test log, persistence after Render restart.

**Do not create artificial results, commits, approvals, screenshots or dates.** Present pending cases as planned with input and expected outcome; turn them PASS only after actual execution and recorded evidence. The three people can each own and execute real review/test tasks to create meaningful contribution evidence.
