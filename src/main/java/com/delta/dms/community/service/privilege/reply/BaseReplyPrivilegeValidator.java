package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.PermissionObject.ACTIVEREPLY;
import static com.delta.dms.community.swagger.model.PermissionObject.PRIVATEFORUMREPLY;
import static com.delta.dms.community.swagger.model.PermissionObject.PUBLICFORUMREPLY;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.PermissionObject;

public abstract class BaseReplyPrivilegeValidator extends BasePrivilegeValidator {

  protected BaseReplyPrivilegeValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
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
}
