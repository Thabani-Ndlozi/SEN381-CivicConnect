# Firebase activation returning an empty HTTP 403: focused security-chain fix

## Observed diagnostics
- `AUTH_MODE=firebase` inside the API container.
- `POST http://localhost:8085/api/auth/firebase/activate` without an Authorization header returned `403 Forbidden` with `Content-Length: 0`.
- The expected activation behavior for a missing header is `401 Unauthorized` (with the controller's JSON error body).
- PostgreSQL and Flyway V7 startup succeeded; no database reset or account re-bootstrap is needed.

## Changes in this candidate
- In `backend/src/main/java/com/civicconnect/security/FirebaseSecurityConfig.java`, allow the servlet **ERROR dispatch** and `/error` to preserve the original 401/404 responses. Previously a secondary `/error` dispatch could be evaluated by `.anyRequest().authenticated()` and overwrite an initial error with an empty 403.
- Add explicit JSON replies for unauthenticated and access-denied requests.
- Log one clear startup marker when the Firebase `SecurityFilterChain` is configured.
- Preserve the earlier fix that builds the Firebase bearer filter *inside* the Spring Security chain, rather than registering it as a servlet-filter bean as well.
- Preserve all previous Firebase verification, frontend, Brevo, and Flyway migration work.

**Important:** The empty-403 symptom is consistent with secondary error dispatch blocking but does not alone prove that is the sole cause. Test the actual Docker build; if it persists, investigate the active security chain instead of changing user accounts.

## Minimal installation on your existing Windows project
1. Extract this ZIP to a separate location.
2. Copy **only** `backend/src/main/java/com/civicconnect/security/FirebaseSecurityConfig.java` from the extracted ZIP into the corresponding location in your existing project.
3. Save it. Keep `.env`, `compose.yaml`, `secrets`, and PostgreSQL volumes unchanged.
4. From your existing project folder, rebuild only the API:

   ```powershell
   docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build --force-recreate api
   ```

5. Check the startup marker:

   ```powershell
   docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --tail=100 api
   ```

   Expected: `CivicConnect Firebase SecurityFilterChain active (auth mode: firebase)`.

6. Call the endpoint **without credentials**:

   ```powershell
   curl.exe -i -X POST "http://localhost:8085/api/auth/firebase/activate" -H "Content-Type: application/json" --data-raw "{}"
   ```

   Expected: `401 Unauthorized` and `{"message":"Missing Firebase ID token"}` (or another explicit authentication error).

7. Sign in at http://localhost:8085 as `inl2group13@gmail.com`. If a different message appears, inspect the *Response* tab for `activate`. Do not share passwords, bearer tokens, service-account JSON, or Brevo API keys.

If the unauthenticated curl command **still returns a blank 403**, verify that the new backend class compiled and startup marker appeared. Also capture the output of:

   ```powershell
   curl.exe -i http://localhost:8085/api/this-route-does-not-exist
   ```

A properly preserved API error should not be converted into an empty 403. You may temporarily enable Spring Security DEBUG logging for deeper analysis, but **never share Authorization headers or security credentials**.

Leave `BREVO_NOTIFICATIONS_ENABLED=false` until login and request workflows pass.
Never use `docker compose down -v` as part of this fix.

## Verification status
Source change and ZIP integrity checked offline. The complete Docker/Maven build and Firebase sign-in have not been verified in this environment.
