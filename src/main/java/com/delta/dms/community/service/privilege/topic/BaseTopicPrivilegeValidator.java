package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.PermissionObject.ACTIVETOPIC;
import static com.delta.dms.community.swagger.model.PermissionObject.PRIVATEFORUMTOPIC;
import static com.delta.dms.community.swagger.model.PermissionObject.PUBLICFORUMTOPIC;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.PermissionObject;

public abstract class BaseTopicPrivilegeValidator extends BasePrivilegeValidator {

  protected BaseTopicPrivilegeValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum) {
    if (PRIVATE == ForumType.fromValue(forum.getForumType())) {
      return PRIVATEFORUMTOPIC;
    }
    if (ACTIVITY == CommunityType.fromValue(community.getCommunityType())) {
      return ACTIVETOPIC;
    }
    return PUBLICFORUMTOPIC;
  }
}
