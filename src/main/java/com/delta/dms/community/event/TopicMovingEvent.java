package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class TopicMovingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int forumId;
  private int communityId;
  private String userId;
  private long time;

  public TopicMovingEvent(Object source, int forumId, int communityId, String userId, long time) {
    super(source);
    this.forumId = forumId;
    this.communityId = communityId;
    this.userId = userId;
    this.time = time;
  }

  public int getForumId() {
    return forumId;
  }

  public void setForumId(int forumId) {
    this.forumId = forumId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }
}
