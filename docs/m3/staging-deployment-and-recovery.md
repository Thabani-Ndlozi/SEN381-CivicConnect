# Simulated staging and deployment control

**Architecture:** separate `staging` Spring profile, Compose `api` + `postgres` + `web` Nginx. All public container ports bind `127.0.0.1`: browser `http://localhost:8081`, backend API/health `http://localhost:8082`. Staging volume is different from dev. Credentials generated with Python `secrets` and written to untracked mode-0600 `.env.staging`; injected via environment. Release label is visible at `/api/release` and must match the actual final Git tag/commit (currently pending).

Commands at repo root:

```bash
python3 scripts/setup_staging.py
docker compose --env-file .env.staging -f compose.staging.yaml up -d --build
curl --fail http://localhost:8082/actuator/health
curl --fail http://localhost:8082/api/release
python3 scripts/smoke.py
python3 scripts/performance.py
```

**Assessor access:** in-person/laptop local demonstration; show browser and requests across requester, staff, manager roles; show generated env keys but **never show password values**. Not a public URL. Do not expose port bindings beyond localhost with Basic auth.

**Promotion check:** use exact commit/tag; update `RC_VERSION` locally to same tag; restart containers; show `/api/release`, `git rev-parse HEAD`, test evidence, dashboard. Never claim a placeholder is a final RC.

**Rollback/recovery:** retain previously vetted Git tag/container image, backups and staging configuration outside repo; stop current stack; switch to last verified tag, rebuild and restart. Database Flyway schema forward migration may make an old application incompatible; **do not auto-rollback database**. Prior to release, take consistent PostgreSQL backup (`pg_dump`) and test restore into an isolated DB, document restore and data-loss risk. The current M3 simulation has **no exercised rollback or recovery proof**. `down -v` deliberately deletes staging data; use only for disposable acceptance data, never real incidents.

**Differences and limitations:** generated in-memory Basic identities, no TLS, local single database/container host, no cloud operations, no public/stakeholder production use, no actual traffic estimate, no approved overdue classification. Desktop may run old Docker/Postgres; clean-build CI will find compatibility problems.
