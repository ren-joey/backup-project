package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class CommunityAvatarChangingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;

  public CommunityAvatarChangingEvent(Object source, int communityId) {
    super(source);
    this.communityId = communityId;
  }

  public int getCommunityId() {
    return communityId;
  }
}
