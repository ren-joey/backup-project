package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.Operation.UPDATE;
import static com.delta.dms.community.swagger.model.PermissionObject.CONCLUSION;
import static com.delta.dms.community.swagger.model.ReplyOperation.CONCLUSIONEDIT;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_UPDATE_CONCLUSION;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_UPDATE_REPLY;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.ReplyOperation;
import com.delta.dms.community.swagger.model.TopicType;

@Component
public class UpdateConclusionValidator extends BasePrivilegeValidator {

  protected UpdateConclusionValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
  }

  @Override
  public ReplyOperation getReplyOperation() {
    return CONCLUSIONEDIT;
  }

  @Override
  Operation getPrivilegeOperation() {
    return UPDATE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_UPDATE_CONCLUSION;
  }

  @Override
  PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum) {
    return CONCLUSION;
  }

  @Override
  void validateExtraReplyStatus(TopicInfo topic, ReplyInfo reply) {
    if (!isConclusion(reply)) {
      throw new IllegalArgumentException(MSG_CANNOT_UPDATE_REPLY);
    }
    if (isSystemTopic(TopicType.fromValue(topic.getTopicType()))) {
      throw new UnauthorizedException(getErrorMessage());
    }
  }

  private boolean isSystemTopic(TopicType topicType) {
    switch (topicType) {
      case GENERAL:
      case PROBLEM:
        return false;
      default:
        return true;
    }
  }
}
