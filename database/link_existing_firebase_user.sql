-- Use only after checking the Firebase UID and verified email together in Firebase Console.
-- Executed by trusted DBA (postgres), not through a public API.
\set ON_ERROR_STOP on
\prompt 'Existing CivicConnect email (lowercase): ' account_email
\prompt 'Verified Firebase UID belonging to that email: ' verified_firebase_uid
UPDATE app_users SET firebase_uid = :'verified_firebase_uid'
 WHERE email = :'account_email' AND firebase_uid IS NULL;
SELECT email,role,enabled,(firebase_uid IS NOT NULL) AS firebase_linked
 FROM app_users WHERE email = :'account_email';
-- Expect UPDATE 1. If UPDATE 0, investigate. Never guess or share the UID with strangers.
