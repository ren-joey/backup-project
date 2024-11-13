package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.Operation.UPDATE;
import static com.delta.dms.community.swagger.model.ReplyOperation.REPLYEDIT;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_UPDATE_CONCLUSION;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_UPDATE_REPLY;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.ReplyOperation;

@Component
public class UpdateReplyValidator extends BaseReplyPrivilegeValidator {

  protected UpdateReplyValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
  }

  @Override
  public ReplyOperation getReplyOperation() {
    return REPLYEDIT;
  }

  @Override
  Operation getPrivilegeOperation() {
    return UPDATE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_UPDATE_REPLY;
  }

  @Override
  void validateExtraReplyStatus(TopicInfo topic, ReplyInfo reply) {
    if (isConclusion(reply)) {
      throw new IllegalArgumentException(MSG_CANNOT_UPDATE_CONCLUSION);
    }
  }
}
