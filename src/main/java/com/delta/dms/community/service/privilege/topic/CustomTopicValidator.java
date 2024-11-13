package com.delta.dms.community.service.privilege.topic;

import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.TopicType;

public abstract class CustomTopicValidator extends BaseTopicPrivilegeValidator {

  protected CustomTopicValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  boolean allowCreatorOperate() {
    return true;
  }

  @Override
  void validateExtraTopicStatus(TopicInfo topic) {
    validateTopicLockStatus(topic);
    if (!isCustomTopic(TopicType.fromValue(topic.getTopicType()))) {
      throw new UnauthorizedException(getErrorMessage());
    }
  }

  private boolean isCustomTopic(TopicType topicType) {
    switch (topicType) {
      case GENERAL:
      case PROBLEM:
      case EERPMMANUAL:
        return true;
      default:
        return false;
    }
  }
}
