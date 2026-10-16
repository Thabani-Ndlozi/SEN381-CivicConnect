-- Run as the database administrator after Flyway migrations. Never run with app runtime credentials.
-- psql -U postgres -d civicconnect -f database/create_manager.sql
\set ON_ERROR_STOP on
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\prompt 'Manager full name: ' manager_name
\prompt 'Manager email: ' manager_email
\prompt 'Manager password (12 to 72 UTF-8 bytes): ' manager_password
-- Reject invalid passwords and existing addresses; no overwrite of an existing account.
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=current_user AND rolsuper)
THEN RAISE EXCEPTION 'Use a database administrator to provision a manager'; END IF; END $$;
CREATE TEMP TABLE manager_input(name text,email text,password text CHECK(octet_length(password) BETWEEN 12 AND 72 AND length(password)>=12));
INSERT INTO manager_input VALUES (:'manager_name',lower(trim(:'manager_email')),:'manager_password');
INSERT INTO app_users(email,full_name,password_hash,role)
SELECT email,name,crypt(password,gen_salt('bf',12)),'MANAGER' FROM manager_input;
DROP TABLE manager_input;
\unset manager_password
