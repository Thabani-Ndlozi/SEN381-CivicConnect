-- Run only as the database administrator after verifying this Firebase UID/email in Firebase Console.
-- DO NOT run with arbitrary UID or use an unverified account.
\set ON_ERROR_STOP on
\prompt 'Firebase UID for verified inl2group13@gmail.com: ' firebase_uid
DO $$ BEGIN IF EXISTS(SELECT 1 FROM app_users WHERE role='SUPERUSER') THEN
 RAISE EXCEPTION 'Superuser already exists'; END IF; END $$;
INSERT INTO app_users(email,full_name,password_hash,role,department,firebase_uid,enabled)
VALUES('inl2group13@gmail.com','CivicConnect Superuser','!firebase-only!', 'SUPERUSER',NULL,:'firebase_uid',true)
ON CONFLICT(email) DO UPDATE SET role='SUPERUSER',firebase_uid=EXCLUDED.firebase_uid,
 password_hash='!firebase-only!',enabled=true;
-- Before running, ensure Firebase email ownership is verified and account UID matches.
