package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class ForumDeletingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;
  private int forumId;
  private String userId;
  private long time;
  private String forumDdfId;

  public ForumDeletingEvent(
      Object source, int communityId, int forumId, String userId, long time, String forumDdfId) {
    super(source);
    this.communityId = communityId;
    this.forumId = forumId;
    this.userId = userId;
    this.time = time;
    this.forumDdfId = forumDdfId;
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

  public String getForumDdfId() {
    return forumDdfId;
  }
}
