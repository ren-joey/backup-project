package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.swagger.model.ActivityLogData;

public class ActivityLogEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private ActivityLogData activityLogData;

  public ActivityLogEvent(Object source, ActivityLogData activityLogData) {
    super(source);
    this.activityLogData = activityLogData;
  }

  public ActivityLogData getActivityLogData() {
    return activityLogData;
  }
}
