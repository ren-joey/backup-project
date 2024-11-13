package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class CommunityChangingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;

  public CommunityChangingEvent(Object source, int communityId) {
    super(source);
    this.communityId = communityId;
  }

  public int getCommunityId() {
    return communityId;
  }
}
