package com.civicconnect.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // M2 bootstrap uses explicit Authorization headers, not cookie sessions.
            // Revisit CSRF together with the final authentication mechanism.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/management/**").hasRole("MANAGER")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "MANAGER")
                .requestMatchers("/api/requests/**").hasRole("REQUESTER")
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Profile("dev")
    UserDetailsService developmentUsers(
            @Value("${civicconnect.dev-users.requester.username}") String requesterUsername,
            @Value("${civicconnect.dev-users.requester.password}") String requesterPassword,
            @Value("${civicconnect.dev-users.staff.username}") String staffUsername,
            @Value("${civicconnect.dev-users.staff.password}") String staffPassword,
            @Value("${civicconnect.dev-users.manager.username}") String managerUsername,
            @Value("${civicconnect.dev-users.manager.password}") String managerPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(requesterUsername).password("{noop}" + requesterPassword).roles("REQUESTER").build(),
                User.withUsername(staffUsername).password("{noop}" + staffPassword).roles("STAFF").build(),
                User.withUsername(managerUsername).password("{noop}" + managerPassword).roles("MANAGER").build()
        );
    }
}
