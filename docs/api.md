# CivicConnect REST contract — M3 continuation

API root at `http://localhost:8082` (simulated staging; local only). Development root `http://localhost:8080`. Basic credentials are environment-injected and strictly non-production. Do not share password values in evidence. Content type `application/json`.

| Role | HTTP/endpoint | Response / noteworthy contract |
|---|---|---|
| Public | `GET /actuator/health` | Spring actuator health |
| Public | `GET /api/release` | `{ "release": "<staging RC_VERSION>" }`; compare with final tag |
| REQUESTER | `POST /api/requests` | JSON `{ "title": "...", "description": "...", "category": "FACILITY_FAULT" }` -> **201** and unique ID/status/version; validation failures **400** |
| REQUESTER | `GET /api/requests/mine` | Authenticated requester's own list only |
| REQUESTER | `GET /api/requests/{id}/history` | Owner-only in-app feedback, actor omitted/null; unrelated ID -> **404** |
| STAFF or MANAGER | `GET /api/staff/requests` | Staff queue (all requests) |
| STAFF or MANAGER | `GET /api/staff/requests/{id}` | Detail by UUID; unknown -> **404** |
| STAFF or MANAGER | `POST /api/staff/requests/{id}/accept` | Only unassigned SUBMITTED; success 200 with owner/status/version; subsequent accept **409** |
| STAFF or MANAGER | `POST /api/staff/requests/{id}/status` | `{ "targetStatus": "IN_PROGRESS", "comment":"Work started", "version":1 }`; current-owner-only except unassigned SUBMITTED->REJECTED with mandatory reason. Invalid/stale -> **409**; nonowner -> **403**; missing version -> **400** |
| MANAGER | `GET /api/management/summary` | Record from each status to count |
| MANAGER | `GET /api/management/breakdown` | Category -> status -> count |
| MANAGER | `GET /api/management/requests/{id}/history` | History includes actor, action, prior/next status, comment and timestamp |

### Status-transition table (provisional A-005; stakeholder approval required)

- SUBMITTED -> ACCEPTED (through ownership accept) or REJECTED (staff with reason)
- ACCEPTED -> IN_PROGRESS or REJECTED
- IN_PROGRESS -> RESOLVED
- RESOLVED -> CLOSED or IN_PROGRESS
- REJECTED and CLOSED -> terminal

`RESOLVED`, `CLOSED` and `REJECTED` need a nonblank comment. Concurrent/stale payload version fails with HTTP 409; reload request first. `version` is the JPA optimistic-lock counter and must be sent by callers. On authentication failure expect HTTP 401; insufficient role expect HTTP 403. Requester-visible event histories omit privileged staff actor identifiers; managers can inspect them.

**API performance:** `GET /api/management/summary` is the chosen PER-01 load exercise; see scripts/performance.py. **Overdue:** no status or SLA is implied or computed until original A-005 is resolved.
