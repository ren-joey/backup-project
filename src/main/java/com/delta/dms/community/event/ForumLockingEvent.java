package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class ForumLockingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int forumId;
  private String userId;
  private long lockedTime;

  public ForumLockingEvent(Object source, int forumId, String userId, long lockedTime) {
    super(source);
    this.forumId = forumId;
    this.userId = userId;
    this.lockedTime = lockedTime;
  }

  public int getForumId() {
    return forumId;
  }

  public String getUserId() {
    return userId;
  }

  public long getLockedTime() {
    return lockedTime;
  }
}
