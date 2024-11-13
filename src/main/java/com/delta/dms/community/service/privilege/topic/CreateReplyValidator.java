package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.Operation.CREATE;
import static com.delta.dms.community.swagger.model.PermissionObject.ACTIVEREPLY;
import static com.delta.dms.community.swagger.model.PermissionObject.PRIVATEFORUMREPLY;
import static com.delta.dms.community.swagger.model.PermissionObject.PUBLICFORUMREPLY;
import static com.delta.dms.community.swagger.model.TopicOperation.REPLYCREATE;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_CREATE_REPLY;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.TopicOperation;

@Component
public class CreateReplyValidator extends BasePrivilegeValidator {

  public CreateReplyValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return REPLYCREATE;
  }

  @Override
  Operation getPrivilegeOperation() {
    return CREATE;
  }

  @Override
  boolean allowCreatorOperate() {
    return false;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_CREATE_REPLY;
  }

  @Override
  PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum) {
    if (PRIVATE == ForumType.fromValue(forum.getForumType())) {
      return PRIVATEFORUMREPLY;
    }
    if (ACTIVITY == CommunityType.fromValue(community.getCommunityType())) {
      return ACTIVEREPLY;
    }
    return PUBLICFORUMREPLY;
  }

  @Override
  void validateExtraTopicStatus(TopicInfo topic) {
    validateTopicLockStatus(topic);
  }
}
