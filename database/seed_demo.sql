-- Optional demo requests. Run as database administrator after creating a requester in the app.
-- psql -U postgres -d civicconnect -f database/seed_demo.sql
\set ON_ERROR_STOP on
\prompt 'Existing requester email: ' requester_email
BEGIN;
CREATE TEMP TABLE seed_owner(email text NOT NULL);
INSERT INTO seed_owner SELECT email FROM app_users WHERE email=lower(trim(:'requester_email')) AND role='REQUESTER';
DO $$ BEGIN IF NOT EXISTS (SELECT FROM seed_owner) THEN RAISE EXCEPTION 'Register this requester in the app first'; END IF; END $$;
WITH created AS (
 INSERT INTO service_requests(id,requester_id,title,description,category,status,created_at,updated_at,version,location,priority)
 SELECT gen_random_uuid(),o.email,'Demo: '||c.label,'Optional demonstration request. Replace this with a real issue description.',c.code,'SUBMITTED',now(),now(),0,'Demo location','NORMAL'
 FROM seed_owner o CROSS JOIN request_categories c
 WHERE NOT EXISTS (SELECT 1 FROM service_requests r WHERE r.requester_id=o.email AND r.title='Demo: '||c.label)
 RETURNING id,requester_id
)
INSERT INTO request_audit(id,request_id,actor,action,to_status,comment,occurred_at)
SELECT gen_random_uuid(),id,requester_id,'REQUEST_CREATED','SUBMITTED','Demo request seeded',now() FROM created;
COMMIT;
