package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class TopicChangingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private int topicId;
  private int forumId;
  private int communityId;
  private String userId;
  private long time;

  public TopicChangingEvent(
      Object source, int topicId, int forumId, int communityId, String userId, long time) {
    super(source);
    this.topicId = topicId;
    this.forumId = forumId;
    this.communityId = communityId;
    this.userId = userId;
    this.time = time;
  }

  public int getTopicId() {
    return topicId;
  }

  public int getForumId() {
    return forumId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public String getUserId() {
    return userId;
  }

  public long getTime() {
    return time;
  }
}
