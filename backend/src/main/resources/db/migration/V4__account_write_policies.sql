ALTER TABLE app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_users FORCE ROW LEVEL SECURITY;
-- Authentication needs password hashes; this table is never exposed directly to clients.
CREATE POLICY account_authenticate ON app_users FOR SELECT USING (true);
CREATE POLICY account_register ON app_users FOR INSERT WITH CHECK (
 role='REQUESTER' OR (role='STAFF' AND current_setting('app.role',true)='MANAGER'));
CREATE POLICY account_manage ON app_users FOR UPDATE USING (
 current_setting('app.role',true)='MANAGER' AND role<>'MANAGER') WITH CHECK (
 current_setting('app.role',true)='MANAGER' AND role<>'MANAGER');
