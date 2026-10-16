-- Grants are executed as the migration-table owner, never by the restricted runtime role.
-- The role is created explicitly in Neon SQL Editor before first deployment.
GRANT USAGE ON SCHEMA public TO civicconnect_runtime;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO civicconnect_runtime;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO civicconnect_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO civicconnect_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO civicconnect_runtime;
