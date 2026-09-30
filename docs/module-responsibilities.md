# Backend modular-monolith responsibilities

| Module/package | Responsibility | Must not own |
|---|---|---|
| `request.domain` | Request aggregate, categories/statuses, provisional transition policy | HTTP, database configuration, notifications |
| `request.application` | Use-case orchestration and transaction boundary | Provider-specific integration |
| `request.persistence` | Request repository port implemented with Spring Data JPA | Workflow policy |
| `request.web` | REST input/output boundary | Persistence rules |
| `audit` | Mandatory lifecycle audit history | Secondary notifications |
| `feedback` | Observer-style secondary reaction after commit | Correctness-critical request state |
| `management` | Initial management read/summary functions | Request mutation rules |
| `security` | M2 role boundary/bootstrap security | Final production identity decision |
| `shared` | Cross-cutting API exceptions only | Domain-specific business logic |

This structure is intended to show meaningful responsibility boundaries without creating microservices or unnecessary deployment units.
