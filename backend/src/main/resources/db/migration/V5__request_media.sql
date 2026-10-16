-- Private request media. Image bytes are never served as public static assets.
-- Requesters can only read images belonging to their own requests;
-- staff/managers can read all requests through the existing role permissions.
CREATE TABLE request_media (
  id uuid PRIMARY KEY,
  request_id uuid NOT NULL REFERENCES service_requests(id),
  uploader_id varchar(120) NOT NULL,
  kind varchar(20) NOT NULL CHECK (kind IN ('REQUEST_IMAGE','COMMENT')),
  comment varchar(2000),
  original_name varchar(160),
  content_type varchar(30),
  data bytea,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_media_payload CHECK (
    (kind='REQUEST_IMAGE' AND data IS NOT NULL AND comment IS NULL)
    OR (kind='COMMENT' AND comment IS NOT NULL AND length(trim(comment)) > 0)
  ),
  CONSTRAINT ck_media_image_fields CHECK (
    (data IS NULL AND content_type IS NULL AND original_name IS NULL)
    OR (data IS NOT NULL AND content_type IN ('image/png','image/jpeg') AND octet_length(data) <= 5242880)
  )
);
CREATE INDEX idx_request_media_request ON request_media(request_id,created_at,id);
ALTER TABLE request_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE request_media FORCE ROW LEVEL SECURITY;
CREATE POLICY request_media_read ON request_media FOR SELECT USING (
 EXISTS (SELECT 1 FROM service_requests r WHERE r.id=request_id AND
   (current_setting('app.role',true) IN ('STAFF','MANAGER') OR r.requester_id=current_setting('app.email',true)))
);
CREATE POLICY request_media_insert ON request_media FOR INSERT WITH CHECK (
 uploader_id=current_setting('app.email',true) AND EXISTS (
 SELECT 1 FROM service_requests r WHERE r.id=request_id AND
 ((kind='REQUEST_IMAGE' AND current_setting('app.role',true)='REQUESTER' AND r.requester_id=current_setting('app.email',true))
 OR (kind='COMMENT' AND (current_setting('app.role',true)='MANAGER'
    OR (current_setting('app.role',true)='STAFF' AND r.owner_id=current_setting('app.email',true)))))
  )
);
-- No UPDATE or DELETE policy: attachment evidence is immutable.
