package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class CommunityDeletingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;
  private String communityDdfId;

  public CommunityDeletingEvent(Object source, int communityId, String communityDdfId) {
    super(source);
    this.communityId = communityId;
    this.communityDdfId = communityDdfId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public String getCommunityDdfId() {
    return communityDdfId;
  }
}
