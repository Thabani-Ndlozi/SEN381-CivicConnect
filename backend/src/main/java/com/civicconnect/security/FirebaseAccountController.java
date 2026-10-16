package com.civicconnect.security;

import com.google.firebase.auth.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.civicconnect.feedback.BrevoMailService;

/** Firebase UID never confers a role by itself; PostgreSQL owns authorization. */
@RestController
@ConditionalOnProperty(name="civic.auth.mode",havingValue="firebase")
public class FirebaseAccountController {
 private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FirebaseAccountController.class);
 private final JdbcTemplate jdbc;
 private final BrevoMailService mail;
 FirebaseAccountController(JdbcTemplate jdbc,BrevoMailService mail){this.jdbc=jdbc;this.mail=mail;}
 /** Return a safe and actionable error for activation; do not mask it behind an empty 403. */
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,String>> accountFailure(ResponseStatusException ex) {
  return ResponseEntity.status(ex.getStatusCode())
    .body(Map.of("message",ex.getReason()==null?"Unable to complete authentication":ex.getReason()));
 }

 public record Activation(@Size(max=120) String name){}
 @PostMapping("/api/auth/firebase/activate")
 public Map<String,Object> activate(@RequestHeader(value="Authorization",required=false) String auth,
                                   @RequestBody(required=false) Activation input) {
  FirebaseToken token=validate(auth);
  String email=token.getEmail()==null?"":token.getEmail().trim().toLowerCase(Locale.ROOT);
  if(!token.isEmailVerified() || email.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify your email before signing in");
  List<Map<String,Object>> rows=jdbc.queryForList(
    "select email,full_name as name,role,enabled,firebase_uid from app_users where email=? or firebase_uid=?",email,token.getUid());
  if(!rows.isEmpty()) {
   var row=rows.get(0);
   // Never link an existing email by matching a claimed email alone: requires DBA verified migration.
   if(!token.getUid().equals(row.get("firebase_uid")) || !email.equals(row.get("email")))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Existing account must be linked by an administrator");
   if(!Boolean.TRUE.equals(row.get("enabled"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account disabled");
   return Map.of("email",email,"role",row.get("role"));
  }
  String name=input==null || input.name()==null || input.name().isBlank()?
    (token.getName()==null || token.getName().isBlank()?email:token.getName()):input.name().trim();
  if(name.length()>120) name=name.substring(0,120);
  int added=jdbc.update("insert into app_users(email,full_name,password_hash,role,firebase_uid) values(?,?,'!firebase-only!','REQUESTER',?) on conflict do nothing",
    email,name,token.getUid());
  if(added==0) {
    var current=jdbc.queryForList("select role,enabled from app_users where email=? and firebase_uid=?", email,token.getUid());
    if(current.size()!=1 || !Boolean.TRUE.equals(current.get(0).get("enabled")))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Account already exists; contact support");
    return Map.of("email",email,"role",current.get(0).get("role"));
  }
  return Map.of("email",email,"role","REQUESTER");
 }
 private FirebaseToken validate(String auth) {
  if(auth==null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Missing Firebase ID token");
  try {return FirebaseAuth.getInstance().verifyIdToken(auth.substring(7),true);}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid Firebase ID token");}
 }
 public record Invite(@NotBlank @Size(max=120) String name,@NotBlank @Email String email,
                      @NotBlank String role,String department){}
 @PostMapping("/api/admin/users") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> invite(@Valid @RequestBody Invite input,Authentication actor) {
  String role=input.role().trim().toUpperCase(Locale.ROOT);
  if(!role.equals("STAFF") && !role.equals("MANAGER"))
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only Staff or Manager can be invited");
  String email=input.email().trim().toLowerCase(Locale.ROOT);
  if(jdbc.queryForObject("select count(*) from app_users where email=?",Integer.class,email)>0)
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Account already exists");
  if(input.department()!=null && !input.department().isBlank() && jdbc.queryForObject(
        "select count(*) from departments where name=?",Integer.class,input.department())==0)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown department");
  UserRecord user;
  try {user=FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
      .setEmail(email).setDisplayName(input.name().trim()).setEmailVerified(false));}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unable to provision Firebase identity");}
  try {
    jdbc.update("insert into app_users(email,full_name,password_hash,role,department,firebase_uid) values(?,?,'!firebase-only!',?,?,?)",
      email,input.name().trim(),role,input.department()==null||input.department().isBlank()?null:input.department(),user.getUid());
  } catch(Exception e){
    try {FirebaseAuth.getInstance().deleteUser(user.getUid());} catch(Exception ignored){}
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to create application profile");
  }
  boolean sent=sendInvite(email);
  return Map.of("email",email,"role",role,"invitationSent",sent);
 }
 @PostMapping("/api/admin/users/{email}/resend-invitation")
 public Map<String,Object> resend(@PathVariable String email) {
  String normalized=email.trim().toLowerCase(Locale.ROOT);
  var rows=jdbc.queryForList(
    "select firebase_uid,enabled from app_users where email=? and role in ('STAFF','MANAGER')",normalized);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Internal account not found");
  var row=rows.get(0);
  if(!Boolean.TRUE.equals(row.get("enabled")))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Activate the account before resending an invitation");
  String uid=(String)row.get("firebase_uid");
  if(uid==null || uid.isBlank())
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Account has no linked Firebase identity");
  try {
    UserRecord identity=FirebaseAuth.getInstance().getUser(uid);
    if(!normalized.equalsIgnoreCase(identity.getEmail()) || identity.isDisabled())
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Firebase identity does not match this enabled account");
  } catch (ResponseStatusException e) {throw e;}
    catch (FirebaseAuthException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unable to verify Firebase account for invitation");
    }
  return Map.of("invitationSent",sendInvite(normalized));
 }
 private boolean sendInvite(String email) {
  try {
    String verify=FirebaseAuth.getInstance().generateEmailVerificationLink(email);
    String setPassword=FirebaseAuth.getInstance().generatePasswordResetLink(email);
    return mail.sendInvitation(email,verify,setPassword);
  }catch(Exception e){
    // Diagnostic category only: never log email verification links, passwords, UIDs or recipients.
    log.warn("Firebase invitation link generation failed; failure type={}",e.getClass().getSimpleName());
    return false;
  }
 }
}
