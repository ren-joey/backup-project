package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class CommunityReopeningEvent extends ApplicationEvent {
  private static final long serialVersionUID = 1L;
  private int communityId;
  private String userId;
  private long reopenedTime;

  public CommunityReopeningEvent(Object source, int communityId, String userId, long reopenedTime) {
    super(source);
    this.communityId = communityId;
    this.userId = userId;
    this.reopenedTime = reopenedTime;
  }

  public int getCommunityId() {
    return communityId;
  }

  public String getUserId() {
    return userId;
  }

  public long getReopenedTime() {
    return reopenedTime;
  }
}
