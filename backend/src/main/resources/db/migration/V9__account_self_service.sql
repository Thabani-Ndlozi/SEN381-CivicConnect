-- Add self-service profile changes without touching the existing Superuser/Manager policies.
-- RLS limits updated rows to the signed-in non-Superuser's own account.
-- Backend endpoints additionally restrict the columns that may change.
CREATE POLICY account_self_manage ON app_users FOR UPDATE USING (
  email = current_setting('app.email', true) AND role <> 'SUPERUSER'
) WITH CHECK (
  email = current_setting('app.email', true) AND role <> 'SUPERUSER'
);
