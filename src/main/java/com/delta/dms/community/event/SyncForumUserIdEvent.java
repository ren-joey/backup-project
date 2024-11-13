package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class SyncForumUserIdEvent extends ApplicationEvent {
  private static final long serialVersionUID = 1L;
  private int communityId;
  private String groupId;

  public SyncForumUserIdEvent(Object source, int communityId, String groupId) {
    super(source);
    this.communityId = communityId;
    this.groupId = groupId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
}
