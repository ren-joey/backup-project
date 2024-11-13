package com.delta.dms.community.service.privilege.topic;

import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.TopicOperation;
import org.springframework.stereotype.Component;

import static com.delta.dms.community.swagger.model.Operation.UPDATE;
import static com.delta.dms.community.swagger.model.TopicOperation.TOPICNOTIFY;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_NOTIFY_TOPIC;

@Component
public class NotifyTopicValidator extends CustomTopicValidator {

  public NotifyTopicValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return TOPICNOTIFY;
  }

  @Override
  Operation getPrivilegeOperation() {
    return UPDATE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_NOTIFY_TOPIC;
  }
}