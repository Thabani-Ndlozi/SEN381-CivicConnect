package com.civicconnect.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class FirebaseBearerFilter extends OncePerRequestFilter {
  private final JdbcTemplate jdbc;
  FirebaseBearerFilter(JdbcTemplate jdbc){this.jdbc=jdbc;}
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
      throws ServletException,IOException {
    String header=request.getHeader("Authorization");
    if(header!=null && header.startsWith("Bearer ")) {
      try {
        FirebaseToken token=FirebaseAuth.getInstance().verifyIdToken(header.substring(7),true);
        String email=token.getEmail()==null?"":token.getEmail().trim().toLowerCase(Locale.ROOT);
        var matches=jdbc.query("select email,role,enabled from app_users where firebase_uid=?",
          (rs,n)->new String[]{rs.getString(1),rs.getString(2),Boolean.toString(rs.getBoolean(3))},token.getUid());
        String provider="";
        Object firebaseClaim=token.getClaims().get("firebase");
        if(firebaseClaim instanceof java.util.Map<?,?> details) provider=String.valueOf(details.get("sign_in_provider"));
        boolean allowedMethod=matches.size()==1 && ("REQUESTER".equals(matches.get(0)[1]) || "password".equals(provider));
        if(allowedMethod && matches.get(0)[0].equals(email) && Boolean.parseBoolean(matches.get(0)[2])
           && token.isEmailVerified()) {
          var principal=new UsernamePasswordAuthenticationToken(email,null,
              List.of(new SimpleGrantedAuthority("ROLE_"+matches.get(0)[1])));
          SecurityContextHolder.getContext().setAuthentication(principal);
        }
      } catch (Exception ignored) {
        // Fail closed. Do not reveal credential verification details to the caller.
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request,response);
  }
}
