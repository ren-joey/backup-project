package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.swagger.model.CommunityType.ACTIVITY;
import static com.delta.dms.community.swagger.model.ForumType.PRIVATE;
import static com.delta.dms.community.swagger.model.ReplyStatus.LOCKED;
import static com.delta.dms.community.swagger.model.TopicSituation.SEALED;
import static com.delta.dms.community.utils.I18nConstants.MSG_COMMUNITY_LOCKED;
import static com.delta.dms.community.utils.I18nConstants.MSG_TOPIC_SEALED;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.util.Collections;
import java.util.Set;
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
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.ReplyOperation;
import com.delta.dms.community.swagger.model.ReplyStatus;
import com.delta.dms.community.swagger.model.TopicSituation;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.utils.Utility;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BasePrivilegeValidator {

  private final CommunityService communityService;
  final ForumService forumService;
  private final PrivilegeService privilegeService;
  private final TopicDao topicDao;
  private final ReplyDao replyDao;

  public abstract ReplyOperation getReplyOperation();

  abstract Operation getPrivilegeOperation();

  abstract String getErrorMessage();

  abstract PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum);

  abstract void validateExtraReplyStatus(TopicInfo topic, ReplyInfo reply);

  public void validatePrivilege(int replyId) {
    ReplyInfo reply = replyDao.getReplyInfoById(replyId);
    TopicInfo topic = topicDao.getTopicInfoById(reply.getFollowTopicId());
    validateReplyStatus(topic, reply);
    ForumInfo forum = forumService.getForumInfoById(topic.getForumId());
    CommunityInfo community = communityService.getCommunityInfoById(forum.getCommunityId());
    PermissionObject permissionObject = getPermissionObject(community, forum);
    if (!privilegeService.checkUserPrivilege(
            Utility.getCurrentUserIdWithGroupId(),
            forum.getCommunityId(),
            forum.getForumId(),
            permissionObject.toString(),
            getPrivilegeOperation().toString())
        && !isValidCreator(
            CommunityType.fromValue(community.getCommunityType()),
            forum,
            reply.getReplyCreateUserId())) {
      throw new UnauthorizedException(getErrorMessage());
    }
  }

  boolean isConclusion(ReplyInfo reply) {
    // Index 0 indicates that the reply is conclusion
    return INTEGER_ZERO == reply.getReplyIndex() && INTEGER_ZERO == reply.getFollowReplyId();
  }

  private void validateReplyStatus(TopicInfo topic, ReplyInfo reply) {
    // Community is locked
    if (LOCKED == ReplyStatus.fromValue(reply.getReplyStatus())) {
      throw new UnauthorizedException(MSG_COMMUNITY_LOCKED);
    }
    // Topic is locked
    if (SEALED == TopicSituation.fromValue(topic.getTopicSituation())) {
      throw new UnauthorizedException(MSG_TOPIC_SEALED);
    }
    validateExtraReplyStatus(topic, reply);
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
