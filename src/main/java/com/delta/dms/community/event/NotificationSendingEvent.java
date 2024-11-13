package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.swagger.model.Notification;

public class NotificationSendingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private Notification notification;

  public NotificationSendingEvent(Object source, Notification notification) {
    super(source);
    this.notification = notification;
  }

  public Notification getNotification() {
    return notification;
  }
}
