package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.Operation.PIN;
import static com.delta.dms.community.swagger.model.TopicOperation.TOPICSETTOPPING;
import static com.delta.dms.community.utils.I18nConstants.MSG_TOPIC_NOT_AUTHORIZED;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.TopicOperation;

@Component
public class PinTopicValidator extends BaseTopicPrivilegeValidator {

  public PinTopicValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return TOPICSETTOPPING;
  }

  @Override
  Operation getPrivilegeOperation() {
    return PIN;
  }

  @Override
  boolean allowCreatorOperate() {
    return false;
  }

  @Override
  String getErrorMessage() {
    return MSG_TOPIC_NOT_AUTHORIZED;
  }

  @Override
  void validateExtraTopicStatus(TopicInfo topic) {
    // Do nothing
  }
}
