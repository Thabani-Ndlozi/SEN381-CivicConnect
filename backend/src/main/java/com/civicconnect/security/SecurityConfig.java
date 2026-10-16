package com.civicconnect.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@ConditionalOnProperty(name="civic.auth.mode", havingValue="legacy", matchIfMissing=true)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, org.springframework.transaction.PlatformTransactionManager tx, org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
        // Return a plain 401 JSON response without the WWW-Authenticate: Basic header.
        // Without that header, browsers will not show their native credential popup,
        // allowing the React app to display its own inline error message instead.
        AuthenticationEntryPoint noPopupEntryPoint = (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
        };

        http
            .cors(Customizer.withDefaults())
            // M2 bootstrap uses explicit Authorization headers, not cookie sessions.
            // Revisit CSRF together with the final authentication mechanism.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/actuator/health", "/api/release").permitAll()
                .requestMatchers("/api/management/**").hasRole("MANAGER")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "MANAGER")
                .requestMatchers("/api/requests/**").hasRole("REQUESTER")
                .anyRequest().authenticated())
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.authenticationEntryPoint(noPopupEntryPoint))
            .addFilterAfter(new DatabaseContextFilter(tx, jdbc), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8081", "http://localhost:8083", "http://localhost:8085"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService databaseUsers(
            org.springframework.jdbc.core.JdbcTemplate jdbc,
            org.springframework.security.crypto.password.PasswordEncoder encoder,
            @Value("${STAGING_REQUESTER_USERNAME:}") String reqUser,
            @Value("${STAGING_REQUESTER_PASSWORD:}") String reqPw,
            @Value("${STAGING_STAFF_USERNAME:}") String staffUser,
            @Value("${STAGING_STAFF_PASSWORD:}") String staffPw,
            @Value("${STAGING_MANAGER_USERNAME:}") String mgrUser,
            @Value("${STAGING_MANAGER_PASSWORD:}") String mgrPw) {
        return username -> {
            String normalized = username.trim().toLowerCase(java.util.Locale.ROOT);
            if (!reqUser.isBlank() && normalized.equalsIgnoreCase(reqUser.trim())) {
                return User.withUsername(reqUser.trim()).password(encoder.encode(reqPw)).roles("REQUESTER").build();
            }
            if (!staffUser.isBlank() && normalized.equalsIgnoreCase(staffUser.trim())) {
                return User.withUsername(staffUser.trim()).password(encoder.encode(staffPw)).roles("STAFF").build();
            }
            if (!mgrUser.isBlank() && normalized.equalsIgnoreCase(mgrUser.trim())) {
                return User.withUsername(mgrUser.trim()).password(encoder.encode(mgrPw)).roles("MANAGER").build();
            }
            return jdbc.query("select email,password_hash,role,enabled from app_users where email = ?",
                (rs, n) -> User.withUsername(rs.getString(1)).password(rs.getString(2))
                    .roles(rs.getString(3)).disabled(!rs.getBoolean(4)).build(), normalized)
                .stream().findFirst().orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Invalid credentials"));
        };
    }
}
