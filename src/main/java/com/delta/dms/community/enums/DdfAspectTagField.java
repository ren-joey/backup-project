package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DdfAspectTagField {
  COMMUNITY_ID("communityId"),

  COMMUNITY("community"),
  
  COMMUNITY_EN("communityEname"),

  COMMUNITY_TYPE("communityType"),

  COMMUNITY_CATEGORY("communityCategory"),

  COMMUNITY_STATUS("communityStatus"),

  FORUM_ID("forumId"),

  FORUM("forum"),

  FORUM_TYPE("forumType"),

  FORUM_STATUS("forumStatus"),

  TOPIC_ID("topicId"),

  TOPIC("topic"),

  TOPIC_TYPE("topicType"),

  TOPIC_STATUS("topicStatus"),

  TOPIC_SITUATION("topicSituation"),

  TOPIC_CONCLUSION_STATE("topicConclusionState"),

  APP_FIELD("appField");

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
