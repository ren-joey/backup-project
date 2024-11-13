package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.dao.entity.ActivityLogMsg;

public class ActivityLogMsgEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private ActivityLogMsg activityLogMsg;

  public ActivityLogMsgEvent(Object source, ActivityLogMsg activityLogMsg) {
    super(source);
    this.activityLogMsg = activityLogMsg;
  }

  public ActivityLogMsg getActivityLogMsg() {
    return activityLogMsg;
  }
}
