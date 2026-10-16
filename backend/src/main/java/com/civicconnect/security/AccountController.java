package com.civicconnect.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
public class AccountController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    @Value("${civic.auth.mode:legacy}") private String authMode;
    public AccountController(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }
    public record NewAccount(@NotBlank @Size(max=120) String name,
        @NotBlank @Email @Size(max=120) String email,
        @NotBlank @Size(min=12,max=72) String password, String department) {}
    @PostMapping("/api/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@Valid @RequestBody NewAccount input) {
      if ("firebase".equals(authMode)) throw new ResponseStatusException(HttpStatus.GONE,"Use Firebase registration");
      return create(input, "REQUESTER"); }
    @PostMapping("/api/management/users") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> staff(@Valid @RequestBody NewAccount input) {
      if ("firebase".equals(authMode)) throw new ResponseStatusException(HttpStatus.GONE,"Superuser invitations are required");
      return create(input, "STAFF"); }
    private Map<String,Object> create(NewAccount input, String role) {
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes");
        if (input.department()!=null && !input.department().isBlank() && jdbc.queryForObject(
            "select count(*) from departments where name=?", Integer.class,input.department()) == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown department");
        int inserted = jdbc.update("insert into app_users(email,full_name,password_hash,role,department) values(?,?,?,?,?) on conflict(email) do nothing",
            email,input.name().trim(),encoder.encode(input.password()),role,input.department());
        if (inserted==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Account already exists");
        return profile(email);
    }
    private Map<String,Object> profile(String email) {
        return jdbc.queryForMap("select email,full_name as name,role,department,enabled,created_at from app_users where email=?",email);
    }
    @GetMapping("/api/auth/me") public Map<String,Object> me(Authentication auth) { return profile(auth.getName()); }
    public record ChangePassword(@NotBlank String currentPassword,
        @NotBlank @Size(min=12,max=72) String newPassword) {}
    @PostMapping("/api/auth/me/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePassword input) {
        if ("firebase".equals(authMode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use Firebase password management");
        String currentHash = jdbc.queryForObject("select password_hash from app_users where email=?", String.class, authentication.getName());
        if (currentHash == null || !encoder.matches(input.currentPassword(), currentHash))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect");
        if (input.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes");
        if (jdbc.update("update app_users set password_hash=? where email=? and enabled=true", encoder.encode(input.newPassword()), authentication.getName()) != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account is disabled");
    }
    @PostMapping("/api/auth/me/deactivate")
    public void deactivate(Authentication authentication) {
        if (jdbc.update("update app_users set enabled=false where email=? and enabled=true and role <> 'SUPERUSER'", authentication.getName()) != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This account cannot be deactivated");
    }
    @GetMapping("/api/management/users") public List<Map<String,Object>> users() {
        return jdbc.queryForList("select email,full_name as name,role,department,enabled,created_at from app_users order by full_name");
    }
    public record Access(boolean enabled) {}
    @PatchMapping("/api/management/users/{email}") public void access(@PathVariable String email,@RequestBody Access access) {
        if(jdbc.update("update app_users set enabled=? where email=? and role <> 'SUPERUSER'",access.enabled(),email)==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only staff and requester access can be changed");
    }
    @GetMapping("/api/catalog") public Map<String,Object> catalog() {
        return Map.of("priorities",jdbc.queryForList("select code,label from priorities order by sort_order"),"categories",jdbc.queryForList("select code,label from request_categories order by label"),
            "statuses",jdbc.queryForList("select code,label from request_statuses order by sort_order"),
            "departments",jdbc.queryForList("select name from departments order by name"),
            "help",jdbc.queryForList("select title,body from help_articles order by id"));
    }
}
