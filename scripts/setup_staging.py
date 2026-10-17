#!/usr/bin/env python3
"""Generate a private .env.staging if absent; don't commit generated secrets."""
from pathlib import Path
from secrets import token_urlsafe
p=Path('.env.staging')
if p.exists():raise SystemExit('.env.staging exists; leaving credentials unchanged')
items={'POSTGRES_DB':'civicconnect','POSTGRES_USER':'civicconnect','POSTGRES_PASSWORD':token_urlsafe(28),
 'STAGING_REQUESTER_USERNAME':'requester-staging','STAGING_REQUESTER_PASSWORD':token_urlsafe(20),
 'STAGING_STAFF_USERNAME':'staff-staging','STAGING_STAFF_PASSWORD':token_urlsafe(20),
 'STAGING_MANAGER_USERNAME':'manager-staging','STAGING_MANAGER_PASSWORD':token_urlsafe(20),
 'RC_VERSION':'m3-rc1-pending-git-tag'}
p.write_text(''.join(f'{k}={v}\n' for k,v in items.items()))
p.chmod(0o600)
print('Generated local .env.staging (never commit). Staging test scripts read it automatically.')
