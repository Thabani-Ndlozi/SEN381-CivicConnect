package com.civicconnect.feedback;

import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** At-least-once email delivery, one pending record per committed status event. */
@Component
public class NotificationDispatcher {
 private final JdbcTemplate jdbc;private final BrevoMailService brevo;private final TransactionTemplate tx;
 public NotificationDispatcher(JdbcTemplate jdbc,BrevoMailService brevo,PlatformTransactionManager manager){
  this.jdbc=jdbc;this.brevo=brevo;this.tx=new TransactionTemplate(manager);
 }
 @Scheduled(fixedDelayString="${civic.email.dispatch-delay-ms:60000}")
 public void dispatch(){
   if(!brevo.enabled()) return;
   for(int i=0;i<10;i++) {
     Boolean processed=tx.execute(status->{
       List<Map<String,Object>> jobs=jdbc.queryForList("""
         select id,request_id,recipient_email,request_title,previous_status,next_status,attempts
         from notification_outbox where sent_at is null and next_attempt_at<=now()
         order by created_at for update skip locked limit 1
         """);
       if(jobs.isEmpty())return false;
       var job=jobs.get(0);UUID id=(UUID)job.get("id");
       boolean ok=brevo.sendStatus((String)job.get("recipient_email"),job.get("request_id").toString(),
         (String)job.get("request_title"),(String)job.get("previous_status"),(String)job.get("next_status"));
       if(ok) jdbc.update("update notification_outbox set sent_at=now(),last_error=null where id=?",id);
       else jdbc.update("""
         update notification_outbox set attempts=attempts+1,last_error='Email provider unavailable or rejected',
         next_attempt_at=now() + (least(3600,power(2,least(attempts,10))::integer*60)*interval '1 second') where id=?
         """,id);
       return true;
     });
     if(!Boolean.TRUE.equals(processed))break;
   }
 }
}
