package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class ForumReopeningEvent extends ApplicationEvent {
  private static final long serialVersionUID = 1L;
  private int forumId;
  private String userId;
  private long reopenedTime;

  public ForumReopeningEvent(Object source, int forumId, String userId, long reopenedTime) {
    super(source);
    this.forumId = forumId;
    this.userId = userId;
    this.reopenedTime = reopenedTime;
  }

  public int getForumId() {
    return forumId;
  }

  public String getUserId() {
    return userId;
  }

  public long getReopenedTime() {
    return reopenedTime;
  }
}
