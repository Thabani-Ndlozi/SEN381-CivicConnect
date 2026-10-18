# CivicConnect M3 — Google Maps issue-location upgrade

## What has been implemented

This release extends the **previous M3 photo/comment/role-filter release**. It does **not** replace the existing authentication, image uploads, role management, or request history.

* **Requester → New Request:** Google Places (New) address autocomplete while typing, Google Map preview, click-to-pin, draggable pin, and **Use my current location** (permission required). Choosing a suggestion places the pin automatically. Dragging or clicking attempts reverse geocoding and retains the exact coordinates even if address lookup fails. The selected pin must be confirmed before submission **when Google Maps is configured**.
* **Backend:** persist request `location` (address), `latitude` and `longitude` (WGS84) in a single creation operation; reject incomplete/out-of-bounds coordinate pairs.
* **Requester / Staff / Manager → Request Details:** read-only Google Map with the saved issue pin, coordinates, and **Open location in Google Maps** link. Requests submitted before this release display **No exact map pin was recorded** instead of an invented location.
* **Fallback:** if no Google key is configured, the manual address input remains usable for local development, without saved coordinates. Google location functionality requires a working key and enabled billing.

## Configure Google Maps (required to see the interactive map)

1. Open [Google Cloud Console](https://console.cloud.google.com/) and choose/create a project. Enable **billing** for the project; Google Maps is not available without it.
2. Enable **Maps JavaScript API** and **Places API (New)**. The frontend uses the new `AutocompleteSuggestion.fetchAutocompleteSuggestions` API, not deprecated legacy autocomplete. The pin uses the Maps JavaScript **AdvancedMarkerElement**. Reverse geocoding is handled by the Maps JavaScript API.
3. Create a browser API key in **APIs & Services → Credentials**. Restrict it to **Websites (HTTP referrers)**. Add the website origins that *you actually use*, for example:
   * `http://localhost:8085/*` (your existing local front end)
   * `http://localhost:8081/*` (ZIP default)
   * `http://localhost:5173/*` (Vite dev)
   * your real deployment's HTTPS origin, once known.
4. Restrict the key to the **Maps JavaScript API** and **Places API (New)**. Configure billing budgets, quotas, and monitoring to control usage.
5. Create a **Map ID** in Google Cloud's Maps Management section if deploying beyond local testing. `DEMO_MAP_ID` is provided only as a local development placeholder for Advanced Markers.
6. In the **new project folder**, copy your *existing* `.env` file (retain PostgreSQL passwords) and add these lines to the end:

```dotenv
# Google Maps browser key — intentionally excluded from the ZIP
GOOGLE_MAPS_API_KEY=PUT_YOUR_RESTRICTED_BROWSER_KEY_HERE
GOOGLE_MAPS_MAP_ID=DEMO_MAP_ID
```

**Never put a server account credential or unrestricted key here.** Unlike a backend secret, a browser API key will be present in the built JavaScript and visible in the browser; **referrer and API restrictions are mandatory**. Keep the `.env` out of GitHub. **Do not paste your key into this chat.**

This version passes the browser key through a Vite build argument. Changing it requires rebuilding **web**, not merely restarting a running container.

## Safely upgrade your existing Windows Docker installation

You previously used Compose project name `civicconnect_local`, with website `localhost:8085`, API `localhost:8084`, and PostgreSQL host port `5434`. Those ports are modifications you made locally: **they are not the defaults in this ZIP**. Before starting, open `compose.yaml` in the new extracted folder and retain the **three host port mappings from your working installation**:

```yaml
# postgres service
ports:
  - "5434:5432"
# api service
ports:
  - "127.0.0.1:8084:8080"
# web service
ports:
  - "127.0.0.1:8085:80"
```

1. Extract this ZIP in a *new* directory. Open `CivicConnect_Milestone3_Release` in VS Code.
2. Copy the `.env` **from the working installation**. Do not generate new database passwords. Add the Google configuration above.
3. Before the upgrade, back up PostgreSQL using the existing running project. The following commands use the new directory only if `.env` contains the same database credentials:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml exec postgres pg_dump -U postgres -d civicconnect -Fc -f /tmp/pre_maps_v6.dump
docker compose -p civicconnect_local --env-file .env -f compose.yaml cp postgres:/tmp/pre_maps_v6.dump .\pre_maps_v6.dump
```

4. Verify config then rebuild **both** API and web (the browser key is compiled into the web bundle):

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml config --quiet
docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build api web
docker compose -p civicconnect_local --env-file .env -f compose.yaml ps
docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --tail=100 api
```

5. Check `http://localhost:8084/actuator/health` (expected `{"status":"UP"}`), then open `http://localhost:8085` and refresh with `Ctrl+F5`. If the site shows `502 Bad Gateway`, the API likely did not start. Check the API logs.
6. Flyway **V6__request_coordinates.sql** should run once, after V5, and preserve previous requests and users. If you see `FATAL: password authentication failed for user "civicconnect_runtime"`, verify that `DB_PASSWORD` matches the **existing PostgreSQL runtime role's password**. Do not delete database volumes. If your old data is important, do not run `docker compose down -v`.

## Acceptance-test checklist (complete these before marking M3 release ready)

### Requester

- [ ] Type at least three letters of a valid address and receive Google suggestions (with key configured).
- [ ] Choose a suggestion: input fills, map zooms, and confirmed coordinates appear.
- [ ] Drag marker to a nearby location; coordinates change; reverse-geocoded label updates when available.
- [ ] Click map elsewhere; pin relocates to the clicked spot.
- [ ] Select **Use my current location**, grant permission, verify current device position, and adjust it to the **issue's** location if needed.
- [ ] Deny geolocation permission: clear message appears; search and map-click still work.
- [ ] Type another address after selection: previous pin is cleared and the location is no longer considered confirmed.
- [ ] Try submitting without a pin while Maps is configured: frontend blocks submission with a useful message.
- [ ] Submit with confirmed pin **and photo**, verify both are saved; check address and coordinates in Request Details.
- [ ] Submit a second request without an image; location still persists correctly.
- [ ] Test narrow viewport/mobile keyboard navigation and suggestion list.

### Staff and Manager

- [ ] Staff can open new request, see **the same coordinates** and read-only map pin.
- [ ] Manager can open the request and see **the same coordinates** and pin.
- [ ] **Open location in Google Maps** opens the saved coordinates in a new tab.
- [ ] Older requests without V6 coordinates show the address and **No exact map pin was recorded**.
- [ ] Existing Staff status and Manager reporting, role filtering, request-photo and comment-photo flows are unaffected.
- [ ] One Requester cannot retrieve another Requester's request; unauthenticated access is denied (existing RLS/auth controls).

### Backend and operations

- [ ] V6 migration applies to existing V5 database with no data loss.
- [ ] API create accepts a valid `{location,latitude,longitude}` pair and returns it unchanged in response and subsequent reads.
- [ ] API rejects one-coordinate-only request with `400`.
- [ ] API rejects latitude outside `[-90,90]` or longitude outside `[-180,180]` with `400`.
- [ ] Legacy creation without coordinates remains compatible while manual fallback is enabled.
- [ ] Browser key only authorizes approved sites/APIs. Review quota/billing alerts.
- [ ] Run automated test suites and save screenshots/logs for PED v3.0 and CI evidence.

## Known limitations and deliberate design choices

* Google's autocomplete and maps require working external Google services, a billable project, configured APIs, a restricted key, and internet connectivity. **No live Google session was run in this build environment**, because no Google key was provided.
* This is a civic issue location, not a dispatch/navigation service; Requesters should confirm the **issue's** location, not blindly use their current GPS position. Browser GPS is permission-based and may be inaccurate indoors.
* This release stores only the address and WGS84 coordinates, not Google's Place ID or geometry history. Updates to previously submitted request coordinates are intentionally not allowed in this change.
* The stored pin is only available to users already authorized to view the request. Maps tiles and autocomplete queries are requested directly from Google by the browser and are subject to Google's terms and privacy notices. Add a clear location/third-party service privacy disclosure before public launch.
* Manager-wide heatmaps, geographic filtering, route optimization, and geofencing are **not** part of this scope.

## Official technical references

* [Google Places Autocomplete Data API](https://developers.google.com/maps/documentation/javascript/place-autocomplete-data)
* [Maps JavaScript API loader](https://developers.google.com/maps/documentation/javascript/load-maps-js-api)
* [Advanced Markers](https://developers.google.com/maps/documentation/javascript/reference/advanced-markers)
* [Google Maps API key security](https://developers.google.com/maps/api-security-best-practices)
