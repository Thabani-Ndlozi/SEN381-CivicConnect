# Milestone 3 — controlled construction, evidence index and M2 conformance

**Original baseline preserved:** PED v2.0 (31 original pages), M2 prototype from `CivicConnect_M2_Code_Proposal.zip`. Release label *candidate*: `m3-rc1-pending-git-tag` is a placeholder, **not a Git tag or tested RC**; final assessed hash/tag must be stamped after team PR approval.

| Approved M2 baseline item | M3 source evidence / status | Controlled deviation or follow-up |
|---|---|---|
| React/TypeScript/Vite UI (`ADR-M2-TECH-001`) | `frontend/src/App.tsx`, `api.ts` augmented | Client-side filtering still a scalability tradeoff; human usability tests pending |
| Java 25 Spring Boot 4.1.1 modular monolith (`ADR-M2-ARCH-001`) | request/audit/management/security modules retained | M2 prototype had missing feedback and ownership controls; ADR-M3-002 |
| PostgreSQL 18.6 / Flyway V1, transactional audit (`ADR-M2-DES-001`) | original migration retained; `RequestService` persists audits in transaction | Database transactional E2E needs live Compose verification |
| Optimistic JPA versions (`ADR-M2-DES-002`) | version required by status payload; HTTP 409 stale conflict | ADR-M3-001 aligns omitted M2 proto payload with approved design |
| Roles / private settings | `SecurityConfig`, role-specific audit projection | Staging Basic credential mechanism retained *only* for loopback demo; production blocker |
| PR-visible GitHub CI (`ADR-M2-CI-001`) | `.github/workflows/ci.yml` updated for tests/QA/staging | No live GitHub CI run or PR has occurred |
| Container-ready, provider not selected (`ADR-M2-DEP-001`) | `compose.staging.yaml`, Dockerfiles, `application-staging.yml` | ADR-M3-003; no hosted staging URL, assessor live localhost demonstration |

**Integrated increment:** requester submits/reads requests and views history; staff accepts and changes lifecycle with reason and version; management sees counts, breakdown and privileged audit history. **Not in demonstrated scope:** real authentication, external messaging, production hosting, approved overdue rule, multi-tenant municipality deployment, production capacity/availability, final category catalogue validation.

**Quality prioritisation (SO25):** role boundary and ownership breaches, stale updates and missing audit are high risk; prioritised tests: API01/02/05/06, UT/domain, E2E01/02. Input validation: BB01–BB08; performance-sensitive management summary: PERF-01. Avoid claiming passing tests from source presence alone.
