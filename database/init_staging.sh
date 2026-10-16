#!/bin/sh
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE civicconnect_migrator LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '${MIGRATION_DB_PASSWORD:-$POSTGRES_PASSWORD}';
    CREATE ROLE civicconnect_runtime LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '${DB_PASSWORD:-$POSTGRES_PASSWORD}';
    GRANT ALL ON SCHEMA public TO civicconnect_migrator;
    ALTER SCHEMA public OWNER TO civicconnect_migrator;
    GRANT USAGE ON SCHEMA public TO civicconnect_runtime;
    ALTER DEFAULT PRIVILEGES FOR ROLE civicconnect_migrator IN SCHEMA public GRANT SELECT,INSERT,UPDATE ON TABLES TO civicconnect_runtime;
    ALTER DEFAULT PRIVILEGES FOR ROLE civicconnect_migrator IN SCHEMA public GRANT USAGE,SELECT ON SEQUENCES TO civicconnect_runtime;
EOSQL
