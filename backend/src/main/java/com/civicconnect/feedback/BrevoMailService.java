package com.civicconnect.feedback;

import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Backend-only transactional email transport. Never expose the Brevo key to React. */
@Service
public class BrevoMailService {
 private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BrevoMailService.class);
 private final ObjectMapper json;
 @Value("${civic.email.enabled:false}") private boolean notificationsEnabled;
 private final String key,senderName,senderEmail,replyTo,appUrl,logoUrl;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
 public BrevoMailService(ObjectMapper json,@Value("${civic.email.brevo-key:}") String key,
   @Value("${civic.email.sender-name:CivicConnect}") String senderName,
   @Value("${civic.email.sender-email:}") String senderEmail,
   @Value("${civic.email.reply-to:}") String replyTo,
   @Value("${civic.email.app-url:http://localhost:8085}") String appUrl,
   @Value("${civic.email.logo-url:}") String logoUrl) {
  this.json=json;this.key=key;this.senderName=senderName;this.senderEmail=senderEmail;
  this.replyTo=replyTo;this.appUrl=appUrl.replaceAll("/$","");this.logoUrl=logoUrl;
 }
 public boolean configured(){return !key.isBlank() && !senderEmail.isBlank();}
 public boolean enabled(){return notificationsEnabled && configured();}
 public static String safe(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 private boolean send(String to,String subject,String html,String text) {
   if(!configured()) {
     log.warn("Brevo email not attempted: API key or sender email is missing");
     return false;
   }
   try {
     Map<String,Object> payload=new LinkedHashMap<>();
     payload.put("sender",Map.of("name",senderName,"email",senderEmail));
     payload.put("to",List.of(Map.of("email",to)));
     if(!replyTo.isBlank()) payload.put("replyTo",Map.of("email",replyTo));
     payload.put("subject",subject);payload.put("htmlContent",html);payload.put("textContent",text);
     HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
       .timeout(Duration.ofSeconds(20)).header("api-key",key)
       .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
     HttpResponse<String> response=http.send(req,HttpResponse.BodyHandlers.ofString());
     boolean accepted = response.statusCode()>=200 && response.statusCode()<300;
     if(!accepted) log.warn("Brevo transactional email rejected; HTTP status={}. Check Brevo Transactional logs and sender settings.", response.statusCode());
     return accepted;
   } catch (InterruptedException e) {
     Thread.currentThread().interrupt();
     log.warn("Brevo transactional email interrupted");
     return false;
   } catch (Exception e) {
     // Log only the exception category; never log API keys, recipients, or action links.
     log.warn("Brevo transactional email request failed; failure type={}", e.getClass().getSimpleName());
     return false;
   }
 }
 private String wrap(String heading,String content) {
   String mark=logoUrl.isBlank()?"<strong style='font-size:23px;color:white'>CivicConnect</strong>":
       "<img src='"+safe(logoUrl)+"' alt='CivicConnect' height='48' style='max-width:240px'>";
   return "<!doctype html><html><body style='margin:0;background:#f1f6fb;font-family:Arial,sans-serif;color:#15304b'>"
     +"<table role='presentation' cellpadding='0' cellspacing='0' width='100%' style='padding:32px 12px'><tr><td align='center'>"
     +"<table role='presentation' width='580' style='max-width:100%;background:white;border-radius:12px;overflow:hidden'>"
     +"<tr><td style='background:#1165c0;padding:28px'>"+mark+"</td></tr>"
     +"<tr><td style='padding:34px'><h1 style='font-size:24px'>"+safe(heading)+"</h1>"+content+"</td></tr>"
     +"<tr><td style='padding:20px;color:#65758b;font-size:12px;background:#f5f8fc'>CivicConnect · Automated service message. Please do not share your login details.</td></tr>"
     +"</table></td></tr></table></body></html>";
 }
 public boolean sendStatus(String email,String id,String title,String from,String to) {
   String url=appUrl+"/";
   String html=wrap("Your request status has changed","<p>Your service request has a new update.</p>"
     +"<div style='background:#eff6ff;padding:20px;border-radius:8px'><b>"+safe(title)+"</b><p>Request #"+safe(id)+"</p>"
     +"<p>"+safe(from)+" → <b>"+safe(to)+"</b></p></div>"
     +"<p><a href='"+safe(url)+"' style='background:#1165c0;color:white;padding:12px 20px;display:inline-block;border-radius:6px;text-decoration:none'>View request details</a></p>");
   return send(email,"CivicConnect request update: "+to,html,
     "CivicConnect request #"+id+" ("+title+") changed from "+from+" to "+to+". Open "+url);
 }
 public boolean sendInvitation(String email,String verifyLink,String resetLink) {
   String html=wrap("You've been invited to CivicConnect","<p>Your Staff or Manager account has been created by the CivicConnect Superuser.</p>"
       +"<ol><li><a href='"+safe(verifyLink)+"'>Verify your work email</a></li>"
       +"<li><a href='"+safe(resetLink)+"'>Set your account password</a></li></ol>"
       +"<p>Once both steps are complete, sign in at <a href='"+safe(appUrl)+"'>CivicConnect</a>.</p>");
   return send(email,"CivicConnect staff/manager invitation",html,
      "CivicConnect invitation: Verify email: "+verifyLink+" Set password: "+resetLink);
 }
}
