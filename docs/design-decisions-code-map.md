# Code-backed design decisions for team comparison

## Decision 1 — Observer / in-process application event for secondary lifecycle reactions

**Problem:** request status changes can trigger feedback/reporting reactions. Directly calling every secondary consumer from the lifecycle service would increase coupling.

**Candidate decision:** keep the request state change and mandatory audit write synchronous in the main transaction. Publish `RequestStatusChangedEvent` for secondary reactions and observe it after commit.

**Code evidence:**
- `request/application/event/RequestStatusChangedEvent.java`
- `feedback/RequesterFeedbackListener.java`
- `request/application/RequestService.java`

This matches Anele's current Design Decision 1.

## Decision 2 candidate — Application Service + Repository + optimistic concurrency for status changes

**Problem:** a request status update can become inconsistent if business validation, request persistence and audit history are split across unrelated code paths, or if two staff members overwrite each other's changes.

**Candidate approach:** the application service owns the use-case/transaction boundary; Spring Data JPA repositories handle persistence; `@Version` detects stale updates; the database migration supplies structural constraints; the mandatory audit write is part of the same transaction.

**Code evidence:**
- `request/application/RequestService.java`
- `request/domain/ServiceRequest.java` (`@Version`)
- `request/persistence/ServiceRequestRepository.java`
- `audit/application/AuditService.java`
- `db/migration/V1__initial_request_schema.sql`

This is only a **comparison candidate** for the team's second M2 design/approach decision. Masia's final persistence/design work may choose or refine it. Do not record it as an accepted ADR until the team reviews the alternatives and approves the final decision.
