# AI usage register — M3 continuation

This entry supplements the original M1/M2 AI register preserved in the source PED v2.0. Earlier usage records should remain unchanged.

| ID | Date | Tool | AI-assisted materials | Verification performed | Human check required | Owner/accountability |
|---|---|---|---|---|---|---|
| AI-M3-001 | 2026-10-12 | OpenAI ChatGPT | Proposed Java/backend/frontend updates to the submitted M2 prototype; tests; staging Docker/CI workflow; RTM expansion; registers; PED v3.0 addition; GitHub instructions | Compared to M3 brief, original PED and M2 code; Python scripts compile; local static/secrets heuristic passed; file/baseline checks recorded | Team must review code, run Java/Node/Docker CI, inspect failing/passing test results, reconcile exact original RTM, approve wording and architecture, demonstrate manually | All three student authors remain accountable for acceptance and correctness; AI is not an approver |

**Verified limitations:** ChatGPT environment did not have Java 25, Maven, Docker or network dependency resolution, so no successful Maven/Vitest/Compose/API/performance execution has been asserted. No fabricated PRs, GitHub action links, code coverage numbers, external approvals, user acceptance or measured load results are entered. An example requiring **human correction**: the original M2 change-status command had no version field despite `ADR-M2-DES-002`; source was changed to require it and return 409 for stale payloads, but the fix still requires integration verification.
