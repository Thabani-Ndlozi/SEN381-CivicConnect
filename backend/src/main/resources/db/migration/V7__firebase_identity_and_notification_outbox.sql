-- Add Firebase identity without changing existing request ownership (email keys).
ALTER TABLE app_users ADD COLUMN firebase_uid varchar(128) UNIQUE;
-- Replace the originally anonymous role CHECK safely by looking up its actual name.
DO $$ DECLARE chk text;
BEGIN
 SELECT conname INTO chk FROM pg_constraint
 WHERE conrelid='app_users'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%role%';
 IF chk IS NOT NULL THEN EXECUTE format('ALTER TABLE app_users DROP CONSTRAINT %I',chk); END IF;
END $$;
ALTER TABLE app_users ADD CONSTRAINT ck_users_role CHECK (role IN ('REQUESTER','STAFF','MANAGER','SUPERUSER'));
CREATE UNIQUE INDEX uq_one_superuser ON app_users ((role)) WHERE role='SUPERUSER';

DROP POLICY IF EXISTS account_register ON app_users;
CREATE POLICY account_register ON app_users FOR INSERT WITH CHECK (
 (role='REQUESTER' AND current_setting('app.role',true) IS DISTINCT FROM 'STAFF')
 OR (role IN ('STAFF','MANAGER') AND current_setting('app.role',true)='SUPERUSER'));
DROP POLICY IF EXISTS account_manage ON app_users;
CREATE POLICY account_manage ON app_users FOR UPDATE USING (
 (current_setting('app.role',true)='SUPERUSER' AND role IN ('REQUESTER','STAFF','MANAGER'))
 OR (current_setting('app.role',true)='MANAGER' AND role='STAFF'))
 WITH CHECK ((current_setting('app.role',true)='SUPERUSER' AND role IN ('REQUESTER','STAFF','MANAGER'))
 OR (current_setting('app.role',true)='MANAGER' AND role='STAFF'));
-- One-time Superuser bootstrap is intentionally DBA-only, not an application endpoint.
DROP POLICY IF EXISTS request_read ON service_requests;
CREATE POLICY request_read ON service_requests FOR SELECT USING (
 current_setting('app.role',true) IN ('STAFF','MANAGER','SUPERUSER')
 OR requester_id=current_setting('app.email',true));
DROP POLICY IF EXISTS request_update ON service_requests;
CREATE POLICY request_update ON service_requests FOR UPDATE USING (
 current_setting('app.role',true) IN ('MANAGER','SUPERUSER')
 OR (current_setting('app.role',true)='STAFF' AND (owner_id=current_setting('app.email',true) OR (owner_id IS NULL AND status='SUBMITTED'))))
 WITH CHECK (current_setting('app.role',true) IN ('MANAGER','SUPERUSER')
 OR (current_setting('app.role',true)='STAFF' AND (owner_id=current_setting('app.email',true) OR (owner_id IS NULL AND status='REJECTED'))));
DROP POLICY IF EXISTS request_media_read ON request_media;
CREATE POLICY request_media_read ON request_media FOR SELECT USING (
 EXISTS(SELECT 1 FROM service_requests r WHERE r.id=request_id AND
 (current_setting('app.role',true) IN ('STAFF','MANAGER','SUPERUSER') OR r.requester_id=current_setting('app.email',true))));
DROP POLICY IF EXISTS request_media_insert ON request_media;
CREATE POLICY request_media_insert ON request_media FOR INSERT WITH CHECK (
 uploader_id=current_setting('app.email',true) AND EXISTS(SELECT 1 FROM service_requests r WHERE r.id=request_id AND
 ((kind='REQUEST_IMAGE' AND current_setting('app.role',true)='REQUESTER' AND r.requester_id=current_setting('app.email',true))
 OR (kind='COMMENT' AND (current_setting('app.role',true) IN ('MANAGER','SUPERUSER')
 OR (current_setting('app.role',true)='STAFF' AND r.owner_id=current_setting('app.email',true)))))));

CREATE TABLE notification_outbox (
 id uuid PRIMARY KEY, request_id uuid NOT NULL, recipient_email varchar(120) NOT NULL,
 previous_status varchar(30) NOT NULL, next_status varchar(30) NOT NULL,
 request_title varchar(160) NOT NULL,
 attempts integer NOT NULL DEFAULT 0, sent_at timestamptz, next_attempt_at timestamptz NOT NULL DEFAULT now(),
 created_at timestamptz NOT NULL DEFAULT now(), last_error varchar(200)
);
CREATE INDEX idx_outbox_pending ON notification_outbox(next_attempt_at) WHERE sent_at IS NULL;
-- The application is the only client of this private, unexposed table.
