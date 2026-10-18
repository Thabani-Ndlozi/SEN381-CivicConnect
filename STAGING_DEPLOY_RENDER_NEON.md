# CivicConnect — no-cost staging deployment candidate (Render + Neon)

**This is a deployment candidate, not a live deployment.** Do not share a public URL or invite the team until the tests at the end pass. Your existing Windows Docker Compose setup and PostgreSQL volume remain untouched.

## Architecture and limits

- Render **Static Site (Free)** serves the Vite React build from a HTTPS `*.onrender.com` URL.
- Render **Web Service (Free)** runs the Java 25 / Spring Boot 4 API as a Docker container, on a different HTTPS URL.
- Neon **Free** hosts a separate PostgreSQL database. This package adds Flyway V8 to grant minimum table privileges to the restricted runtime role; all other migrations remain.
- Existing Firebase project `civicconnect-3ed04` and Brevo account are reused, with independent staging database. Use demo data only.
- Free Render API: 512 MB RAM / 0.1 CPU and idle spin-down after 15 minutes; Java cold starts can take a while or run out of memory. This **has not yet been tested** on free Render. Background Brevo notification processing pauses while the API is asleep and resumes on wake.
- Neon Free storage is limited (0.5 GB in its published Free tier); CivicConnect stores photos in Postgres, so keep photos small and monitor capacity. Back up any demo data you want to keep.
- Render **Free PostgreSQL expires after 30 days**; do not use it for a semester-long test database.
- Brevo API-key IP blocking: the Render Free API does **not** guarantee a fixed outbound address. Your existing allowed home IP does not automatically authorize Render. For staging you may need to **temporarily disable Brevo API-key IP allowlisting** (risk: stolen key usable from any IP). Keep API key private, rotate on exposure, and reinstate an allowlist once you have a fixed egress IP in a future paid production host.

## Before you touch hosting

1. Keep `civicconnect_local` and its named PostgreSQL Docker volume unchanged. Backup via `pg_dump` if you have not already done so.
2. Do not upload `.env`, `secrets/firebase-service-account.json`, passwords, Brevo keys, backups, or any other private file to a repository. Review `.gitignore`. Use a NEW PRIVATE GitHub repo for staging and push **the contents** of this `CivicConnect_Milestone3_Release` folder to its repository root, so `render.yaml` is at the root.
3. This ZIP deliberately contains no private credentials and no local database dump. Avoid using a dump of real service requests; new staging DB should contain test records only.

## 1. Neon Free database / SQL permissions (do first)

1. Create a Neon Free project (new database, e.g. `civicconnect_staging`), choose the same geographic region as the Render API if available (Render regions include Frankfurt, Oregon, etc.). Confirm Neon PostgreSQL version supports these migrations.
2. In Neon **SQL Editor**, use the admin database role and follow `database/NEON_STAGING_FIRST_BOOT.sql.example` **FIRST** section, substituting strong distinct passwords only inside Neon. The two roles MUST be created through SQL, not through Neon Console 'Add Role' because console-created roles inherit `neon_superuser` and could bypass row-level security.
3. From Neon **Connect**, copy the **direct** Postgres host (avoid `-pooler` for Flyway and transaction-local RLS). Use TLS `sslmode=require`. Set Render API env `DB_URL` and both role passwords with that host, database and SSL. Example format only: `jdbc:postgresql://<direct-neon-host>:5432/<staging_db>?sslmode=require`.
4. Check user credentials and least privileges before deploying; runtime is `civicconnect_runtime`, migrations `civicconnect_migrator`. They must not inherit `neon_superuser` or have BYPASSRLS.

## 2. Render: connect repo and create services

1. Create a Render account (Free). Connect the new private GitHub repository; choose **New → Blueprint** and select `render.yaml` from the repo root. If Render asks for free vs paid, ONLY approve free instances. There are **no Render databases** in this blueprint.
2. It defines `civicconnect-api-staging` (Docker web service) and `civicconnect-web-staging` (static site). Names may be changed to avoid collisions; actual service URLs shown by Render are authoritative. You can also configure them manually from the same file if needed.
3. Set secret/config env values requested during Blueprint setup, **without committing any values to git**:
   - API `DB_URL` is JDBC direct Neon URL, `DB_PASSWORD` and `MIGRATION_DB_PASSWORD` are the two new SQL-created roles' passwords.
   - API `BREVO_API_KEY` from Brevo, `STAGING_WEB_ORIGIN` is **exact** deployed frontend HTTPS origin (no path), `PUBLIC_APP_URL` is the same frontend URL, `CIVICCONNECT_LOGO_URL` is your public HTTPS raw PNG link. If URL is unknown until deploy, set temporary non-secret example origin and update after Render allocates final URL.
   - Static `VITE_API_BASE_URL` is the **exact** deployed Render API HTTPS URL (no trailing `/api`); e.g. `https://<your-api>.onrender.com`.
   - Static `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_MESSAGING_SENDER_ID` come from your Firebase registered web app; `VITE_GOOGLE_MAPS_API_KEY` and `VITE_GOOGLE_MAPS_MAP_ID` come from your Maps Cloud project.
   - API service **Secret File**: filename `firebase-service-account.json`; paste file contents privately via Render Environment → Secret Files. Render mounts it at `/etc/secrets/firebase-service-account.json`. `GOOGLE_APPLICATION_CREDENTIALS` in blueprint already points there. Do NOT put that file in the repo or Vite env.
   - `AUTH_MODE=firebase`; start with `BREVO_NOTIFICATIONS_ENABLED=false`. Invites are handled separately; enable status emails after staging works.
4. Render free 512 MB is tight for Java 25; the blueprint sets conservative `JAVA_TOOL_OPTIONS` and Hikari pool 3. If health checks fail with OOM / very slow startup, this host may be unsuitable for the full API at zero cost. **Do not assume success or choose paid without discussing it first.**
5. Watch logs for `CivicConnect Firebase SecurityFilterChain active`, `Successfully validated 8 migrations` (after initial migration it may say applied 8), `Started CivicConnectApplication`, and `/actuator/health` status UP. The HTTP transport fix that resolved Firebase verification on Windows is preserved (`NetHttpTransport`).

## 3. One-time staging Superuser bootstrap (after API/Flyway V8)

1. In Firebase Console → Authentication → Users find **the existing verified** `inl2group13@gmail.com` Email/Password user. Copy UID privately.
2. In **Neon SQL Editor**, as trusted database administrator, use the SECOND section of `database/NEON_STAGING_FIRST_BOOT.sql.example` with that verified UID. **Never run as runtime role.** Do not make Superuser via a public registration route.
3. Confirm unique `SUPERUSER` and that `firebase_uid IS NOT NULL`. Separately validate runtime `rolbypassrls=false` and `rolsuper=false` via the THIRD section. Sign in with your existing Firebase credentials on the staging URL.
4. Staging uses the same Firebase Auth project, so Firebase identities exist already, but database roles and requests are separate. Existing local Staff/Manager will NOT automatically have rows in staging; invite staging-only internal accounts after testing. Do not recreate the Firebase UID with the same email in the same Firebase project.

## 4. Configure public Google/Firebase hostnames

- Firebase Console → Authentication → Settings → Authorized domains: add **exact** Render frontend domain without `https://` or path (e.g., `<your-web>.onrender.com`). Keep `localhost` for local testing. Pop-up Google sign-in remains in this source.
- Google Maps API key: in Google Cloud → Credentials, allow the new **HTTPS frontend referrer** and restrict permitted Maps JavaScript / Places API (New) / Geocoding API per your existing application needs. Keep localhost referrer for local development. Note Maps has separate billing from free staging hosts.
- Static site `VITE_*` environment variables are baked into JavaScript **at build time**. Update them and redeploy the static site when the API URL or Firebase app config changes.
- Set `STAGING_WEB_ORIGIN` to the exact new web origin on the API so CORS allows `Authorization` and `Content-Type` from the static host. No CORS wildcard is used. If the browser shows a CORS issue, recheck this exact URL and remove trailing slash.
- `PUBLIC_APP_URL` for Brevo invitation and status links must point to your new **HTTPS web** URL, never `localhost`.

## 5. End-to-end staging acceptance test (before sharing)

- API health `https://<api-url>/actuator/health` returns HTTP 200, status UP; free-tier cold start can delay this. If the web loads but the first API call fails, retry after API wakes.
- Login with verified Superuser. Ensure role is SUPERUSER; cannot access other user's Requester features through role bypass.
- Add real Staff/Manager test accounts (use email inboxes you control); resend invitation; verify both action links and delivery.
- Register a Requester via Google and email/password; submit request with map location and a small photo; check upload/read and comments.
- Staff self-accepts a submitted request; Manager reviews; do not introduce Manager assignment capability.
- Check staff/requester role separation and RLS, including attempts to access other Requesters' requests/images.
- Enable `BREVO_NOTIFICATIONS_ENABLED=true` only after testing; check Brevo sender, logs and notification_outbox. On Free Render, outbox sends may be deferred while the API sleeps.
- Inspect logs for OOM/health failures and Neon storage / connection quotas. Test after 20+ minutes idle. Keep your Windows local installation as fallback.

## Costs and data policy

Render Free web service (0.1 CPU, 512 MB) and static site + Neon Free may cost $0 under included limits, **but limits and plan eligibility change** and there is **no uptime guarantee**. Firebase Spark Auth and Brevo Free remain as configured; Google Maps may incur usage charges on your already-billed Google Cloud project. With no fixed Render egress IP, Brevo allowlisting is an operational tradeoff, not a code defect. Never use this setup as a production public service before review of IAM, secrets, backups, abuse/rate limits and app performance.

Official docs to verify limits: https://render.com/docs/free , https://render.com/docs/compute-plans , https://neon.com/pricing , https://neon.com/docs/manage/roles , https://render.com/docs/configure-environment-variables .
