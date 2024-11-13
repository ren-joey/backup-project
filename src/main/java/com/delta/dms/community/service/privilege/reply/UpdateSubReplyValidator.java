package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.ReplyOperation.REPLYCHILDEDIT;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.ReplyOperation;

@Component
public class UpdateSubReplyValidator extends UpdateReplyValidator {

  protected UpdateSubReplyValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao,
      ReplyDao replyDao) {
    super(communityService, forumService, privilegeService, topicDao, replyDao);
  }

  @Override
  public ReplyOperation getReplyOperation() {
    return REPLYCHILDEDIT;
  }
}
