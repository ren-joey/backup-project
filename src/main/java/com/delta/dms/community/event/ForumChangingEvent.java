package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class ForumChangingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;
  private int forumId;
  private String userId;
  private long time;
  private boolean toUpdateDdf;

  public ForumChangingEvent(Object source, int communityId, int forumId, String userId, long time, boolean toUpdateDdf) {
    super(source);
    this.communityId = communityId;
    this.forumId = forumId;
    this.userId = userId;
    this.time = time;
    this.toUpdateDdf = toUpdateDdf;
  }

  public int getCommunityId() {
    return communityId;
  }

  public int getForumId() {
    return forumId;
  }

  public String getUserId() {
    return userId;
  }

  public long getTime() {
    return time;
  }

  public boolean getToUpdateDdf() {
    return toUpdateDdf;
  }
}
