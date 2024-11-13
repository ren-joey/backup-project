package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.Operation.MOVE;
import static com.delta.dms.community.swagger.model.TopicOperation.TOPICMOVE;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_MOVE_TOPIC;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.TopicOperation;

@Component
public class MoveTopicValidator extends CustomTopicValidator {

  public MoveTopicValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return TOPICMOVE;
  }

  @Override
  Operation getPrivilegeOperation() {
    return MOVE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_MOVE_TOPIC;
  }
}
