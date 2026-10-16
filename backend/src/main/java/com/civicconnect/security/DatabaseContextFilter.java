package com.civicconnect.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

/** Transaction-local identity is reset by PostgreSQL on commit/rollback, including pooled connections. */
public class DatabaseContextFilter extends OncePerRequestFilter {
    private final TransactionTemplate transaction;
    private final JdbcTemplate jdbc;
    public DatabaseContextFilter(PlatformTransactionManager manager, JdbcTemplate jdbc) {
        this.transaction = new TransactionTemplate(manager); this.jdbc = jdbc;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            transaction.executeWithoutResult(status -> {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth == null ? "" : auth.getName();
                String role = auth == null ? "" : auth.getAuthorities().stream()
                    .map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_"))
                    .findFirst().orElse("").replace("ROLE_", "");
                jdbc.queryForObject("select set_config('app.email', ?, true)", String.class, email);
                jdbc.queryForObject("select set_config('app.role', ?, true)", String.class, role);
                try { chain.doFilter(request, response); }
                catch (IOException | ServletException ex) { throw new FilterFailure(ex); }
                if (response.getStatus() >= 400) status.setRollbackOnly();
            });
        } catch (FilterFailure ex) {
            if (ex.getCause() instanceof IOException io) throw io;
            throw (ServletException) ex.getCause();
        }
    }
    private static class FilterFailure extends RuntimeException { FilterFailure(Exception ex) { super(ex); } }
}
