## Milestone 2 — Implementation and Integration

CivicConnect Milestone 2 extends the existing engineering foundation with a Spring Boot backend, React/TypeScript frontend, PostgreSQL persistence environment, role-based development security, and pull-request CI.

### Technology baseline

- React + TypeScript + Vite frontend
- Java 25 + Spring Boot 4.1.1 backend
- PostgreSQL 18.6
- Flyway database migrations
- Maven backend build
- Node.js 24 frontend build environment
- Docker Compose for local PostgreSQL
- GitHub Actions for pull-request CI

### Repository structure

```text
backend/      Spring Boot backend
frontend/     React + TypeScript + Vite frontend
.github/      GitHub Actions CI workflow
docs/         M2 technical and evidence documentation
compose.yaml  Local PostgreSQL environment
.env.example  Safe local configuration template
```

### Local configuration

Real development credentials must not be committed to the repository.

Each developer should create a local `.env` file from `.env.example` and provide their own local values.

PowerShell:

```powershell
Copy-Item .env.example .env
```

The populated `.env` file must remain local.

### Database

Start the local PostgreSQL service from the repository root:

```powershell
docker compose up -d postgres
docker compose ps
```

### Backend

Requirements:

- Java 25
- Maven

From the `backend` directory:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Backend:

```text
http://localhost:8080
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

### Frontend

Requirements:

- Node.js 24
- npm

From the `frontend` directory:

```powershell
npm ci
npm run dev
```

Frontend:

```text
http://localhost:5173
```

### Development security

The M2 bootstrap provides development-only role-based Spring Security using HTTP Basic authentication.

The development roles are:

- `REQUESTER`
- `STAFF`
- `MANAGER`

Development usernames and passwords are read from environment configuration and are not intended as the final production authentication solution.

### Progressive M2 integration

Milestone 2 is integrated progressively through separate feature branches and Pull Requests:

1. Bootstrap, security, configuration and CI
2. Persistence and audit
3. Request lifecycle, REST API and role-based frontend

Each substantive Pull Request must be reviewed by the two non-author team members before merge.

### Verification

Full integration verification is performed after all three M2 contributions have been merged into `main`.

The final integrated repository should verify:

- Backend tests
- Frontend tests
- Frontend production build
- PostgreSQL and Flyway startup
- Requester functionality
- Staff functionality
- Manager functionality
- Persistence to `service_requests`
- Audit records in `request_audit`
- Role-based access control
- No committed credentials

### M2 evidence

Implementation evidence should come from the controlled team repository and include:

- GitHub Issues and assignees
- Feature branches
- Progressive commit history
- Pull Requests linked to Issues
- Two non-author approvals on substantive Pull Requests
- CI/build/test results
- Database and audit evidence
- Working Requester, Staff and Manager paths
- Review comments and resulting changes
- AI Usage Register entries where applicable

See `docs/m2-evidence-map.md` and `docs/code-comparison-checklist.md` for supporting M2 documentation.