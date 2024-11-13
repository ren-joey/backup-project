package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class TopicDeletingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int communityId;
  private int forumId;
  private int topicId;
  private String userId;
  private long time;

  public TopicDeletingEvent(
      Object source, int communityId, int forumId, int topicId, String userId, long time) {
    super(source);
    this.communityId = communityId;
    this.forumId = forumId;
    this.topicId = topicId;
    this.userId = userId;
    this.time = time;
  }

  public int getCommunityId() {
    return communityId;
  }

  public int getForumId() {
    return forumId;
  }

  public int getTopicId() {
    return topicId;
  }

  public String getUserId() {
    return userId;
  }

  public long getTime() {
    return time;
  }
}
