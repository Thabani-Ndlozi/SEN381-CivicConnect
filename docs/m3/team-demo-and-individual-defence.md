# M3 team presentation + individual engineering defence preparation

The following is a **rehearsal checklist**, not a claim any student delivered a live demonstration. Team members from the approved original PED: **Thaban Ndlozi**, **Anele Nkayi**, **Lebogang Masia**. These are suggested talking-point allocations, not invented Git contributions.

**Team demo, on identified final release commit/tag:** (1) Show PED v3.0 submission information/evidence links and original M2 historical pages. (2) Show GitHub protected main and two non-author approvals/review changes on a substantive PR; cite CI URL. (3) Show `/api/release` exactly matches Git tag; demonstrate staging health. (4) Requester enters valid/invalid request, views list; staff accepts, attempts forbidden status, then in-progress/resolved/closed with mandatory reason; requester views audit. (5) Manager inspects category/status breakdown and actor audit. (6) Show actual tests/matrix, security/dependency and performance JSON, defect/risk/debt/AI records. (7) Explain overdue requirement A-005 gap and current readiness classification.

**Individual defence assignments to rehearse (all students should understand all):**
- **Thaban — technology/SCM/CI:** explain ADR-M2-TECH-001 and ADR-M3-003, exact tag vs deployed config, main protection/two approvals, hard gates, credentials and residual Basic-auth risk. Show authentic commits/reviews only after team creates them.
- **Anele — architecture/service and lifecycle:** explain modular-monolith and dependency boundaries, test state-transition decision table, stale 409, one unexpected invalid input, transactional audit and defect correction.
- **Lebogang — data/traceability/verification:** trace FR-010/NFR-004 to `request_audit`, `AuditView.management`, E2E02, actual execution result, defect and risk. Explain how PostgreSQL Flyway and optimistic locking support integrity; distinguish mock test vs DB evidence.

**Practice prompts for each person:** trace one AC from original PED to RTM row, code, exact test assertion, observed CI output and a residual risk; explain an implemented divergence and tradeoff; answer why green CI doesn't prove user acceptance; identify a failing/blocked gate and how to fix it; explain limits of 40-request load; explain AI contribution and what human reviewers verified.

**Not yet available:** authentic individual contribution histories, PR review comments, CI failures/fixes, test screenshots, assessor presentation, actual user feedback. These require humans and live GitHub/staging; do not invent them.
