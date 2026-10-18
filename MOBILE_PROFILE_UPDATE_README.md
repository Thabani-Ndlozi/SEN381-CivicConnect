# CivicConnect — Mobile, Authentication, and Profile Update

**Prepared for review; not automatically deployed.** Based on the staging release source. Includes the CORS bean-name fix from the later staging deployment. No database reset, secrets, or account deletion.

## Requested changes

- Remove the redundant public-header **Sign in** action; keep the Sign in/Create account tabs.
- Clear email, password, and name when switching between the authentication tabs.
- Automatically close **Your request has been submitted** after 5 seconds; leave other important notices visible.
- Keep a single **Help** navigation entry for every role; remove the duplicate top-bar Help button.
- Translate Firebase authentication failures into clear messages (including "Incorrect email or password"), hiding Firebase error codes.
- Pin the authenticated top bar and sidebar; scroll only the main page vertically. On narrow screens, the menu stays pinned horizontally beneath the top bar.
- Preserve full desktop tables. Show mobile request cards (Requester: Title, Submitted, Status; Staff/Manager: ID, Submitted, Status) with a full details dialog, including the previous maps, history, media, and request actions.
- Show User Management cards on mobile (Email, Role, Status) and a full-details dialog with the **existing** activation/invitation actions.
- Hide Update Status until the request has been accepted by the current staff/manager user; show **Accept Request** for available, unassigned submissions.
- Profile allows password changes (verify current password first) and voluntary self-deactivation for Requester/Staff/Manager. Deactivation updates `app_users.enabled=false`, blocks staging API access, and preserves all request/audit history. It does **not** delete the Firebase identity; because the Firebase project is shared, the local environment may be separately active. The designated Superuser cannot self-deactivate.
- **No account removal or deletion feature has been added.** Existing User Management Activate/Deactivate actions remain as they were.

## Safe staging rollout — DO NOT skip database step

Current Neon staging schema version: V8; this revision includes `V9__account_self_service.sql`.

The initial V1–V8 Neon objects may be **owned by `neondb_owner`**, not the `civicconnect_migrator` role used by Flyway. Because PostgreSQL requires ownership to create a table policy, run the following in **Neon SQL Editor**, `neondb` database, as `neondb_owner`, **before** deploying backend V9:

```sql
SELECT pg_get_userbyid(relowner) AS table_owner
FROM pg_class WHERE oid = 'public.app_users'::regclass;
```

If the result is `neondb_owner`, make the migrator the owner **of this one table only**:

```sql
ALTER TABLE public.app_users OWNER TO civicconnect_migrator;
```

If PostgreSQL says that `neondb_owner` must be a member of the new owning role, temporarily grant membership, transfer ownership, and revoke it afterward:

```sql
GRANT civicconnect_migrator TO neondb_owner;
ALTER TABLE public.app_users OWNER TO civicconnect_migrator;
REVOKE civicconnect_migrator FROM neondb_owner;
```

Only perform the membership sequence if the direct ownership change fails for that reason, and don't run it while another migration is underway. **Do not** make the runtime role a database owner or bypass RLS.

Afterward, confirm:

```sql
SELECT pg_get_userbyid(relowner) AS table_owner
FROM pg_class WHERE oid = 'public.app_users'::regclass;
```

Expected: `civicconnect_migrator`. Existing data and user rows remain untouched.

### Build and deploy

1. Back up your current staging repository branch. Replace the repository's source files with this updated package, preserving your private Render environment variables and secret file; commit to a review branch/PR if your team uses PRs.
2. Run from `frontend`: `npm ci`, `npm run build`, `npm test`. Do not deploy if they fail. This ZIP was syntax-checked, but network restrictions prevented installing NPM packages in the generation environment, so these must be run on your machine/Render.
3. Build/test the backend with Java 25 and Maven (or Docker backend build). Backend V9 Flyway migration requires the ownership step above.
4. Merge/push only after tests, then manually deploy **civicconnect-api-staging** first and wait for `Live`; Flyway should report schema version **9**. Check the restricted database runtime role.
5. Deploy **civicconnect-web-staging** after the API is healthy (Render `autoDeployTrigger: off` may require Manual Deploy → Deploy latest commit). Static frontend `VITE_*` configuration remains in Render.
6. Re-test mobile navigation, all three lists and dialogs, change password with correct/incorrect current password, non-Superuser self-deactivation, sign-in error messaging, status update after acceptance, notifications, Google Maps and attachments.
7. Remember: this staging environment uses the **same Firebase project** as your local instance. Changing a Firebase password affects that Firebase account in both environments. Deactivating via this feature blocks access in **staging Neon only**; it does not remove the shared Firebase identity.

### Troubleshooting

- If Flyway V9 fails with `must be owner of table app_users`, stop redeploying and correct the owner as described above. Do **not** disable RLS or use `neondb_owner` as the runtime DB login.
- If login fails only in-browser, check that Render has the exact `STAGING_WEB_ORIGIN`, and that `FirebaseSecurityConfig` uses the named `corsConfigurationSource` bean.
- If password change fails, check the visible message; Google's sign-in-only users may need to set an email password using Forgot password instead of this form.
- Keep `BREVO_NOTIFICATIONS_ENABLED=true` and the existing private Brevo key only in the backend environment. The revision does not change Brevo logic.
