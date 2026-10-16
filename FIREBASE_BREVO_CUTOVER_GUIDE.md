# CivicConnect Firebase + Brevo upgrade — controlled working candidate

**Designated sole Superuser:** `inl2group13@gmail.com`.

**This is not a tested production release.** The new Firebase/Brevo integration code is staged, but Maven and npm package registries were unavailable in this build environment. The existing JavaScript/TypeScript sources passed an offline syntax/type check *using temporary Firebase type stubs*, not a real Firebase SDK build. Docker, database migration V7, Firebase calls, Brevo delivery and CI still require execution on your team's machine. Do not switch your working system immediately.

## Architecture and safety

- Firebase Auth project `civicconnect-3ed04` is the identity provider.
- Google sign-in and email/password are **only for Requesters**; Staff/Manager/Superuser use email/password and require verified email.
- The backend validates Firebase ID tokens and uses PostgreSQL `app_users.firebase_uid` to determine the role. It never trusts a client-supplied role.
- The `inl2group13@gmail.com` address is **not** automatically promoted merely by signing up. Superuser promotion is a one-time DBA task requiring a Firebase Console-verified UID.
- PostgreSQL retains existing emails as the request ownership keys and preserves requests, photos, locations, staff ownership and audit events. `V7` adds nullable Firebase UID and a notification outbox.
- Request images and comment images already had previews and Remove actions **before submission**, and those remain intact. They do not support deletion after posting because audit evidence is immutable.
- The Brevo key and Firebase service-account JSON are backend secrets. Do not paste them into chat or commit to GitHub.

## Step 1 — Back up BEFORE upgrading

Open a terminal in the new extracted folder after copying your current **private** `.env` file. Set the same Docker Compose project name and ports you used previously. Example:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml exec postgres pg_dump -U postgres -d civicconnect -Fc -f /tmp/pre-firebase-migration.dump
docker compose -p civicconnect_local --env-file .env -f compose.yaml cp postgres:/tmp/pre-firebase-migration.dump .\pre-firebase-migration.dump
```

Verify the backup file exists and is not empty. **Never** use `docker compose down -v` on the existing project.

## Step 2 — Prepare Firebase service-account credentials

Firebase Console → Project settings → Service accounts → Generate new private key, or use an existing appropriately scoped service account. Download the JSON *to your machine*. Place it at:

`CivicConnect_Milestone3_Release/secrets/firebase-service-account.json`

The folder is mounted read-only to `/run/civic-secrets` in the API container. This file must stay private and outside version control; the ZIP contains only a `.gitkeep` placeholder. Use the least privileged IAM permissions feasible for token verification and managed user invitation operations.

In Firebase Authentication → Settings → Authorized domains, confirm `localhost` is present. Email/Password and Google providers should remain enabled.

## Step 3 — Add local variables to `.env`

Preserve your existing `POSTGRES_PASSWORD`, `DB_PASSWORD`, `MIGRATION_DB_PASSWORD`, `GOOGLE_MAPS_API_KEY`, and `GOOGLE_MAPS_MAP_ID` unchanged.

```dotenv
# Keep LEGACY during migration and first build; change to firebase ONLY at cutover.
AUTH_MODE=legacy
FIREBASE_PROJECT_ID=civicconnect-3ed04
FIREBASE_WEB_API_KEY=<Firebase Web App config apiKey>
FIREBASE_WEB_APP_ID=<Firebase Web App config appId>
FIREBASE_AUTH_DOMAIN=civicconnect-3ed04.firebaseapp.com
FIREBASE_MESSAGING_SENDER_ID=<Firebase Web App config messagingSenderId>
BREVO_API_KEY=<your Brevo REST API key, backend only>
BREVO_SENDER_NAME=CivicConnect
BREVO_SENDER_EMAIL=inl2group13@gmail.com
BREVO_REPLY_TO=inl2group13@gmail.com
PUBLIC_APP_URL=http://localhost:8085
BREVO_NOTIFICATIONS_ENABLED=false
# Optional hosted HTTPS image (email clients cannot fetch localhost)
CIVICCONNECT_LOGO_URL=
```

The Firebase **web API key is publicly visible** in client JavaScript, as intended by Firebase, and must not be confused with the **private admin service-account JSON** or Brevo REST API key.

## Step 4 — Build the migration candidate and inspect V7

Update the `compose.yaml` port mappings if using frontend `8085`, backend `8084` and PostgreSQL `5434` as before. They still default to `8081/8082/5433` in the package. Run:

```powershell
cd frontend
npm install
npm run build
npm test
cd ..
docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build api web
docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --tail=120 api
```

**The frontend package-lock file cannot be refreshed in this offline authoring environment.** Running `npm install` on an internet-connected machine updates the lock. Inspect/test the resulting dependency versions and **commit the refreshed lock** before CI/submission. Do not treat this as a verified build until Docker/Maven runs successfully.

Flyway V7 should add a nullable `firebase_uid`, the SUPERUSER role and `notification_outbox`. Verify that it does not delete any of your existing data. Leave `AUTH_MODE=legacy` until identity migration is ready.

## Step 5 — Create/verify the Firebase Superuser identity

1. Create an email/password Firebase Authentication user `inl2group13@gmail.com` in the Firebase Console (or through the signup UI when firebase mode is activated).
2. Verify ownership of that email with the Firebase verification email.
3. In Firebase Console → Authentication → Users, copy the **UID** for exactly that verified email.
4. As a trusted DBA, execute `database/bootstrap_superuser.sql` from within PostgreSQL using psql. The script prompts for the Firebase UID; do not substitute an email as UID. If the account already exists in PostgreSQL it will preserve request associations and promote the account; if a Superuser already exists it aborts.

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml cp .\database\bootstrap_superuser.sql postgres:/tmp/bootstrap_superuser.sql
docker compose -p civicconnect_local --env-file .env -f compose.yaml exec postgres psql -U postgres -d civicconnect -f /tmp/bootstrap_superuser.sql
```

**Never expose an API route to make someone a Superuser by matching `inl2group13@gmail.com`.** Check the database row contains the verified UID and role `SUPERUSER`.

## Step 6 — Link existing Staff/Manager/Requester accounts

For **each** existing account you wish to preserve, create/migrate a Firebase identity with the same verified email. Look up the Firebase UID and verify the email in the Firebase Console, then run `database/link_existing_firebase_user.sql` using the DBA role. The script prompts for email and verified UID. An existing email with missing UID **cannot** self-link through `/api/auth/firebase/activate`: this is deliberate account-takeover prevention.

Existing password hashes are left in PostgreSQL for the legacy rollback window; do not expose them. Once every necessary account is migrated and verified, retire legacy authentication and remove hashes in a separate hardening migration.

## Step 7 — Enable Firebase mode and test

Set `AUTH_MODE=firebase`. Leave `BREVO_NOTIFICATIONS_ENABLED=false` until delivery tests pass. Rebuild **both** API and frontend because `VITE_AUTH_MODE` and Firebase web config are compiled into the frontend:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build --force-recreate api web
docker compose -p civicconnect_local --env-file .env -f compose.yaml ps
docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --tail=100 api
```

Test login as Superuser, then invite a Staff and a Manager from User Administration. The admin backend provisions Firebase identities and attempts to send **two action links** in a Brevo invitation: verify the email and set the password. It does not let users select or grant their own elevated role. If the invitation reports unsuccessful sending, use the **resend invitation** API or check Brevo configuration; don't create duplicate accounts. This route exists at `POST /api/admin/users/{email}/resend-invitation` for authenticated Superusers.

Firebase password reset works through the **Forgot password?** screen. Google login must be rejected for internal roles by the backend, even if a Google account happens to have the same address.

## Step 8 — Email notifications

- Brevo verified sender: CivicConnect `<inl2group13@gmail.com>`; reply-to also Gmail. Brevo may rewrite sender address because Gmail is a freemail domain.
- For best branding, host the CivicConnect logo on a public HTTPS URL and set `CIVICCONNECT_LOGO_URL`; otherwise the template has a styled CivicConnect text wordmark.
- The request status event is recorded in the notification outbox after commit. A scheduled dispatcher attempts to send through Brevo REST API, records `sent_at` on success, and retries failures with backoff.
- Set `BREVO_NOTIFICATIONS_ENABLED=true` **only after** checking verified sender/recipient delivery and outbox records. Status updates remain successful even if email sending is temporarily unavailable.
- Delivery is **at least once**; duplicates are possible if Brevo accepts a message but the client times out. Do not claim exactly-once delivery without a provider-level idempotency mechanism.
- Inspect notification attempts safely via trusted DBA query: `SELECT request_id,next_status,attempts,sent_at,last_error FROM notification_outbox ORDER BY created_at DESC LIMIT 15;`

## Regression and security acceptance checklist (record Pass/Fail with evidence)

- [ ] Current requests, staff ownership, coordinates, media and audit history unchanged by V7.
- [ ] Requester registers with email/password → receives verification → can sign in only after verifying.
- [ ] Requester Google sign-in creates a profile with role REQUESTER only.
- [ ] Fake/expired/revoked Firebase Bearer token yields 401/403 and no data access.
- [ ] An existing unlinked email is rejected; no automatic account linking.
- [ ] Superuser login works only after UID-verified DBA bootstrap.
- [ ] Additional Superuser role insertion rejected by database single-superuser constraint.
- [ ] Requester cannot access Staff, Manager or Superuser endpoints.
- [ ] Staff can accept/update own requests, not another staff member's owned requests.
- [ ] Manager can view summary, reports and requests; cannot invite internal accounts in Firebase mode.
- [ ] Superuser can invite Staff/Manager, deactivate accounts and resend invitations.
- [ ] Staff and Manager must verify email and set password before first login.
- [ ] Google sign-in is rejected for Staff/Manager/Superuser backend roles.
- [ ] Requester, Staff and Manager password-reset emails are received; revoked/disabled users cannot access API.
- [ ] Request and comment attachments show previews, reject invalid files, and allow removal *before* posting.
- [ ] Previously posted attachments remain visible to authorized users and hidden from unauthorized Requesters.
- [ ] Request location search, current position, draggable pin and saved map are unchanged.
- [ ] One successful status transition creates one outbox record; no event for failed/stale update.
- [ ] Template displays real status/title/request ID, CivicConnect branding, valid reply-to and working link.
- [ ] Brevo send succeeds, record marked sent; provider outage retries without blocking status update.
- [ ] `localhost` is authorized in Firebase and Google Maps browser key is referrer-restricted.
- [ ] Firebase private service-account JSON, Brevo API key, DB passwords, dump files are absent from ZIP/commits.
- [ ] Full backend Maven tests, frontend tests, production build, API smoke suite, security scans and E2E walkthrough pass on real Docker runtime.

## Known validation gaps

- Firebase SDK and Java Admin dependency versions **not resolved or compiled** in this environment due DNS/network restrictions. Install dependencies and fix any incompatibilities on your laptop before switching AUTH_MODE.
- The lockfile is stale until `npm install` runs with network access. Validate `npm audit` and update CI back to a locked `npm ci` build afterward.
- No real Brevo email was sent from this environment; live sender behavior requires a test in your Brevo account.
- No real Firebase UID or service account JSON is included; never paste or upload those secrets.
- Do not treat this as complete M3 submission evidence until the tests above pass and results are recorded.

## Existing unverified Firebase account: resend link

If Email/Password sign-in is accepted by Firebase but CivicConnect refuses activation, check the account's `emailVerified` value. For a user with `false`, sign in on the CivicConnect login page, select **Resend verification email**, and open the Firebase verification link delivered to that inbox. Then sign in again; this refreshes the Firebase ID token. A password-reset email does **not** verify the address. Do not replace the Firebase UID or rerun the Superuser bootstrap. The resend button is only available in Firebase authentication mode and requires the Firebase user to have signed in. Firebase sending quotas apply.

## Fix: email verified but Superuser activation still 403

This update refreshes the **Firebase ID token** (not just the user profile) immediately
before calling `/api/auth/firebase/activate`. The backend also returns a specific,
sanitized activation error as JSON and the UI displays it instead of always showing
the generic access-denied message.

If an existing account was verified in another browser/tab:
1. Install the updated frontend files and rebuild **web** using your existing Compose
   project name, `.env`, ports and volume. This change does not require a database migration.
2. Open a new private/incognito window, sign in with the verified Firebase email/password.
3. If activation still fails, capture only the HTTP status and `message` response
   from the `activate` request, never the Bearer token or Authorization header.
4. If the message reports administrator linking, compare the Firebase Console UID with
   the PostgreSQL `firebase_uid` **privately**; do not publish either UID.

Never rerun the bootstrap, reset the database, or bypass email verification to solve a login error.
