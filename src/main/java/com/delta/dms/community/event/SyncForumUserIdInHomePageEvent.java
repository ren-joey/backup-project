package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class SyncForumUserIdInHomePageEvent extends ApplicationEvent {
  private static final long serialVersionUID = 1L;
  private int communityId;
  private int forumId;
  private String groupId;

  public SyncForumUserIdInHomePageEvent(
      Object source, int communityId, int forumId, String groupId) {
    super(source);
    this.communityId = communityId;
    this.forumId = forumId;
    this.groupId = groupId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public int getForumId() {
    return forumId;
  }

  public void setForumId(int forumId) {
    this.forumId = forumId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
}
