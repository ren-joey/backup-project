package com.delta.dms.community.event;

import com.delta.dms.community.utils.Utility;
import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.swagger.model.EmailWithChineseAndEnglishContext;
import com.delta.dms.community.swagger.model.TemplateType;

public class EmailSendingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private EmailWithChineseAndEnglishContext context;
  private TemplateType templateType = TemplateType.BOTHMAIL;

  public EmailSendingEvent(Object source, EmailWithChineseAndEnglishContext context) {
    super(source);
    context.setCreator(Utility.getUserIdFromSession());
    this.context = context;
  }

  public EmailSendingEvent(
      Object source, EmailWithChineseAndEnglishContext context, TemplateType templateType) {
    super(source);
    this.context = context;
    this.templateType = templateType;
  }

  public EmailWithChineseAndEnglishContext getEmailWithChineseAndEnglishContext() {
    return context;
  }

  public TemplateType getTemplateType() {
    return templateType;
  }
}
