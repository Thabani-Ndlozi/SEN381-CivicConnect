package com.civicconnect.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.servlet.*;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;

/** Tokens identify the user; PostgreSQL remains the authority for roles. */
@Configuration
@ConditionalOnProperty(name="civic.auth.mode", havingValue="firebase")
public class FirebaseSecurityConfig {
  @Bean FirebaseApp firebaseApp(@Value("${civic.auth.firebase-project:}") String projectId) throws IOException {
    if (projectId.isBlank()) throw new IllegalStateException("FIREBASE_PROJECT_ID is required in firebase auth mode");
    if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
    // Application Default Credentials: mount a service-account JSON into the API container.
    return FirebaseApp.initializeApp(FirebaseOptions.builder()
      .setCredentials(GoogleCredentials.getApplicationDefault())
      .setProjectId(projectId)
      // Test the Google API Client transport instead of the default Apache HTTP/2 transport.
      .setHttpTransport(new com.google.api.client.http.javanet.NetHttpTransport())
      .build());
  }

  @Bean SecurityFilterChain firebaseChain(HttpSecurity http, FirebaseApp app,
      org.springframework.transaction.PlatformTransactionManager tx, JdbcTemplate jdbc) throws Exception {
    // Construct this filter only for Spring Security. Publishing it as a @Bean
    // also auto-registers it as a servlet filter in Spring Boot, where it can
    // run before SecurityContextHolderFilter and lose its authentication.
    FirebaseBearerFilter tokenFilter = new FirebaseBearerFilter(jdbc);
    org.slf4j.LoggerFactory.getLogger(FirebaseSecurityConfig.class)
      .info("CivicConnect Firebase SecurityFilterChain active (auth mode: firebase)");
    http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        // Servlet containers may dispatch an exception to /error. Without
        // explicit error-dispatch permission, Spring Security can overwrite
        // the original 401/404 with an empty 403 response.
        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
        .requestMatchers("/error", "/actuator/health", "/api/release", "/api/auth/firebase/activate").permitAll()
        .requestMatchers("/api/admin/**").hasRole("SUPERUSER")
        .requestMatchers("/api/management/**").hasAnyRole("MANAGER", "SUPERUSER")
        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "MANAGER", "SUPERUSER")
        .requestMatchers("/api/requests/**").hasRole("REQUESTER")
        .anyRequest().authenticated())
      .httpBasic(b -> b.disable())
      .exceptionHandling(errors -> errors
        .authenticationEntryPoint((request, response, exception) -> {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json");
          response.getWriter().write("{\"message\":\"Authentication required\"}");
        })
        .accessDeniedHandler((request, response, exception) -> {
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          response.setContentType("application/json");
          response.getWriter().write("{\"message\":\"Access denied\"}");
        }))
      .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(new DatabaseContextFilter(tx,jdbc), FirebaseBearerFilter.class);
    return http.build();
  }

  @Bean(name = "corsConfigurationSource") CorsConfigurationSource firebaseCors(@Value("${STAGING_WEB_ORIGIN:}") String stagingWebOrigin) {
    CorsConfiguration c = new CorsConfiguration();
    java.util.ArrayList<String> origins = new java.util.ArrayList<>(List.of("http://localhost:5173","http://localhost:8081","http://localhost:8083","http://localhost:8085"));
    String trustedOrigin = stagingWebOrigin.trim().replaceAll("/+$", "");
    if (!trustedOrigin.isEmpty()) {
      if (!trustedOrigin.startsWith("https://") || trustedOrigin.contains("*"))
        throw new IllegalArgumentException("STAGING_WEB_ORIGIN must be an exact https:// origin");
      origins.add(trustedOrigin);
    }
    c.setAllowedOrigins(origins);
    c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization","Content-Type"));
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;
  }
  @Bean org.springframework.security.crypto.password.PasswordEncoder firebasePasswordEncoder(){
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
  }
}
