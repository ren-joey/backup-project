package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.Operation.DELETE;
import static com.delta.dms.community.swagger.model.ReplyOperation.REPLYDELETE;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_DELETE_CONCLUSION;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_DELETE_REPLY;
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
public class DeleteReplyValidator extends BaseReplyPrivilegeValidator {

  protected DeleteReplyValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
  }

  @Override
  public ReplyOperation getReplyOperation() {
    return REPLYDELETE;
  }

  @Override
  Operation getPrivilegeOperation() {
    return DELETE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_DELETE_REPLY;
  }

  @Override
  void validateExtraReplyStatus(TopicInfo topic, ReplyInfo reply) {
    if (isConclusion(reply)) {
      throw new IllegalArgumentException(MSG_CANNOT_DELETE_CONCLUSION);
    }
  }
}
