package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.Operation.UPDATE;
import static com.delta.dms.community.swagger.model.TopicOperation.TOPICLOCKED;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_SEAL_TOPIC;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.TopicOperation;

/**
 * Lock Topic = set topic_situation as sealed
 *
 * @author LENA.KE
 */
@Component
public class LockTopicValidator extends BaseTopicPrivilegeValidator {

  public LockTopicValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return TOPICLOCKED;
  }

  @Override
  Operation getPrivilegeOperation() {
    return UPDATE;
  }

  @Override
  boolean allowCreatorOperate() {
    return true;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_SEAL_TOPIC;
  }

  @Override
  void validateExtraTopicStatus(TopicInfo topic) {
    // Do nothing
  }
}
