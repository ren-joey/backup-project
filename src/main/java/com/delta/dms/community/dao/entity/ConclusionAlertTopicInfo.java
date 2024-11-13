package com.delta.dms.community.dao.entity;

import com.delta.dms.community.swagger.model.TopicState;
import com.delta.dms.community.swagger.model.TopicType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ConclusionAlertTopicInfo {
  private int topicId;
  private int forumId;
  private String topicTitle;
  private TopicState topicState;
  private int duration;
  private String forumName;
  private TopicType topicType;
  private String topicText;
  private String factory;
  private String communityName;
}
