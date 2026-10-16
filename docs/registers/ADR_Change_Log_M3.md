# ADR / controlled design changes — carried forward from M2

Original decisions retained in **PED v2.0 Part B**: `ADR-M2-ARCH-001` modular monolith, `ADR-M2-DES-001` transactional audit/in-process events, `ADR-M2-DES-002` optimistic concurrency, `ADR-M2-TECH-001` Java 25 / Spring Boot / React / PostgreSQL, `ADR-M2-DEP-001` container-ready/provider deferred, `ADR-M2-CI-001` PR-visible CI. No architecture or hosting provider replacement claimed.

## ADR-M3-001 — Reconcile implemented status changes with M2 optimistic concurrency
- **Previous M2 decision:** `ADR-M2-DES-002` intended a version field in status-change payload. The submitted M2 prototype omitted that field even though the persisted request had an `@Version` column.
- **Evidence/problem:** `ChangeStatusCommand` was missing the client version; racing/stale updates could overwrite state unexpectedly.
- **Decision:** require non-null `version` in status-change request; reject a mismatch with HTTP 409; use existing JPA `@Version`, audit and transaction. React forwards its current version.
- **Trade-off:** clients must reload after conflicts; real two-client concurrency still requires DB integration evidence.
- **Trace:** FR-007, NFR-004; `RequestService`, `ChangeStatusCommand`, `api.ts`; `RequestServiceM3Test.staleVersionRejectedBeforeAudit`; `API04`.
- **Status:** implemented in source; execution pending.

## ADR-M3-002 — Expose role-filtered audit as requester feedback (no third-party provider)
- **M2 baseline:** lifecycle audit committed atomically; feedback listener event is secondary; no external notification provider selected.
- **Gap:** requester UI had no access to audit/history, so FR-004/FR-010 were incompletely observable.
- **Decision:** add requester-owned history endpoint with `AuditView` projection omitting actor; separate manager endpoint includes actor and timestamp; reject cross-requester IDs via 404.
- **Trade-off:** feedback is in-app/poll-on-open only, not push/SMS/email; auth remains staging-only.
- **Trace:** FR-003/004/010, NFR-002/004; `AuditView`, `RequestService`, requester/manager controllers; API06, E2E01/02.
- **Status:** implemented in source; execution pending.

## ADR-M3-003 — Isolated Docker Compose staging instead of premature public hosting
- **M2 direction:** `ADR-M2-DEP-001` explicitly deferred provider selection.
- **Decision:** Compose/Postgres with distinct `staging` profile, randomly generated local env credentials and loopback-only browser/API ports; backend `/api/release` endpoint identifies configured RC.
- **Rationale:** enables repeatable student-scale verification without cloud spend or a public HTTP Basic endpoint.
- **Trade-off:** cannot assess real municipal availability/scale, accessible only when demonstrated on a machine, limited backup semantics.
- **Trace:** NFR-003, RISK-009/011; `compose.staging.yaml`, Dockerfiles, `scripts/setup_staging.py`, E2E01/02, performance-01.
- **Status:** configuration prepared; deployment execution pending.
