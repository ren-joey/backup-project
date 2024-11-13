package com.delta.dms.community.dao.entity;

import java.util.List;
import java.util.Objects;

import com.delta.dms.community.swagger.model.ForumType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForumInfo {
  private int forumId;
  private int communityId = 0;
  private String communityName = "";
  private String forumType = "";
  private String forumName = "";
  private String forumDesc = "";
  private String forumImgAvatar = "";
  private String forumStatus = "";
  private String forumCreateUserId = "";
  private long forumCreateTime = 0;
  private String forumModifiedUserId = "";
  private long forumModifiedTime = 0;
  private String forumDeleteUserId = "";
  private long forumDeleteTime = 0;
  private String forumLastModifiedUserId = "";
  private long forumLastModifiedTime = 0;
  private String forumLastTopicId = "";
  private String forumDdfId = "";
  private int forumToppingOrder = 0;
  private List<TopicTypeEntity> supportTopicType;
  private boolean conclusionAlert;

  public boolean isPublicForum() {
    return Objects.equals(this.forumType, ForumType.PUBLIC.toString());
  }
}
