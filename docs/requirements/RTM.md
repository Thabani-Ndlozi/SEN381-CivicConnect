# Requirements Traceability Matrix — M1 (Historical Baseline)

| Source / Stakeholder | Requirement ID | Requirement | Priority | Acceptance Criteria | Change / Issue / PR | Design / Architecture | Implementation | Test | Acceptance / Release Evidence |
|---|---|---|---|---|---|---|---|---|---|
| STK-01 / Master Section 3 | FR-001 | Submit service request | High | AC-FR-001 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-01 / Master Section 3 | FR-002 | Controlled categorisation | High | AC-FR-002 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-01 / Master Section 3 | FR-003 | View status and history | High | AC-FR-003 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-01 / Master Section 3 | FR-004 | Requester lifecycle feedback | High | AC-FR-004 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-02 / Master Section 3 | FR-005 | Staff view/search/filter/details | High | AC-FR-005 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-02 / Master Section 3 | FR-006 | Assign/accept responsibility | High | AC-FR-006 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-02 / A-005 | FR-007 | Controlled status/actions/comments | High | AC-FR-007 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-02 / Master Section 3 | FR-008 | Resolve/close requests | High | AC-FR-008 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-03 / Master Section 3 | FR-009 | Management activity oversight | High | AC-FR-009 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| STK-03/04 / Master Sections 2-3 | FR-010 | Lifecycle accountability information | High | AC-FR-010 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| C-005 / STK-01/02/03 | NFR-001 | Core workflow usability | High | AC-NFR-001 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| C-006 / A-006 | NFR-002 | Role-based access/privacy | High | AC-NFR-002 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| C-006 / C-009 | NFR-003 | Secrets/configuration security | High | AC-NFR-003 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |
| C-005 / STK-03 | NFR-004 | Auditability of lifecycle changes | High | AC-NFR-004 | Change: N/A; exact Issue/PR in live RTM. | Deferred to M2 | Deferred to M3 | Deferred to M3 | Deferred to M4 |

---

# Requirements Traceability Matrix — M3 Reconciled Final Baseline

> **Authoritative Live Register:** The complete, continuously maintained Requirements Traceability Matrix is hosted at [`docs/registers/RTM_CONTINUED.csv`](../registers/RTM_CONTINUED.csv) and documented in [`docs/registers/RTM_PROVENANCE_AND_HANDOFF.md`](../registers/RTM_PROVENANCE_AND_HANDOFF.md). Narrative traceability and verification synthesis are compiled in **Project Engineering Document (PED v3.0), Section D11**.

| Requirement ID | Requirement Summary | Stakeholder / Source | M2 Design / ADR | M3 Implementation Component | Verification / Test IDs | M3 Status | Authentic Execution Evidence |
|---|---|---|---|---|---|---|---|
| **FR-001** | Submit service request | STK-01 / Master Sec 3 | ADR-M2-ARCH-001 | RequestController; RequestService; CreateRequestCommand; App.tsx | BB-01..BB-08; API03; E2E01 | **VERIFIED (PASS)** | 47 Maven tests (BB-01..08); test_API03 & test_E2E01 pass in smoke-results.json |
| **FR-002** | Controlled categorisation | STK-01 / Master Sec 3 | ADR-M2-ARCH-001 | RequestCategory; ServiceRequest; schema V1; App.tsx | BB-01; E2E01 | **VERIFIED (PASS)** | BB-01 validation; schema V1 check constraint; test_E2E01 pass in smoke-results.json; 6 categories active |
| **FR-003** | View status and personal history | STK-01 / Master Sec 3 | ADR-M2-ARCH-001 / REST | GET /api/requests/mine; RequestService.myRequests; App.tsx | API01; E2E01 | **VERIFIED (PASS)** | test_API01 (401 unauthorised) & test_E2E01 pass in smoke-results.json; App.tsx requester view |
| **FR-004** | Requester lifecycle feedback | STK-01 / Master Sec 3 | ADR-M2-ARCH-001; ADR-M3-002 | GET /api/requests/{id}/history; AuditView.requester; App.tsx | API06; E2E01 | **VERIFIED (PASS)** | test_API06 (staff identity redacted) & test_E2E01 pass in smoke-results.json; RequesterFeedbackListener |
| **FR-005** | Staff view, filter, sort and details | STK-02 / Master Sec 3 | ADR-M2-ARCH-001 | GET /api/staff/requests; GET /{id}; StaffController; App.tsx | API02; API04; E2E01 | **VERIFIED (PASS)** | test_API02 (requester barred with 403) & test_E2E01 pass in smoke-results.json; 22 Vitest tests |
| **FR-006** | Assign/accept responsibility | STK-02 / Master Sec 3 | ADR-M2-DES-001; ADR-M2-DES-002 | ServiceRequest.assignTo; RequestService.acceptOwnership; request UI | UT-02..UT-05; API05; E2E01 | **VERIFIED (PASS)** | UT-02..05 pass; test_API05 (403 for non-owner) & test_E2E01 pass in smoke-results.json; ownership guard verified |
| **FR-007** | Controlled status transitions | STK-02 / A-005 | ADR-M2-DES-001; ADR-M2-DES-002; ADR-M3-001 | RequestStatusTransitionPolicy; ChangeStatusCommand(version); RequestService | UT-07..UT-15; API04; E2E01 | **VERIFIED (PASS)** | UT-07..15 pass; test_API04 (invalid transition returns 409) & test_E2E01 pass in smoke-results.json |
| **FR-008** | Resolve/close requests | STK-02 / Master Sec 3 | ADR-M2-DES-001 | RequestService.changeStatus; RequestAudit; App.tsx | UT-16..UT-17; E2E01 | **VERIFIED (PASS)** | UT-16..17 pass; test_E2E01 lifecycle completion in smoke-results.json; mandatory reason enforced |
| **FR-009** | Management activity oversight | STK-03 / Master Sec 3 | ADR-M2-ARCH-001 / Query | ManagementController.summary/breakdown; App.tsx | E2E02; PERF-01 | **VERIFIED (PASS)** | PERF-01 (40 reqs, 0 err, p50=135ms, p95=148ms) in performance-results.json; test_E2E02 pass |
| **FR-010** | Lifecycle accountability information | STK-03/04 / Master Sec 2-3 | ADR-M2-DES-001; ACID audit | GET /api/management/requests/{id}/history; AuditView.management; RequestAudit | E2E02; UT-16 | **VERIFIED (PASS)** | UT-16 pass; test_E2E02 (actor, action, timestamp verified) pass in smoke-results.json |
| **NFR-001** | Core workflow usability | C-005 / STK-01/02/03 | ADR-M2-ARCH-001 / UI | App.tsx on-screen forms/history/status; responsive mobile cards | manual UI demo; M3_MANUAL_ACCEPTANCE_CASES.csv | **CONDITIONAL (UAT pending)** | Responsive UI and card layout implemented; 43 manual acceptance cases defined; formal UAT pending |
| **NFR-002** | Role-based access and data privacy | C-006 / A-006 | ADR-M2-TECH-001; Spring Security; PostgreSQL RLS | SecurityConfig; RequestService.requesterHistory; RLS policies; V10 migration | API01; API02; API05; API06; RLS-01..04 | **VERIFIED (PASS)** | test_API01, 02, 05, 06 pass in smoke-results.json; 14 RLS assertions pass; NOSUPERUSER NOBYPASSRLS verified |
| **NFR-003** | Secrets and configuration security | C-006 / C-009 | ADR-M2-CI-001; ADR-M2-DEP-001 | staging env vars; gitignore; check_source.py; npm overrides | STATIC-01; SEC-02; SEC-03; npm audit | **VERIFIED (PASS)** | STATIC-01 pass in local-static-check.txt (0 secrets); DEF-007 resolved (0 npm vulns); TruffleHog verified |
| **NFR-004** | Auditability of lifecycle changes | C-005 / STK-03 | ADR-M2-DES-001; ACID audit | AuditService; request_audit; transactional RequestService; Flyway V1 | UT-16; E2E02; RLS-01..04 | **VERIFIED (PASS)** | UT-16 pass; test_E2E02 audit trail pass in smoke-results.json; atomic persistence in Flyway V1 |
