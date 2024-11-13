package com.delta.dms.community.dao.entity;

import java.util.List;
import com.delta.dms.community.swagger.model.User;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TopicInfo {
  private int topicId;
  private int forumId = 0;
  private String forumName = "";
  private String topicTitle = "";
  private String topicType = "";
  private String topicState = "";
  private String topicStatus = "";
  private String topicSituation = "";
  private String topicCreateUserId = "";
  private long topicCreateTime = 0;
  private String topicModifiedUserId = "";
  private long topicModifiedTime = 0;
  private String topicDeleteUserId = "";
  private long topicDeleteTime = 0;
  private int topicViewCount = 0;
  private String topicLastModifiedUserId = "";
  private long topicLastModifiedTime = 0;
  private String topicText = "";
  private String topicDdfId = "";
  private int topicToppingOrder = 0;
  private boolean showState = false;
  private String conclusion = "";
  private long conclusionModifiedTime = 0;
  private List<String> appFieldList;

  private int communityId;
  private String communityGid = "";
  private String communityName = "";
  private String communityCategory = "";
  private String communityType = "";
  private int numberOfReply;
  private String notificationType;
  private String topicCreateUserName;
  private List<TopicTypeEntity> forumSupportTopicType;
  private List<User> recipient;
  private boolean archiveConclusionAttachment;
}
