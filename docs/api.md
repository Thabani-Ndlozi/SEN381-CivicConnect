# Initial API / integration evidence

The browser is a separate runtime from the Spring Boot server, so HTTP is a justified network boundary here. Inside the backend, modules remain in-process.

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/requests` | REQUESTER | Submit a request |
| GET | `/api/requests/mine` | REQUESTER | View own request list/status |
| GET | `/api/staff/requests` | STAFF/MANAGER | View current requests |
| POST | `/api/staff/requests/{id}/accept` | STAFF/MANAGER | Accept ownership |
| POST | `/api/staff/requests/{id}/status` | STAFF/MANAGER | Apply a controlled status transition |
| GET | `/api/management/summary` | MANAGER | Basic counts by lifecycle status |
| GET | `/actuator/health` | Public health check | Initial operational health evidence |

## Error behaviour

- `400` validation failure.
- `401/403` authentication/authorisation failure.
- `404` unknown request.
- `409` invalid transition or optimistic-concurrency conflict.

## Security boundary

The M2 bootstrap uses Spring Security and role checks. The exact final browser authentication mechanism remains a controlled decision; the development profile uses environment-supplied HTTP Basic users only to exercise role boundaries without committing production credentials.
