# CivicConnect M3 – request and comment photos / manager role filter

## What's new

1. **Requester → Submit a Service Request:** optional JPG/PNG image, maximum **5 MiB**. On submit the request and image are inserted together in one database transaction. If the uploaded file is invalid, the request is not created.
2. **Requester → Request Details:** shows the original request photo and all Staff/Manager comment photos. Requester may only view media belonging to their own requests.
3. **Staff → Requests → Details:** shows the Requester photo. The assigned staff member can post a comment with an optional JPG/PNG attachment or attach an image to a status update. Other staff can view the images, but only the assigned staff member can comment.
4. **Manager → Requests → Details:** same photos and comments are visible; Managers can attach photos to comments and status updates for any request.
5. **Manager → User Administration:** an **All roles / Requester / Staff / Manager** filter combines with the existing name/email search and displays the number of matching users. This is a frontend filter over the Manager-only account list, not an account permission change.

## Implementation & security

- Flyway **V5__request_media.sql** creates `request_media` and RLS policies while preserving existing records, users, and request/audit history.
- Image content is held in PostgreSQL `bytea`; the browser retrieves it only through an authenticated, role-restricted endpoint. It is *not* under `frontend/public` or any public asset path.
- JPG and PNG only, ≤5 MiB, image format signature and dimensions checked (maximum 8,000 px per dimension and 24 million pixels). SVG/HTML are not accepted. Only one image per new request or comment/status update.
- Comments with images are stored as immutable request-media entries. They are visible in the common "Comments & attached photos" section; comment activity is also audited.
- Requesters see "Service team" as the internal comment author, while staff and managers see the underlying account email.
- Nginx `client_max_body_size 6m` and Spring `multipart` limits cover image payload and associated fields.
- No image editing, thumbnail server, external storage, malware scanner, or email notification is included. For production, add antivirus scanning, EXIF/privacy review, quotas, retention rules, and scalable object storage.

## Updating an existing installation (keep your data)

1. **Back up the existing PostgreSQL database before any migration.** This update is additive, but backing up is standard release practice.
2. Unzip this package into a separate folder. Copy your existing private `.env` into that folder. **Do not share or commit secrets.** Reconcile any local changes to `compose.yaml` (e.g. the host port) and to `SecurityConfig.java` (allowed local origins) with this release; allowed origins now include `localhost:8081`, `8083`, and `8085`.
3. Keep the same Docker project name and database volume if you want to retain existing data; switching the Compose project name creates a different database volume.
4. From the project root, with Docker running, use the compose and env file appropriate for your setup, for example:

   ```powershell
   docker compose --env-file .env -f compose.yaml up -d --build api web
   docker compose --env-file .env -f compose.yaml logs --tail=100 api
   docker compose --env-file .env -f compose.yaml ps
   ```

5. Check logs for Flyway V5 migration success and backend health; only then test image uploads.
6. If your manager has previously used port `8083` or `8085`, ensure the host port matches the one in `compose.yaml`. No client code hardcodes the origin; calls use `/api` relative to the website.
7. **Never run `docker compose down -v`** during an update: it would remove the database volume.

## Manual acceptance and regression checklist

- [ ] Register/login Requester, Staff, Manager with existing database-backed accounts.
- [ ] Requester submits a request **without** an image; Staff and Manager can still view the request.
- [ ] Requester submits a request **with** a JPG and another with PNG; the image appears under Request Details for Requester, Staff, and Manager after refresh.
- [ ] A bogus `.png` that contains HTML or SVG is rejected; no request is created as a result of that attempted upload.
- [ ] Upload larger than 5 MiB is rejected with a helpful error.
- [ ] Requester A cannot fetch Requester B's image directly, even when given the request ID and image ID (404 or 403).
- [ ] Staff accepts a request, then adds a comment and image; the requester and manager see both text and image after refresh.
- [ ] Staff attaches a photo to a status change, including after acceptance; requester and manager see the photo. No stale-version error on the next update after reloading the latest request.
- [ ] A non-owning Staff user cannot attach a comment/photo to another Staff member's request (403).
- [ ] Manager attaches a comment photo and a status-update photo to a request; Requester and Staff can see both.
- [ ] Existing M2/M3 requests with no media still load with an empty state.
- [ ] Manager filter: All, Requester, Staff, and Manager; combinations with text search; 0-result state; no role/account mutation from filtering.
- [ ] Requesters and Staff cannot call `GET /api/management/users` or bypass the Manager role filter to obtain a privileged user list.
- [ ] Run unit tests, security/RLS database tests, frontend build, browser E2E and CI after merging; capture test evidence for PED v3.0.

## Relevant endpoints

| Role | Method and endpoint | Purpose |
|---|---|---|
| Requester | `POST /api/requests/with-image` | Multipart `request` JSON and optional `image` |
| Requester | `GET /api/requests/{id}/media` | Media metadata for own request |
| Requester | `GET /api/requests/{id}/media/{mediaId}/image` | Authorized private image |
| Staff/Manager | `GET /api/staff/requests/{id}/media` | Media metadata |
| Staff/Manager | `GET /api/staff/requests/{id}/media/{mediaId}/image` | Authorized private image |
| Assigned Staff/Manager | `POST /api/staff/requests/{id}/comments` | Multipart `comment` and optional `image` |
| Assigned Staff/Manager | `POST /api/staff/requests/{id}/status-with-image` | Multipart `status` JSON and `image` |

Original JSON POST endpoints remain compatible. No GitHub integration has been performed.

## Verification status for this handoff

The TypeScript application type-check and local source-pattern checks passed. The backend Java integration tests, PostgreSQL V5 migration, full production frontend bundle and Docker/browser end-to-end checks **have not been executed in this environment**. Run them in your team setup before treating this build as submission-ready.
