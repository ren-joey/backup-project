package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.enums.ConclusionState.CONCLUDED;
import static com.delta.dms.community.swagger.model.Operation.CREATE;
import static com.delta.dms.community.swagger.model.PermissionObject.CONCLUSION;
import static com.delta.dms.community.swagger.model.TopicOperation.CONCLUSIONCREATE;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_CREATE_CONCLUSION;
import static com.delta.dms.community.utils.I18nConstants.MSG_CONCLUSION_HAS_EXISTED;
import org.springframework.stereotype.Component;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.TopicOperation;
import com.delta.dms.community.swagger.model.TopicType;

@Component
public class CreateConclusionValidator extends BasePrivilegeValidator {

  public CreateConclusionValidator(
      CommunityService communityService,
      ForumService forumService,
      PrivilegeService privilegeService,
      TopicDao topicDao) {
    super(communityService, forumService, privilegeService, topicDao);
  }

  @Override
  public TopicOperation getTopicOperation() {
    return CONCLUSIONCREATE;
  }

  @Override
  Operation getPrivilegeOperation() {
    return CREATE;
  }

  @Override
  String getErrorMessage() {
    return MSG_CANNOT_CREATE_CONCLUSION;
  }

  @Override
  boolean allowCreatorOperate() {
    return true;
  }

  @Override
  PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum) {
    return CONCLUSION;
  }

  @Override
  void validateExtraTopicStatus(TopicInfo topic) {
    validateTopicLockStatus(topic);
    if (!allowConclusion(topic)) {
      throw new UnauthorizedException(getErrorMessage());
    }
    if (CONCLUDED == ConclusionState.fromValue(topic.getTopicState())) {
      throw new UnauthorizedException(MSG_CONCLUSION_HAS_EXISTED);
    }
  }

  private boolean allowConclusion(TopicInfo topic) {
    return TopicType.EERPMSUMMARY != TopicType.fromValue(topic.getTopicType());
  }
}
