# M2 code evidence map — update the live RTM only after team verification

This file is a handoff for the team's **existing RTM**. It is not a replacement RTM.

| M1 requirement / driver | Architecture responsibility | Data/design/interface decision | Technology | Candidate application artefact | Initial verification |
|---|---|---|---|---|---|
| FR-001 Submit request | Request module | Service-layer validation + transaction; REST browser boundary | Spring Boot, JPA, PostgreSQL, React | `RequestController`, `RequestService.create`, `ServiceRequest`, V1 migration, `App.tsx` | `RequestServiceTest`; manual/API path after run |
| FR-003 View status/history | Request module | Request query interface; REST browser boundary | Spring Boot + React | `GET /api/requests/mine`, request list UI | Frontend build/test; manual role test after run |
| FR-006 Assign responsibility | Request module | Application-service ownership update | Spring Boot/JPA | `StaffRequestController.accept`, `RequestService.acceptOwnership` | Planned service/API test |
| FR-007 Controlled transitions | Request + Audit modules | Provisional policy; request + audit atomic transaction; optimistic locking; after-commit Observer | Spring/JPA/PostgreSQL | `RequestStatusTransitionPolicy`, `RequestService.changeStatus`, `RequestAudit`, `RequesterFeedbackListener` | `RequestStatusTransitionPolicyTest`; add integration test before baseline claim |
| FR-009 Management oversight | Management module | Read/summary query | Spring Data JPA | `/api/management/summary` | Planned API test |
| NFR-002 Authorisation/privacy | Security boundary | Role-based endpoint access | Spring Security | `SecurityConfig` | Add security endpoint tests |
| NFR-003 Secrets/configuration | Configuration boundary | Externalised variables; no live credentials in Git | Spring config, `.env.example`, GitHub Actions | `application.yml`, `.env.example` | Repository review / secret scan in real PR |
| NFR-004 Auditability | Audit module | Mandatory audit write in transaction | JPA/PostgreSQL | `AuditService`, `RequestAudit`, migration | Add transaction rollback/integration test |

## Minimum team actions before this becomes real M2 evidence

1. Review against Masia's final data/persistence decision and update the model if needed.
2. Validate the provisional status/category rules from A-005 or keep them clearly controlled as provisional.
3. Install dependencies and commit the real frontend lockfile.
4. Generate and commit the official Maven Wrapper if the team keeps Thaban's wrapper decision.
5. Run backend/frontend builds and tests on the selected versions.
6. Run the Flyway migration against PostgreSQL 18.6.
7. Add missing security/transaction/API integration tests appropriate to the implemented path.
8. Merge through a real Issue -> branch -> PR -> two non-author approvals.
9. Populate the live RTM with the actual Issue/PR/class/test references.
