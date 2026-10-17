-- Run once as postgres on the target database: psql -U postgres -d civicconnect -f database/provision.sql
\set ON_ERROR_STOP on
\prompt 'Migration role password: ' migration_password
\prompt 'Runtime role password: ' runtime_password
SELECT format('CREATE ROLE civicconnect_migrator LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD %L', :'migration_password')
WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='civicconnect_migrator') \gexec
SELECT format('CREATE ROLE civicconnect_runtime LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD %L', :'runtime_password')
WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='civicconnect_runtime') \gexec
GRANT USAGE, CREATE ON SCHEMA public TO civicconnect_migrator;
GRANT USAGE ON SCHEMA public TO civicconnect_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE civicconnect_migrator IN SCHEMA public GRANT SELECT,INSERT,UPDATE ON TABLES TO civicconnect_runtime;
-- Existing installations: transfer ownership to the migrator as the previous owner/admin before migrating.
-- Upgrade ownership of the known baseline tables without touching unrelated tables.
SELECT format('ALTER TABLE public.%I OWNER TO civicconnect_migrator',tablename)
FROM pg_tables WHERE schemaname='public' AND tablename IN ('service_requests','request_audit','flyway_schema_history') \gexec
GRANT SELECT,INSERT,UPDATE ON ALL TABLES IN SCHEMA public TO civicconnect_runtime;
DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_tables WHERE schemaname='public' AND tablename='app_users') THEN
    REVOKE UPDATE ON public.app_users FROM civicconnect_runtime;
    GRANT UPDATE (password_hash, enabled) ON public.app_users TO civicconnect_runtime;
  END IF;
END
$$;
