> **Authentication integration candidate:** See `FIREBASE_BREVO_CUTOVER_GUIDE.md` before enabling Firebase.
> Sole designated Superuser: `inl2group13@gmail.com`, linked ONLY through trusted database bootstrap with verified Firebase UID.
> Database V7 is non-destructive. Package needs full dependency build and runtime validation.

## New in this M3 package: Google Maps issue location

Requesters can use Google Places (New) address suggestions, a draggable issue pin, or permission-based current location. Staff, Managers and Requesters can view the saved exact coordinates and map in Request Details. Requires an HTTP-referrer-restricted Google Maps browser API key and enabled billing. **Read [`GOOGLE_MAPS_LOCATION_UPGRADE.md`](GOOGLE_MAPS_LOCATION_UPGRADE.md) before upgrading an existing database.** This adds Flyway V6; preserve your existing PostgreSQL volume and `.env` passwords.

## M3 update: request photos, comment photos, Manager role filtering

This complete-source package includes optional JPG/PNG photo uploads on new requests, private photo retrieval in Requester/Staff/Manager request details, images with Staff/Manager comments and status updates, and User Administration role filtering for Managers. The additive Flyway V5 migration preserves existing data. **Before upgrading, back up the database** and follow `M3_PHOTOS_AND_ROLE_FILTER.md` for installation, security behavior and the acceptance checklist. Java integration/database migrations and full live end-to-end tests are still pending in the recipient environment.

# Updated authentication and GUI

Start with [SETUP.md](SETUP.md) for the database accounts, unified login, manager provisioning, and validation notes. The milestone notes below describe the original baseline; their development-role accounts are superseded.

# CivicConnect — Milestone 3 controlled release-candidate snapshot

**Status: candidate source packaged; full staging/CI run not executed in this preparation environment.**
This upgrades the submitted Milestone 2 Java/Spring Boot + React/TypeScript + PostgreSQL modular-monolith prototype rather than replacing it. No GitHub repository, branch protections, pull requests, or CI run has been changed remotely.

## Core functionality
- Requester submits validated, categorised requests, views their own requests and status/history feedback.
- Staff queue and per-request detail; accept ownership once, enforce owner-only transitions, optimistic request-version check; require reason when rejecting/resolving/closing.
- Manager status counts, category/status breakdown, and actor-aware audit timeline.
- Role-based API access; staff actor IDs are omitted from requester feedback.
- PostgreSQL Flyway migrations and transactional lifecycle audit writes; post-commit secondary notification listener.
- React role flows, searching/category/status filters, chronology sorting, timeline view and on-screen form validation.
- Local simulated staging with a version identification endpoint, repeatable live verification, and a small management-dashboard load exercise.

**Known scope gap:** exact catalogue and status rules need stakeholder confirmation under original assumption **A-005**; overdue classification is not implemented until that rule is approved. Basic auth is for isolated staging/demo only, **not production**.

## Quick start: assessor-accessible staging simulation
Prerequisites: Docker Engine + Compose v2; images require network access during first build.

```bash
python3 scripts/setup_staging.py
docker compose --env-file .env.staging -f compose.staging.yaml up -d --build
# wait until http://localhost:8082/actuator/health is healthy
curl http://localhost:8082/api/release
python3 scripts/smoke.py --base-url http://localhost:8082
python3 scripts/performance.py --base-url http://localhost:8082
```

- Browser: **http://localhost:8081**; API at **http://localhost:8082** (bound to loopback).
- Local credential values are in the generated **`.env.staging`**. Never commit/upload it. You can use the test role selector in the browser and enter the matching credentials.
- Staging is separate from development and uses a separate PostgreSQL volume and Spring profile.
- Save **CI run URL**, **exact Git commit/tag**, **actual smoke/performance output**, and approval evidence only after the team runs/verifies them.
- Stop: `docker compose --env-file .env.staging -f compose.staging.yaml down` (retains data). For a disposable full reset only, run `... down -v` (deletes staging DB volume).

## Normal development
```bash
# PostgreSQL on localhost:5433 via the original M2 compose.yaml
cp .env.example .env  # change all passwords locally
docker compose up -d postgres
cd backend && mvn -B verify && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# from another terminal
cd frontend && npm ci && npm test && npm run dev
```
Uses **Java 25**, **Maven 3.9+**, **Node 24**, **PostgreSQL 18.6** per M2 architecture decision. Development credentials are supplied via environment, not source. Production must not use in-memory Basic credentials.

## Source layout
- `backend/` and `frontend/`: evolved M2 prototype.
- `.github/workflows/ci.yml`: PR/push gates (untested remotely until connected to GitHub).
- `scripts/smoke.py`: live 6 API + 2 system E2E scenarios (needs staging).
- `scripts/performance.py`: repeatable 40-request/4-worker load exercise (needs staging).
- `scripts/check_source.py`: local limited static/security scan; run with Python 3.
- `docs/m3/`: M3 evidence map, test matrix, staging plan, release evidence summary and demo/defence guide.
- `docs/registers/`: continued RTM derived from the M2 PED entries and milestone registers.
- `PED_v3.0_CivicConnect.pdf`: the original M2 PED pages retained in the expanded M3 document (evidence index and addendum).
- `GITHUB_AFTER_READY.md`: human GitHub governance, review, and submission steps.

## Important assessment rules
M3 requires **authentic** progressive commits, team-owned PRs with **two non-author approvals**, passing CI, staging demonstration, verified performance and tests, current RTM and PED. This ZIP prepares work for your review; it cannot manufacture historic repository activity or evidence of runs that did not happen. Do **not** submit the ZIP to Moodle in place of the PED/URL/tag unless your lecturer explicitly asks; the ZIP is a transfer package.

See `docs/m3/release-evidence-summary.md` before making a readiness claim.
