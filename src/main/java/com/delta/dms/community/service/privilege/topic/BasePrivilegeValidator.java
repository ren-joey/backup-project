package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.TopicSituation.SEALED;
import static com.delta.dms.community.swagger.model.TopicStatus.LOCKED;
import static com.delta.dms.community.utils.I18nConstants.MSG_COMMUNITY_LOCKED;
import static com.delta.dms.community.utils.I18nConstants.MSG_TOPIC_SEALED;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.Collections;
import java.util.Set;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.TopicOperation;
import com.delta.dms.community.swagger.model.TopicSituation;
import com.delta.dms.community.swagger.model.TopicStatus;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.utils.Utility;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BasePrivilegeValidator {

  private final CommunityService communityService;
  final ForumService forumService;
  private final PrivilegeService privilegeService;
  private final TopicDao topicDao;

  public abstract TopicOperation getTopicOperation();

  abstract Operation getPrivilegeOperation();

  abstract boolean allowCreatorOperate();

  abstract String getErrorMessage();

  abstract PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum);

  abstract void validateExtraTopicStatus(TopicInfo topic);

  public void validatePrivilege(int topicId) {
    TopicInfo topic = topicDao.getTopicInfoById(topicId);
    validateTopicStatus(topic);
    ForumInfo forum = forumService.getForumInfoById(topic.getForumId());
    CommunityInfo community = communityService.getCommunityInfoById(forum.getCommunityId());
    PermissionObject permissionObject = getPermissionObject(community, forum);
    if (!privilegeService.checkUserPrivilege(
            Utility.getCurrentUserIdWithGroupId(),
            forum.getCommunityId(),
            forum.getForumId(),
            permissionObject.toString(),
            getPrivilegeOperation().toString())
        && (!allowCreatorOperate()
            || !isValidCreator(
                CommunityType.fromValue(community.getCommunityType()),
                forum,
                topic.getTopicCreateUserId()))) {
      throw new UnauthorizedException(getErrorMessage());
    }
  }

  void validateTopicLockStatus(TopicInfo topic) {
    // Topic is locked
    if (SEALED == TopicSituation.fromValue(topic.getTopicSituation())) {
      throw new UnauthorizedException(MSG_TOPIC_SEALED);
    }
  }

  private void validateTopicStatus(TopicInfo topic) {
    // Community is locked
    if (LOCKED == TopicStatus.fromValue(topic.getTopicStatus())) {
      throw new UnauthorizedException(MSG_COMMUNITY_LOCKED);
    }
    validateExtraTopicStatus(topic);
  }

  private boolean isValidCreator(CommunityType communityType, ForumInfo forum, String creatorId) {
    if (PRIVATE == ForumType.fromValue(forum.getForumType()) || ACTIVITY != communityType) {
      // Must be forum member
      Set<String> forumMembers =
          forumService.getMemberOfForumWithFilters(false, forum.getForumId(), -1, -1,
                      EMPTY, Collections.singletonList(Utility.getUserIdFromSession()), null, EMPTY)
              .stream()
              .map(User::getId)
              .collect(toSet());
      if (!forumMembers.contains(Utility.getUserIdFromSession())) {
        return false;
      }
    }
    return Utility.checkUserIsAuthor(creatorId, Utility.getUserIdFromSession());
  }
}
