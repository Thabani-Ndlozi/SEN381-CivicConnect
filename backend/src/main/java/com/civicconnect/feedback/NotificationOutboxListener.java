package com.civicconnect.feedback;

import com.civicconnect.request.application.event.RequestStatusChangedEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Enqueue only committed status transitions; never block request updates on email delivery. */
@Component
public class NotificationOutboxListener {
 private static final Logger log=LoggerFactory.getLogger(NotificationOutboxListener.class);
 private final JdbcTemplate jdbc;
 public NotificationOutboxListener(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void queue(RequestStatusChangedEvent e) {
   try {
     jdbc.update("""
      insert into notification_outbox(id,request_id,recipient_email,previous_status,next_status,request_title)
      values (?,?,?,?,?,?)
      """,UUID.randomUUID(),e.requestId(),e.requesterId(),e.fromStatus().name(),e.toStatus().name(),e.title());
   } catch(Exception ex){log.error("Notification outbox insert failed for request {}",e.requestId(),ex);}
 }
}
