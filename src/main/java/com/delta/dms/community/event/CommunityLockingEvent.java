package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class CommunityLockingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;
  private String userId;
  private long lockedTime;

  public CommunityLockingEvent(Object source, int communityId, String userId, long lockedTime) {
    super(source);
    this.communityId = communityId;
    this.userId = userId;
    this.lockedTime = lockedTime;
  }

  public int getCommunityId() {
    return communityId;
  }

  public String getUserId() {
    return userId;
  }

  public long getLockedTime() {
    return lockedTime;
  }
}
