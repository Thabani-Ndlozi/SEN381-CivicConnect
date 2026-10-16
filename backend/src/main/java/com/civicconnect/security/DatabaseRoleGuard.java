package com.civicconnect.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Temporary staging diagnostic. Do not remove the startup security check. */
@Component
public class DatabaseRoleGuard implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseRoleGuard.class);
    private final JdbcTemplate jdbc;

    public DatabaseRoleGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String currentUser = jdbc.queryForObject("SELECT current_user::text", String.class);
        String sessionUser = jdbc.queryForObject("SELECT session_user::text", String.class);
        Boolean unsafe = jdbc.queryForObject(
            "SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = current_user",
            Boolean.class
        );
        String configuredRole = System.getenv().getOrDefault("DB_USERNAME", "(unset)");

        // This logs only PostgreSQL role names and privilege flags, never DB credentials.
        LOG.warn("CivicConnect DB role diagnostic: configuredRole={}, currentUser={}, sessionUser={}, privileged={}",
            configuredRole, currentUser, sessionUser, unsafe);

        // Preserve the existing security guard; never disable it to make a deployment start.
        if (!Boolean.FALSE.equals(unsafe)) {
            throw new IllegalStateException(
                "Application DB user must be NOSUPERUSER NOBYPASSRLS. See SETUP.md."
            );
        }
    }
}
