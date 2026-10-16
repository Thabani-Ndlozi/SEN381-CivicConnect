# CivicConnect — shared login and database roles

## Changes

- One email/password login for requester, staff, and manager. There is no client-side role selector.
- Public registration always creates REQUESTER accounts, regardless of extra submitted fields.
- Only managers can create STAFF accounts and activate/deactivate requester or staff access.
- Managers are superusers within CivicConnect, not PostgreSQL superusers. They are provisioned outside the app.
- PostgreSQL supplies users, departments, category/status/priority options, requests, history, and help content. Dashboard statistics are calculated from the authorized database responses; no sample users or counts are embedded in the app.
- Page menus follow the authenticated account returned by the server. API authorization independently protects all routes. PostgreSQL RLS additionally protects request and audit rows, and account writes.
- Requesters can only read their own requests and timelines. Staff can see the service queue, accept unassigned requests, and update their own assigned work. Managers can update requests across the team, while retaining the existing lifecycle and comment rules.
- Database connections receive transaction-local identity, reset at commit/rollback. Startup refuses PostgreSQL superusers and BYPASSRLS roles.
- Passwords use BCrypt; the browser holds credentials only in React memory, not localStorage. Reloading requires login again. Use HTTPS outside local development.

## Database setup

Back up any existing database first. Retain the database volume; do not delete it.

1. Install the project's Java 25, Maven, Node.js, and PostgreSQL prerequisites.
2. Copy `.env.example` to `.env` and choose your own passwords. No default app credentials exist.
3. Start PostgreSQL (the existing `compose.yml` exposes local port 5433), or use your existing PostgreSQL instance. Create the `civicconnect` database if necessary.
4. Run the role setup as the PostgreSQL administrator:

   ```sh
   psql -h localhost -p 5433 -U postgres -d civicconnect -f database/provision.sql
   ```

   Match the two prompted role passwords to `DB_PASSWORD` and `MIGRATION_DB_PASSWORD`. Prompts are visible in psql: run in a private terminal. The script preserves existing roles/passwords rather than silently changing them.

5. Export the environment variables from `.env` in your terminal or configure them in your IDE. Spring Boot does not automatically load the root `.env` file. Required values are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MIGRATION_DB_USERNAME`, and `MIGRATION_DB_PASSWORD`.
6. Run the backend:

   ```sh
   cd backend
   mvn spring-boot:run
   ```

   Flyway applies V1–V5 on a new database, or later migrations up to V5 to an existing database. The existing V1 migration is unchanged. V2 already seeds catalog and help data. No mock accounts are created.
7. Once migrations complete, create the manager from a second terminal:

   ```sh
   psql -h localhost -p 5433 -U postgres -d civicconnect -f database/create_manager.sql
   ```

   Enter your own name, email, and password. The password must be at least 12 characters and at most 72 UTF-8 bytes. This script requires an administrator and the pgcrypto extension. It does not overwrite existing accounts.
8. Run the frontend:

   ```sh
   cd frontend
   npm ci
   npm run dev
   ```

   Open http://localhost:5173. Sign in as the manager, open User Management, and create staff accounts. Requesters use Create account on the same login page.
9. Optional demo requests, after registering a requester:

   ```sh
   psql -h localhost -p 5433 -U postgres -d civicconnect -f database/seed_demo.sql
   ```

   The script populates the database, not the UI. It is repeatable without duplicate demo titles for that requester.

### Existing baseline data

Old in-memory usernames are no longer login accounts. Existing request `requester_id`, `owner_id`, and audit `actor` values may contain old usernames. Create the matching accounts and explicitly migrate those identifiers to their verified lowercase email addresses as an administrator. Do not automatically assign historical records based on a guessed email. Managers can see historical requests before this mapping is done.

### Docker staging

Use the updated `.env` names in `compose.staging.yml`. Start the database service first, run `provision.sql` inside the PostgreSQL container with psql, then start `api` and `web`. Run `create_manager.sql` after migration. The API intentionally refuses the container's PostgreSQL administrator credentials. The older `scripts/setup_staging.py`, smoke/performance scripts, and milestone evidence documents refer to the original in-memory test-user baseline and must not be treated as current authentication setup instructions.

## Design and scope notes

The supplied blue/teal palette, logo, light backgrounds, rounded cards, sidebar, tables, chart composition, and shared requester-style login have been implemented. The original login illustration is reused directly as a decorative panel. Layouts adapt to smaller screens. This is a functional reconstruction from raster references, not a pixel-identical reproduction of every screenshot.

Only database-backed, supported operations are shown. The original backend has no Google OAuth, password-reset delivery, photo storage, email invitations, notification subscription, SLA/response-time rules, escalation model, or draft-request model. These are not falsely presented as working controls or fabricated statistics. Staff setup currently uses a manager-chosen initial password shared securely, rather than an invitation email. Account profile is read-only. Separate staff/manager login pages and manager public registration have intentionally been removed.

## Verification

- TypeScript and Vite production build: passed.
- Existing frontend tests: 7 passed.
- Existing backend tests: 37 passed, compiled/tested with Java 17 using a local `-Djava.version=17` override and explicit Mockito Java agent due to this execution environment's attachment restriction. The supplied Java 25 project setting is retained.
- All four SQL migrations executed in PGlite (PostgreSQL WASM).
- Fourteen RLS assertions passed: requester request/audit isolation, requester write denial, blocked staff/manager self-registration, staff queue visibility, ownership restrictions, manager updates/staff creation, anonymous visibility denial, and requester registration.
- Browser checks passed for all three role dashboards, restricted menus, request details, requester submission form, manager staff-creation panel, mobile overflow, and JavaScript errors. These use API test fixtures; they do not constitute a deployed backend/database integration test.
- Repeat the SQL checks with `cd database/tests`, `npm install`, and `npm test`.
- Run the full app against your own PostgreSQL deployment before release, including two different requesters, two staff members, and the manager. Check API denials as well as page visibility.

PostgreSQL RLS reference: https://www.postgresql.org/docs/current/ddl-rowsecurity.html

## Photos and Manager role filter (M3 addendum)

See `M3_PHOTOS_AND_ROLE_FILTER.md` for the V5 migration, private media API, permission model, upgrade steps and manual regression checklist.
