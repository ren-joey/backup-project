package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.Operation.CREATE;
import static com.delta.dms.community.swagger.model.PermissionObject.*;
import static com.delta.dms.community.swagger.model.ReplyOperation.REPLYCHILDCREATE;
import static com.delta.dms.community.swagger.model.ReplyOperation.REPLYCHILDEDIT;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_CREATE_REPLY;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_UPDATE_CONCLUSION;

import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.privilege.topic.CreateReplyValidator;
import com.delta.dms.community.swagger.model.*;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;

@Component
public class CreateSubReplyValidator extends BaseReplyPrivilegeValidator {

  public CreateSubReplyValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
  }

  @Override
  public ReplyOperation getReplyOperation() {
    return REPLYCHILDCREATE;
  }

  @Override
  Operation getPrivilegeOperation() {
    return CREATE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_CREATE_REPLY;
  }

  @Override
  void validateExtraReplyStatus(TopicInfo topic, ReplyInfo reply) {
    if (isConclusion(reply)) {
      throw new IllegalArgumentException(MSG_CANNOT_UPDATE_CONCLUSION);
    }
  }
}
