# CivicConnect M3 — Resend Invitation + Brevo delivery feedback

This source update adds **Resend Invitation** to **Superuser → User Administration** for existing, enabled Staff/Manager accounts, improves invitation-creation feedback, and logs a safe Brevo HTTP rejection status in Spring Boot. It includes the **NetHttpTransport Firebase Admin change** that resolved Firebase ID-token verification in local testing.

## Important: working database and environment

Do NOT overwrite your existing `.env`, `compose.yaml`, `secrets/` directory, or PostgreSQL volume. Do NOT run `docker compose down -v`. No database migration is required for this UI/backend change.

The existing correctly entered Staff account can already log in through Firebase. **It does not need to be recreated or invited again** just to gain access. The new button is most useful for onboarding the next user or troubleshooting a missed invitation. Consider **deactivating** the old misspelled Staff account through User Administration to prevent unintended access; do not remove requests or the database.

## Recommended installation: five-file patch

Extract `CivicConnect_M3_Resend_Invitation_Patch.zip` to a separate directory. From the extracted patch, copy exactly these files into the *same paths* inside your already-working `CivicConnect_Milestone3_Release` folder:

- `frontend/src/App.tsx`
- `frontend/src/app.css`
- `backend/src/main/java/com/civicconnect/security/FirebaseAccountController.java`
- `backend/src/main/java/com/civicconnect/security/FirebaseSecurityConfig.java`
- `backend/src/main/java/com/civicconnect/feedback/BrevoMailService.java`

Back up the original five files first if you've made additional local edits. In particular, **keep the known-working Firebase NetHttpTransport** in `FirebaseSecurityConfig.java`; this patch includes it and removes temporary token-signature diagnostics from `FirebaseAccountController.java`.

From your existing project directory, run:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml up -d --build --force-recreate api web
docker compose -p civicconnect_local --env-file .env -f compose.yaml ps -a
```

Continue using your existing URL `http://localhost:8085` and existing host ports `8084` API and `5434` PostgreSQL. The original full candidate ZIP defaults to different ports, so **the five-file patch is safer** than replacing your entire local project folder.

## Testing

1. Sign in as `inl2group13@gmail.com` (Superuser).
2. Open **User Management**, find an **enabled Staff or Manager** account with a correct email address, click **Resend Invitation**. Don't create a duplicate account.
3. UI reports whether Brevo **accepted the API call** or the result was unconfirmed. Acceptance is not proof of inbox delivery.
4. In PowerShell, inspect *only the safe Brevo diagnostic lines*:

```powershell
docker compose -p civicconnect_local --env-file .env -f compose.yaml logs --since=5m api | Select-String 'Brevo'
```

5. If Brevo reports HTTP `401/403`, check its API-key and sender/IP restrictions; HTTP `400` suggests the sender/payload or another request validation issue; HTTP `429` indicates rate limiting. If Brevo accepted the call but no email arrives, check **Brevo → Transactional Logs** and the correct inbox/spam/junk folder. The log deliberately does not print API keys, user email addresses, action links, or provider response bodies.
6. An invitation can contain Firebase verification and password setup links. **Receiving a Firebase password-reset email proves Firebase email delivery, not Brevo invitation delivery**.

## Access controls and security

- Only authenticated Superusers may call `POST /api/admin/users/{email}/resend-invitation`.
- Only enabled `STAFF` or `MANAGER` PostgreSQL accounts with a matching Firebase UID and email may be resent an invitation.
- `REQUESTER` and `SUPERUSER` accounts cannot be targeted by this endpoint.
- No role/UID/password/account change occurs on resend; the backend generates fresh Firebase action links and sends an email via Brevo.
- Invitation sending is separate from `BREVO_NOTIFICATIONS_ENABLED` (which controls status-update notification dispatch). **The API key and sender must still be correctly configured**.

## Validation status

This patch passed a local TypeScript transpilation/syntax check, source-level route/role/transport checks, and ZIP-integrity verification. It has **not** been compiled against your live Java/Maven dependencies or used to send a real Brevo email in this environment. Test on your Docker installation before staging deployment.
