# CivicConnect M3 — Landing page and public navigation update

## What was changed

1. Removed the baked-in sign-in/signup mockup background and the CSS rule that hid real hero text. The supplied buildings-only image is now at `frontend/public/assets/landing-buildings.png`.
2. Rebuilt the hero copy and three feature rows as selectable, accessible live HTML and SVG icons.
3. Added visible **Continue with Google** (Google-branded icon) and **Forgot password?** controls in the requester authentication card.
4. Added separate public pages with distinct browser URLs: `/` (Home), `/how-it-works`, `/services`, `/about`, and `/forgot-password`.
5. Fixed client-side history handling so links navigate to different page contents, direct refreshes work through the existing Nginx fallback, and browser back/forward works.
6. Fixed desktop landing-page scrolling by confining the layout to the viewport beneath the header. On narrow/mobile screens scrolling is intentionally allowed so content and controls remain accessible.
7. Matched the Services page categories to those defined in the backend database migration.

## Important: Google login and email password reset

**The backend in the uploaded ZIP does not implement Google OAuth or password-reset emails.** This update provides the correct **UI entry points**, but not fictitious integrations:

- Clicking **Continue with Google** displays a truthful message that it is not configured. Email/password login is unchanged and still functional.
- **Forgot password?** opens a real standalone recovery screen and email form. Submission clearly states that delivery is not set up and **does not claim an email was sent**.
- To make these operational, the team must implement and test Google OAuth client/server ID-token handling or OAuth2 login, account-linking/role assignment, token-based reset endpoints, SMTP/email delivery, expiry, one-time token use, and abuse protections. Configure real provider credentials via deployment secrets, never commit them.

## Local build and review

The ZIP intentionally excludes `node_modules` and local credentials. Copy your original `.env` / `.env.staging` from your **existing working machine** (do not share/commit them) or use the provided example env files to configure your own environment.

From this release folder on Windows PowerShell:

```powershell
# Run your usual command for the compose file and port you already use.
docker compose --env-file .env -f compose.yaml up -d --build
```

If your current installation uses 8083 because M2 occupies 8081, **keep your existing port mapping** in `compose.yaml` when merging this updated source, or change the `web` mapping from `8081:80` to `8083:80` in your new copy.

Then open the frontend at your configured host port (e.g. http://localhost:8083).

To run frontend checks outside Docker, from `frontend`:

```powershell
npm ci
npm run build
npm run test:public-pages
npm test
```

## Public-page acceptance checks

- [ ] At desktop width, the Home screen has no page scrollbar.
- [ ] The background contains **only** the buildings scene, not duplicated text or a second baked sign-in form.
- [ ] The hero heading, feature labels/icons, form inputs, buttons and links are real HTML elements.
- [ ] Sign in and Create account tabs both work with the existing email/password implementation.
- [ ] Continue with Google is visible on both tabs, and accurately indicates configuration is pending.
- [ ] Forgot password? navigates to `/forgot-password` and offers an email input with honest status.
- [ ] Home navigates to `/`.
- [ ] How It Works navigates to `/how-it-works` and shows four lifecycle steps.
- [ ] Services navigates to `/services` and shows the seven actual app request categories.
- [ ] About navigates to `/about` and displays platform information.
- [ ] Browser back/forward buttons change displayed page content appropriately.
- [ ] Hard-refreshing `/how-it-works`, `/services`, `/about`, `/forgot-password` loads the correct page through Nginx.
- [ ] On mobile, the nav is accessible and the page may scroll to preserve content accessibility.
- [ ] Requester registration/login and all signed-in dashboards still work.
- [ ] Existing database requests and credentials remain intact following the frontend rebuild.

## Verification carried out here

- TypeScript project check (`tsc -b`): PASS.
- Server-side rendering checks for all five public routes and navigation hrefs: PASS.
- Full Vite production build and interactive browser click testing: **NOT VERIFIED in this container**, because the uploaded `node_modules` directory contains Windows-specific Rolldown/Vite native bindings and Linux dependency download was unavailable. The Docker build on your machine reinstalls dependencies inside its Linux build stage and should be run before sign-off.
