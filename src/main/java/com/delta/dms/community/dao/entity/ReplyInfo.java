package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class ReplyInfo {
  private int replyId;
  private int forumId = 0;
  private int followTopicId = 0;
  private int followReplyId = 0;
  private String replyStatus = "";
  private String replyCreateUserId = "";
  private long replyCreateTime = 0;
  private String replyModifiedUserId = "";
  private long replyModifiedTime = 0;
  private String replyDeleteUserId = "";
  private long replyDeleteTime = 0;
  private int replyIndex = 0;
  private String replyText = "";
  private String replyConclusionText = "";
  private String replyRespondee = "";
  private String medalFrame = "";
  private int medalId;
  private String medalTitle = "";
  private String forumName = "";

  @Override
  public String toString() {
    return "ReplyInfo [replyId="
        + replyId
        + ", forumId="
        + forumId
        + ", followTopicId="
        + followTopicId
        + ", followReplyId="
        + followReplyId
        + ", replyStatus="
        + replyStatus
        + ", replyCreateUserId="
        + replyCreateUserId
        + ", replyCreateTime="
        + replyCreateTime
        + ", replyModifiedUserId="
        + replyModifiedUserId
        + ", replyModifiedTime="
        + replyModifiedTime
        + ", replyDeleteUserId="
        + replyDeleteUserId
        + ", replyDeleteTime="
        + replyDeleteTime
        + ", replyIndex="
        + replyIndex
        + ", replyText="
        + replyText
        + ", replyConclusionText="
        + replyConclusionText
        + ", replyRespondee="
        + replyRespondee
        + "]";
  }
}
