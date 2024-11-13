package com.delta.dms.community.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.delta.dms.community.event.EmailSendingEvent;
import com.delta.dms.community.service.EmailService;
import com.delta.set.utils.LogUtil;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailEventListener {

  private static final LogUtil log = LogUtil.getInstance();
  private EmailService emailService;

  @Autowired
  public EmailEventListener(EmailService emailService) {
    this.emailService = emailService;
  }

  @Async
  @TransactionalEventListener
  public void handleEmailSendingEvent(EmailSendingEvent event) {
    log.debug("Sending the mail");
    emailService.sendEmail(event.getTemplateType(), event.getEmailWithChineseAndEnglishContext());
  }
}
