-- Hardening for self-service updates on app_users (Defense-in-Depth):
-- 1. Restrict the account_self_manage RLS policy WITH CHECK to prevent role escalation.
--    The updated row's role must match the active session role (cannot elevate to MANAGER/STAFF).
-- 2. Revoke table-level UPDATE on app_users from the runtime role and grant column-level UPDATE
--    only on password_hash and enabled. Sensitive columns (role, email, firebase_uid) cannot be updated.

DROP POLICY IF EXISTS account_self_manage ON app_users;
CREATE POLICY account_self_manage ON app_users FOR UPDATE USING (
  email = current_setting('app.email', true) AND role <> 'SUPERUSER'
) WITH CHECK (
  email = current_setting('app.email', true)
  AND role = current_setting('app.role', true)
);

DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'civicconnect_runtime') THEN
    REVOKE UPDATE ON app_users FROM civicconnect_runtime;
    GRANT UPDATE (password_hash, enabled) ON app_users TO civicconnect_runtime;
  END IF;
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'runtime') THEN
    REVOKE UPDATE ON app_users FROM runtime;
    GRANT UPDATE (password_hash, enabled) ON app_users TO runtime;
  END IF;
END
$$;
