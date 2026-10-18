# Google Maps implementation: verification record

**Scope**: CivicConnect M3 source upgrade, not a declaration that Google Cloud resources have been configured.

| Check | Result | Evidence / follow-up |
|---|---|---|
| Frontend TypeScript project references | PASS (local) | `node node_modules/typescript/bin/tsc -b` (using an installed dependency tree available in the authoring environment) |
| Frontend Google Maps/Places UI wiring | Source reviewed | `frontend/src/LocationPicker.tsx`, `frontend/src/App.tsx`, `frontend/src/app.css` |
| Google key injection | Source reviewed | `compose.yaml` build args, `frontend/Dockerfile`, `.env.example` (no real key included) |
| Persistent coordinates | Source reviewed | `CreateRequestCommand`, `ServiceRequest`, `RequestView`, `RequestService`, V6 migration |
| Java coordinate unit tests | ADDED, NOT EXECUTED | `backend/src/test/java/com/civicconnect/request/application/RequestCoordinatesTest.java` |
| Frontend coordinate unit tests | ADDED, NOT EXECUTED | `frontend/src/location.test.ts` |
| Full frontend Vite production build | NOT EXECUTED | Local toolchain contains Windows-only Rolldown native binding; run `npm ci && npm run build` in Docker/Windows |
| Full backend Maven build/tests | NOT EXECUTED | Java 25 and Maven not available in authoring runtime. Docker build uses JDK 25; separately run `mvn test` in suitable environment |
| Flyway V6 upgrade on existing PostgreSQL | NOT EXECUTED | Back up existing DB; run Docker Compose and inspect API migration logs |
| Real Google autocomplete/maps/geolocation | NOT EXECUTED | Requires your restricted Maps JavaScript API key, Places API (New), billing, and browser permissions |
| Role visibility & regression workflows | NOT EXECUTED | Run full checklist in `GOOGLE_MAPS_LOCATION_UPGRADE.md` |

Do not mark the M3 release as verified until the pending stages have been performed on the team machine and results recorded in the PED v3.0 evidence register.
