# Firebase activation: HTTP 403 diagnosis

This candidate fixes **double registration of FirebaseBearerFilter**: it was
both a Spring Boot servlet-filter bean and a Spring Security chain filter.
Now it runs *only* inside the Spring Security chain. This removes an
independent authentication-context bug, but the actual cause of every 403
cannot be proven without the runtime diagnostics below.

## Apply only this code change to your existing working folder

Copy `backend/src/main/java/com/civicconnect/security/FirebaseSecurityConfig.java`
from the updated ZIP into the same relative path of your working project.
Keep `.env`, `compose.yaml`, `secrets`, uploaded files and the PostgreSQL
volume unchanged. Never use `docker compose down -v`.

Then rebuild the API only:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build --force-recreate api
```

Inspect the mode:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml exec api printenv AUTH_MODE
```

It should print `firebase`.

Probe the activation route **without sending any credentials**:

```powershell
curl.exe -i -X POST http://localhost:8085/api/auth/firebase/activate -H "Content-Type: application/json" --data-raw "{}"
```

Because this route is deliberately `permitAll` but its controller requires
an explicit Bearer token, the expected status for this probe is **401**
with `Missing Firebase ID token` in a JSON response. If the result is still
**403 with an empty body**, Spring Security is likely rejecting the request
before it reaches the controller, or the intended API build is not running.

Try the Firebase login again. If activation fails, inspect
**DevTools > Network > activate > Response** and collect redacted logs:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --tail=100 api
```

Do not share Firebase tokens, Authorization headers, passwords, `.env` or
service-account JSON. Keep Brevo notifications disabled until login works.

This source patch was statically inspected, but was **not** compiled with Maven
or tested against your live Firebase/PostgreSQL services in this environment.
