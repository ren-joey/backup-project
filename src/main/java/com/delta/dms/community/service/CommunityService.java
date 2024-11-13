package com.delta.dms.community.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import com.delta.dms.community.dao.ForumDao;
import com.delta.dms.community.dao.entity.*;
import com.delta.dms.community.swagger.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.dms.community.adapter.NotificationV2Adapter;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.bean.notification.v2.NotificationV2Entities;
import com.delta.dms.community.bean.notification.v2.NotificationV2Link;
import com.delta.dms.community.bean.notification.v2.NotificationV2Request;
import com.delta.dms.community.bean.notification.v2.NotificationV2Variable;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.CommunityDao;
import com.delta.dms.community.enums.CommunityMemberTypeEnum;
import com.delta.dms.community.enums.MedalIdType;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.exception.CreationException;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.service.member.CollectMemberContext;
import com.delta.dms.community.service.member.CollectMemberService;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.GroupConstants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.NotificationConstants;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class CommunityService {

  private static final String MSG_FORUM_EXCHANGE_AREA = "公開討論區 (Public Area)";
  private static final String OPEN_PARENTHESIS = "(";
  private static final String CLOSE_PARENTHESIS = ")";
  private static final String CLOSE_PARENTHESIS_REGEXR = "\\)";
  private static final String LOCKED_ATTACHED_COMMUNITY_NAME_FORMAT = "%s(封存)";
  private static final String CLOSED_GENERAL_COMMUNITY_NAME_FORMAT = "%s(Closed)";
  private static final String REOPENED_GENERAL_COMMUNITY_NAME_REGEX = "\\(Closed\\)";
  private static final String REOPENED_GENERAL_COMMUNITY_NAME_REGEX_REPLACEMENT = "";
  public static final Predicate<CommunityMember> COMMUNITY_ADMIN_PREDICATE =
      c -> c.getRoleId() == Role.COMMUNITY_ADMIN.getId();
  public static final Predicate<CommunityMember> COMMUNITY_MANAGER_PREDICATE =
      c -> c.getRoleId() == Role.COMMUNITY_MEMBER.getId();

  private final CommunityDao communityDao;
  private final ForumDao forumDao;
  private final UserService userService;
  private final PrivilegeService privilegeService;
  private final EventPublishService eventPublishService;
  private final UserGroupAdapter userGroupAdapter;
  private final RichTextHandlerService richTextHandlerService;
  private final FileService fileService;
  private final CollectMemberService collectMemberService;
  private final GroupRecipientHandleService groupRecipientHandleService;
  private final NotificationV2Adapter notificationV2Adapter;
  private final MedalService medalService;
  private final AuthService authService;

  private final YamlConfig yamlConfig;
  private static int avatarWidth = 256;
  private static int avatarHeight = 256;
  private static int bannerWidth = 980;
  private static int bannerHeight = 100;
  private static final Base64.Decoder decoder = Base64.getDecoder();
  private static final Base64.Encoder encoder = Base64.getEncoder();
  private static final int MAX_COMMUNITY_DESC = 255;

  public CommunitySearchResult getOpenCommunityByCategoryAndScope(
      CommunityCategory category,
      SearchScope scope,
      int offset,
      int limit,
      Order sort,
      String userId) {
    CommunitySearchRequestEntity searchRequest =
        setSearchRequestInfo(
            new CommunitySearchRequestEntity()
                .setCategory(ofNullable(category).orElse(CommunityCategory.ALL).toString())
                .setScope(scope)
                .setOffset(offset)
                .setLimit(limit)
                .setSort(sort)
                .setUserId(userId)
                .setExcludeStatusList(
                    Arrays.asList(
                        CommunityStatus.DELETE.toString(),
                        CommunityStatus.LOCKED.toString(),
                        CommunityStatus.CLOSED.toString())));
    List<CommunityInfo> communityInfoList = communityDao.getCommunityByCategory(searchRequest);
    List<CommunityListDetail> communityListDetailList =
        setCommunityListDetail(
            communityInfoList, SearchScope.MINEADMIN.equals(scope) || !userId.isEmpty());
    CommunitySearchResult communitySearchResult = new CommunitySearchResult();
    communitySearchResult.setResult(communityListDetailList);
    communitySearchResult.setNumFound(
        countCommunityCategoryByScope(scope, userId)
            .get(
                SearchScope.MINEADMIN.equals(scope)
                    ? CommunityCategory.ALL.toString()
                    : category.toString()));
    return communitySearchResult;
  }

  private String getUserIdWithoutGroupId(String userId) {
    if (userId.isEmpty()) {
      return Utility.getUserIdFromSession();
      //return Utility.getCurrentUserIdWithGroupId();
    } else {
      return userId;
      /*
      List<String> groupIdList = userGroupAdapter.getBelongGroupIdOfUser(userId);
      if (groupIdList.isEmpty()) {
        return userId;
      } else {
        return new StringBuilder(userId)
            .append(Constants.COMMA_DELIMITER)
            .append(groupIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
            .toString();
      }
       */
    }
  }

  private CommunityFieldName getSortField(String sortField) {
    if (SortField.UPDATETIME.toString().equals(sortField)) {
      return CommunityFieldName.LAST_MODIFIED_TIME;
    } else if (SortField.NAME.toString().equals(sortField)) {
      return CommunityFieldName.NAME;
    }
    return CommunityFieldName.LAST_MODIFIED_TIME;
  }

  private List<CommunityListDetail> setCommunityListDetail(
      List<CommunityInfo> communityInfoList, boolean onlyIdAndName) {
    if (onlyIdAndName) {
      return communityInfoList
          .stream()
          .map(
              item ->
                  new CommunityListDetail().id(item.getCommunityId()).name(item.getCommunityName()))
          .collect(Collectors.toList());
    } else {
      return communityInfoList
          .stream()
          .map(
              item ->
                  new CommunityListDetail()
                      .id(item.getCommunityId())
                      .name(item.getCommunityName())
                      .desc(item.getCommunityDesc())
                      .category(item.getCommunityCategory())
                      .imgAvatar(item.getCommunityImgAvatar())
                      .lastModifiedTime(item.getCommunityLastModifiedTime())
                      .lastModifiedUser(
                          userService.getUserById(item.getCommunityLastModifiedUserId()))
                      .member(
                          getAllMemberOfCommunityWithGroupData(
                              gatherAllCommunityMembers(item.getCommunityId(),null,
                                null, -1, Constants.COMMUNITY_LIST_MEMBER_DISPLAY_NUM))))
          .collect(Collectors.toList());
    }
  }

  public List<User> getAllMemberOfCommunityById(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int role, int limit) {
    return gatherAllCommunityMembers(communityId, toGetDmsMember,
            userIdList, role, limit)
        .stream()
        .map(this::convertToUser)
        .collect(Collectors.toList());
  }

  private User convertToUser(CommunityMember communityMember) {
    return new User()
        .id(communityMember.getUserId())
        .name(communityMember.getCname())
        .mail(communityMember.getMail())
        .status(communityMember.getStatus())
        .lock(communityMember.isDmsSync());
  }

  private List<User> getAllMemberOfCommunityWithGroupData(
      List<CommunityMember> derivativeCommunityMembers) {
    return derivativeCommunityMembers
        .stream()
        .map(this::convertToUser)
        .sorted(Comparator.comparing(User::getName))
        .collect(Collectors.toList());
  }

  private List<User> getGroupMember(List<GroupData> groupList) {
    List<User> userList =
        userService.getUserByIds(
            groupList
                .stream()
                .filter(
                    item ->
                        GroupConstants.SYSTEM_GROUP_NAME_ALLMEMBERS.equals(item.getName())
                            || GroupConstants.SYSTEM_GROUP_NAME_ADMIN.equals(item.getName())
                            || GroupConstants.SYSTEM_GROUP_NAME_MANAGER.equals(item.getName()))
                .map(GroupData::getMembers)
                .flatMap(List::stream)
                .map(User::getId)
                .distinct()
                .collect(Collectors.toList()));
    return userList.stream().map(item -> item.lock(true)).distinct().collect(Collectors.toList());
  }

  public Map<String, Integer> countCommunityCategoryByScope(SearchScope scope, String userId) {
    CommunitySearchRequestEntity request =
        new CommunitySearchRequestEntity()
            .setExcludeStatusList(
                Arrays.asList(
                    CommunityStatus.DELETE.toString(),
                    CommunityStatus.CLOSED.toString(),
                    CommunityStatus.LOCKED.toString()))
            .setCheckRole(needCheckingRole(scope))
            .setUserIdWithGid(getUserIdBySearchScope(scope, userId))
            .setRoleId(
                (SearchScope.MINEADMIN.equals(scope) ? Role.COMMUNITY_ADMIN : Role.COMMUNITY_MEMBER)
                    .getId());
    return getAllCommunityCategoryCount(request);
  }

  public Map<String, Integer> getAllCommunityCategoryCount(CommunitySearchRequestEntity request) {
    Map<String, Integer> result =
        communityDao
            .countCommunityCategory(request)
            .stream()
            .collect(
                Collectors.toMap(
                    m -> m.get(CommunityFieldName.CATEGORY.toString()).toString(),
                    m -> Integer.parseInt(m.get(Constants.SQL_COUNT).toString())));
    result.put(
        CommunityCategory.ALL.toString(),
        result.values().stream().collect(Collectors.summingInt(Integer::intValue)));
    Arrays.stream(CommunityCategory.values())
        .map(CommunityCategory::toString)
        .forEach(cate -> result.putIfAbsent(cate, NumberUtils.INTEGER_ZERO));
    return result
        .entrySet()
        .stream()
        .sorted(
            Comparator.comparing(
                e -> {
                  CommunityCategory category = CommunityCategory.fromValue(e.getKey());
                  return Objects.isNull(category) ? NumberUtils.INTEGER_ZERO : category.ordinal();
                }))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private int getCommunityCategoryCount(CommunitySearchRequestEntity request) {
    return getAllCommunityCategoryCount(request)
        .getOrDefault(request.getCategory().toString(), NumberUtils.INTEGER_ZERO);
  }

  public boolean addMemberIntoCommunityByCommunityType(List<String> memberId, int communityId) {
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_ADD_COMMUNITY_MEMBER_NOT_ADMIN);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    Set<String> filteredMemberIds =
        gatherAllCommunityMembers(communityId, null, memberId, -1, -1)
            .parallelStream()
            .map(CommunityMember::getUserId)
            .collect(Collectors.toSet());
    memberId =
        memberId.stream().filter(item -> !filteredMemberIds.contains(item)).collect(Collectors.toList());
    return memberId.isEmpty() || addMemberAndSendNotification(communityInfo, memberId);
  }

  public CommunityInfo getCommunityInfoById(int communityId) {
    List<CommunityInfo> communityInfoList =
        communityDao.getCommunityById(Collections.singletonList(communityId));
    if (CollectionUtils.isEmpty(communityInfoList)) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOT_EXIST);
    }
    return communityInfoList.get(NumberUtils.INTEGER_ZERO);
  }

  private CommunityType checkCommunityType(CommunityInfo communityInfo) {
    return Optional.ofNullable(CommunityType.fromValue(communityInfo.getCommunityType()))
        .orElseThrow(() -> new IllegalArgumentException(Constants.ERR_INVALID_PARAM));
  }

  private boolean addMemberAndSendNotification(CommunityInfo communityInfo, List<String> memberId) {
    boolean result = addMemberIntoCommunity(memberId, communityInfo.getCommunityId());
    if (result) {
      eventPublishService.publishNotificationSendingEvent(
          getJoinCommunityNotification(communityInfo, memberId));
      //eventPublishService.publishCommunityChangingEvent(communityInfo.getCommunityId());
    }
    return result;
  }

  public boolean addMemberIntoCommunity(List<String> memberId, int communityId) {
    return appendCommunityMembers(memberId, communityId, Role.COMMUNITY_MEMBER);
  }

  public boolean checkUserIsMemberOfCommunity(String userId, int communityId, String groupId) {
    return getAllMemberOfCommunityById(communityId, null,
            Collections.singletonList(userId), -1, -1)
        .stream()
        .anyMatch(item -> item.getId().equals(userId));
  }

  public boolean checkUserIsAdminOfGroup(String userId, List<GroupData> groupList) {
    return getGroupAdmin(groupList).stream().map(User::getId).anyMatch(item -> item.equals(userId));
  }

  public boolean checkUserCanUpdate(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId, communityId, 0, PermissionObject.COMMUNITY.toString(), Operation.UPDATE.toString());
  }

  public boolean checkUserCanEditMember(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId,
        communityId,
        0,
        PermissionObject.COMMUNITYMEMBER.toString(),
        Operation.REVIEW.toString());
  }

  private boolean checkUserCanNotify(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId, communityId, 0, PermissionObject.COMMUNITY.toString(), Operation.NOTIFY.toString());
  }

  private boolean checkUserRoleOfCommunity(String userId, int communityId, int roleId) {
    int row = communityDao.checkUserRoleOfCommunity(userId, communityId, roleId);
    return row >= 1;
  }

  public String addMemberApplicationOfCommunity(
      int communityId, ApplicationDetail applicationDetail) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    CommunityType communityType = checkCommunityType(communityInfo);
    if (CommunityType.PRIVATE.equals(communityType)) {
      return addCurrentUserApplication(communityId, applicationDetail);
    }
    boolean added =
        addMemberAndSendNotification(communityInfo, Arrays.asList(Utility.getUserIdFromSession()));
    if (added) {
      return I18nConstants.MSG_COMMUNITY_APPLICATION_APPROVED;
    } else {
      throw new IllegalArgumentException(I18nConstants.MSG_JOIN_COMMUNITY_APPLICATION_SENT_FAILED);
    }
  }

  private String addCurrentUserApplication(int communityId, ApplicationDetail applicationDetail) {
    if (applicationDetail.getSubject().isEmpty()) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    boolean isMember =
        checkUserRoleOfCommunity(
            Utility.getCurrentUserIdWithGroupId(), communityId, Role.COMMUNITY_MEMBER.getId());
    if (isMember) {
      return I18nConstants.MSG_ALREADY_COMMUNITY_MEMBER;
    }
    if (!checkApplicationExists(communityId, Utility.getUserIdFromSession())) {
      long now = new Date().getTime();
      int row =
          communityDao.addUserIntoCommunityJoinReview(
              communityId, Utility.getUserIdFromSession(), applicationDetail.getDesc(), now);
      if (row != 0) {
        CommunityInfo communityInfo = getCommunityInfoById(communityId);
        EmailWithChineseAndEnglishContext context =
            getCommunityJoinApplicationContext(communityInfo, applicationDetail.getDesc());
        context.setPriority(EmailConstants.HIGH_PRIORITY_MAIL);
        eventPublishService.publishEmailSendingEvent(context);
        eventPublishService.publishNotificationSendingEvent(
            getCommunityJoinApplicationNotification(
                communityInfo, applicationDetail.getDesc(), now));
        return I18nConstants.MSG_JOIN_COMMUNITY_APPLICATION_SENT;
      }
    }
    return I18nConstants.MSG_JOIN_COMMUNITY_APPLICATION_SENT_FAILED;
  }

  private boolean checkApplicationExists(int communityId, String applicantId) {
    return communityDao.checkApplicationExists(communityId, applicantId) == 1;
  }

  public boolean deleteMemberFromCommunity(String memberId, int communityId) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (isFromDmsGroupMember(memberId, communityInfo.getCommunityId())) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_REMOVE_GROUP_MEMBER);
    }
    if (checkLastAdminOfCommunity(memberId, communityId)) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_LAST_ADMIN);
    }
    if (checkMemberIdIsCurrentUserId(memberId)) {
      return leaveCommunity(communityInfo, memberId);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    return removeFromCommunity(communityInfo, memberId);
  }

  public boolean isFromDmsGroupMember(String userId, int communityId) {
    List<CommunityMember> communityMemberList = communityDao.getMemberListWithSortAndLimit(
            false, communityId, true, -1, EMPTY,
            Collections.singletonList(userId), 0, -1, EMPTY);
    return !communityMemberList.isEmpty();
  }

  private Set<String> getDmsGeneralMembers(String groupId) {
    CollectMemberContext context = collectMemberService.getDmsMember(groupId);
    return context
        .getGeneralMembers()
        .stream()
        .map(CommunityMember::getUserId)
        .collect(Collectors.toSet());
  }

  private boolean checkLastAdminOfCommunity(String memberId, int communityId) {
    List<User> adminList = getAdminListOfCommunity(communityId, null,
            Collections.singletonList(memberId), -1);

    List<CommunityMember> totalCount = communityDao.getMemberListWithSortAndLimit(
            true, communityId, null,
            Role.COMMUNITY_ADMIN.getId(), EMPTY,
            null, -1, -1, EMPTY);
    Optional<CommunityMember> optional = totalCount.stream().findFirst();
    int total = 0;
    if(optional.isPresent()) {
      total = optional.get().getTotalCount();
    }
    return (adminList.size() == 1) && (total == 1);
  }

  private boolean checkMemberIdIsCurrentUserId(String memberId) {
    return Utility.getUserIdFromSession().equals(memberId);
  }

  private boolean leaveCommunity(CommunityInfo communityInfo, String memberId) {
    boolean result =
        deleteUserFromCommunity(Arrays.asList(memberId), communityInfo.getCommunityId());
    if (result) {
      //eventPublishService.publishCommunityChangingEvent(communityInfo.getCommunityId());
    }
    return result;
  }

  private boolean removeFromCommunity(CommunityInfo communityInfo, String memberId) {
    boolean result =
        deleteUserFromCommunity(Arrays.asList(memberId), communityInfo.getCommunityId());
    if (result) {
      EmailWithChineseAndEnglishContext context =
          getRemoveFromCommunityContext(communityInfo, Arrays.asList(memberId));
      eventPublishService.publishEmailSendingEvent(context);
      eventPublishService.publishNotificationSendingEvent(
          getRemoveFromCommunityNotification(communityInfo, Arrays.asList(memberId)));
      //eventPublishService.publishCommunityChangingEvent(communityInfo.getCommunityId());
    }
    return result;
  }

  private boolean deleteUserFromCommunity(List<String> memberId, int communityId) {
    boolean deleted = deleteCommunityMembers(memberId, communityId, Role.COMMUNITY_MEMBER)
            && deleteCommunityMembers(memberId, communityId, Role.COMMUNITY_ADMIN);
    deleteUserFromAllForumOfCommunity(memberId, communityId);
    return deleted;
  }

  private void deleteUserFromAllForumOfCommunity(List<String> userId, int communityId) {
    List<RoleDetailEntity> forumInfoRows = forumDao.getAllForumRoleByCommunity(communityId);
    for (RoleDetailEntity row : forumInfoRows) {
      String currentGid = row.getGroupId();
      userGroupAdapter.removeUserGroupMembers(
              currentGid,
              userId);
    }
    communityDao.deleteUserFromForumJoinReviewOfCommunity(communityId, userId);
  }

  public List<ApplicantDetail> getApplicantList(int communityId) {
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    Map<String, String> applicantIdAndApplicationDescMap =
        communityDao
            .getApplicantList(communityId)
            .stream()
            .collect(
                Collectors.toMap(
                    x -> x.get(Constants.SQL_APPLICANT_ID),
                    x -> x.get(Constants.SQL_APPLICATION_DESC)));
    return getApplicantDetailList(applicantIdAndApplicationDescMap);
  }

  private List<ApplicantDetail> getApplicantDetailList(
      Map<String, String> applicantIdAndApplicationDescMap) {
    List<String> applicantIdList =
        applicantIdAndApplicationDescMap.keySet().stream().collect(Collectors.toList());
    List<UserSession> userList = userService.getUserById(applicantIdList, new ArrayList<>());
    return userList
        .stream()
        .map(
            item ->
                new ApplicantDetail()
                    .id(item.getCommonUUID())
                    .name(item.getCommonName())
                    .department(item.getProfileDeptName())
                    .desc(applicantIdAndApplicationDescMap.get(item.getCommonUUID()))
                    .ext(item.getProfilePhone())
                    .imgAvatar(item.getCommonImage()))
        .collect(Collectors.toList());
  }

  public String reviewMemberApplicationOfCommunity(
      int communityId, String applicantId, ReviewAction action) {
    if (!checkApplicationExists(communityId, applicantId)) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_APPLICATION_NOT_EXIST);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    long now = new Date().getTime();
    String reviewer = Utility.getUserIdFromSession();
    int row =
        communityDao.reviewTheMemberApplicationOfCommunity(
            communityId, applicantId, reviewer, now, action.toString());
    if (row == 1) {
      communityInfo = getCommunityInfoById(communityId);
      if (ReviewAction.APPROVED.equals(action) || ReviewAction.AUTO_APPROVED.equals(action)) {
        addMemberIntoCommunity(Arrays.asList(applicantId), communityId);
        EmailWithChineseAndEnglishContext context =
            getApproveJoinApplicationContext(communityInfo, applicantId);
        eventPublishService.publishEmailSendingEvent(context);
        eventPublishService.publishNotificationSendingEvent(
            getApproveJoinApplicationNotification(communityInfo, applicantId, now));
        //eventPublishService.publishCommunityChangingEvent(communityId);
      } else {
        EmailWithChineseAndEnglishContext context =
            getRejectJoinApplicationContext(communityInfo, applicantId);
        eventPublishService.publishEmailSendingEvent(context);
        eventPublishService.publishNotificationSendingEvent(
            getRejectJoinApplicationNotification(communityInfo, applicantId, now));
      }
      return I18nConstants.MSG_COMMUNITY_APPLICATION_APPROVED;
    } else {
      return I18nConstants.MSG_COMMUNITY_APPLICATION_REVIEW_FAILED;
    }
  }

  public MemberListResult getMemberListResult(
      int communityId, int offset, int limit, SortField sortType, String q,
      String userId, Boolean isImgAvatar, MemberType memberType) {
    // filter user list
    List<String> userListId = (userId.isEmpty()) ? null : Collections.singletonList(userId);
    int roleId = (memberType == null || memberType.equals(MemberType.ALL)) ? -1 :
            ((memberType.equals(MemberType.MEMBER)) ? Role.COMMUNITY_MEMBER.getId() : Role.COMMUNITY_ADMIN.getId());

    // get total count
    List<CommunityMember> totalCount = communityDao.getMemberListWithSortAndLimit(
            true, communityId, null, roleId, q,
            userListId, offset, limit, sortType.toString());
    Optional<CommunityMember> optional = totalCount.stream().findFirst();
    int total = 0;
    if(optional.isPresent()) {
      total = optional.get().getTotalCount();
    }

    // get list
    List<CommunityMember> communityMemberList = communityDao.getMemberListWithSortAndLimit(
            false, communityId, null, roleId, q,
            userListId, offset, limit, sortType.toString());
    List<String>userIdList = communityMemberList
            .stream()
            .map(CommunityMember::getUserId)
            .collect(Collectors.toList());

    List<UserSession> userList = userService.getUserById(userIdList, new ArrayList<>());
    List<MemberInfoDetail> memberInfoList =
        getMemberInfoDetail(userIdList, userList, communityMemberList, isImgAvatar);
    return new MemberListResult().result(memberInfoList).numFound(total);
  }

  private List<MemberInfoDetail> getMemberInfoDetail(
      List<String> userIdList,
      List<UserSession> userList,
      List<CommunityMember> communityMemberList,
      Boolean isImgAvatar) {
    Map<String, UserSession> userMap =
        userList
            .stream()
            .collect(Collectors.toMap(UserSession::getCommonUUID, Function.identity()));
    return userIdList
        .stream()
        .map(
            item ->
                transferInternalTalentUserToMemberInfoDetail(
                        userMap.getOrDefault(item, new UserSession()), item, isImgAvatar)
                    .isAdmin(
                      communityMemberList.stream()
                        .anyMatch(member ->
                                member.getUserId().equals(item)
                            && member.getRoleId() == Role.COMMUNITY_ADMIN.getId())))
        .collect(Collectors.toList());
  }

  private MemberInfoDetail transferInternalTalentUserToMemberInfoDetail(
      UserSession user, String userId, boolean isImgAvatar) {
    return new MemberInfoDetail()
        .id(userId)
        .name(user.getCommonName())
        .mail(user.getProfileMail())
        .department(user.getProfileDeptName())
        .ext(user.getProfilePhone())
        .imgAvatar(isImgAvatar ? user.getCommonImage() : StringUtils.EMPTY)
        .status(UserStatus.fromValue(user.getStatus()));
  }

  private boolean addMemberInfoToTempMember(Integer batchId, List<String> members, String type) {
    return communityDao.addMember(batchId, type, members) != 0;
  }

  private boolean checkDuplicateCommunityName(String originalName, String communityName) {
    Integer checkValue = 0;

    checkValue = communityDao.checkDuplicateCommunityName(originalName, communityName);
    return checkValue != 0;
  }

  public ResponseData createCommunity(CreatedCommunityData json) {
    final long milliseconds = System.currentTimeMillis();

    ResponseData code = new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
    boolean isRowAdmin = false;
    boolean isRowMember = false;

    if (json.getName().trim().isEmpty() || (json.getName().length() == 0)) {
      return new ResponseData().statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    if (json.getAdmins().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOTIFY_INPUT_PLACEHOLDER);
    }

    if (checkDuplicateCommunityName(StringUtils.EMPTY, json.getName().trim())) {
      return new ResponseData().statusCode(HttpStatus.SC_ACCEPTED);
    }

    CommunitiesCreationInfo communitiesCreationInfo = setCommunityCreationInfo(json, milliseconds);
    if (communityDao.addCommunitiesCreationInfo(communitiesCreationInfo) == 1) {
      addCommunityNotificationType(
          communitiesCreationInfo.getBatchId(), json.getNotificationType());
      isRowAdmin =
          addMemberInfoToTempMember(
              communitiesCreationInfo.getBatchId(),
              json.getAdmins(),
              CommunityMemberTypeEnum.ADMIN.toString());

      isRowMember =
          addMemberInfoToTempMember(
              communitiesCreationInfo.getBatchId(),
              json.getMembers(),
              CommunityMemberTypeEnum.MEMBER.toString());

      if (isRowAdmin && isRowMember) {
        code.id(communitiesCreationInfo.getBatchId()).statusCode(HttpStatus.SC_CREATED);
        eventPublishService.publishNotificationSendingEvent(
            getCommunityCreateApplicationNotification(communitiesCreationInfo));
      }

      notifyCommunityReview(communitiesCreationInfo, Constants.CONTENT_EMPTY);
    }
    return code;
  }

  private ResponseData notifyCommunityReview(
      CommunitiesCreationInfo communitiesCreationInfo, String reviewMessage) {
    log.info("send community review mail to admin");
    List<String> adminIdList = userService.getSystemAdminIds();
    String host = yamlConfig.getHost();
    String userId = Utility.getUserFromSession().getCommonUUID();
    String userName = Utility.getUserFromSession().getCommonName();
    String link = String.format(EmailConstants.COMMUNITY_REVIEW_URI_FORMAT, host);
    NotificationV2Request notificationV2Request = new NotificationV2Request();
    notificationV2Request.setLink(new NotificationV2Link().setUrl(link));
    notificationV2Request.setName(NotificationV2Entities.COMMUNITY_REVIEW);
    notificationV2Request.setTriggerUserId(userId);
    notificationV2Request.setRecipient(new HashSet<>(adminIdList));
    notificationV2Request.setIconId(userId);
    notificationV2Request.setVariable(
        Stream.of(
                new AbstractMap.SimpleEntry<>(
                    NotificationV2Variable.TITLE, communitiesCreationInfo.getCommunityName()),
                new AbstractMap.SimpleEntry<>(NotificationV2Variable.TRIGGERER, userName))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    notificationV2Adapter.sendNotification(notificationV2Request);

    return new ResponseData().statusCode(HttpStatus.SC_CREATED);
  }

  private boolean addCommunityNotificationType(int batchId, NotificationType notificationType) {
    return notificationType == null
        ? communityDao.addCommunityNotificationType(batchId, "") != 0
        : communityDao.addCommunityNotificationType(batchId, notificationType.toString()) != 0;
  }

  private CommunitiesCreationInfo setCommunityCreationInfo(
      CreatedCommunityData json, long milliseconds) {
    CommunitiesCreationInfo communitiesCreationInfo = new CommunitiesCreationInfo();
    communitiesCreationInfo.setCommunityName(json.getName().trim());
    communitiesCreationInfo.setCommunityDesc(json.getDesc().trim());
    communitiesCreationInfo.setCommunityCategory(json.getCategory().toString());
    communitiesCreationInfo.setApplicantId(Utility.getUserIdFromSession());
    communitiesCreationInfo.setApplicationTime(milliseconds);
    communitiesCreationInfo.setCommunityType(json.getType().toString());
    communitiesCreationInfo.setLanguage(AcceptLanguage.get());
    return communitiesCreationInfo;
  }

  public ResponseData reviewCommunityCreation(Integer batchId, ApprovalStatus body) {
    if (!Objects.equals(body.getStatus(), ReviewAction.AUTO_APPROVED)
        && !checkUserCanReviewCommunity(Utility.getCurrentUserIdWithGroupId())) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_SYSTEM_ADMIN);
    }
    CommunitiesCreationInfo communitiesCreationInfo =
        communityDao.getCommunityCreationById(batchId);
    if (StringUtils.isNotEmpty(communitiesCreationInfo.getStatus())) {
      return new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
    }
    if (checkDuplicateCommunityNameWithCommunities(batchId)
        && Objects.equals(body.getStatus(), ReviewAction.APPROVED)) {
      return new ResponseData().statusCode(HttpStatus.SC_ACCEPTED);
    }

    setCommunitiesCreationInfo(communitiesCreationInfo, body);
    if (communityDao.updateCommunitiesCreationInfo(communitiesCreationInfo) != 0) {
      if (Objects.equals(body.getStatus(), ReviewAction.APPROVED)
          || Objects.equals(body.getStatus(), ReviewAction.AUTO_APPROVED)) {

        return approveCommunity(communitiesCreationInfo, body.getStatus());
      } else if (Objects.equals(body.getStatus(), ReviewAction.REJECTED)
          && (communityDao.updateRejectedMessage(batchId, body.getRejectedMessage()) != 0)) {

        return rejectCommunity(communitiesCreationInfo, body.getRejectedMessage());
      }
    }
    return new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
  }

  public boolean checkDuplicateCommunityNameWithCommunities(int batchId) {
    Integer checkRevieValue = 0;

    checkRevieValue = communityDao.checkDuplicateCommunityNameWithCommunities(batchId);
    return checkRevieValue != 0;
  }

  private ResponseData rejectCommunity(
      CommunitiesCreationInfo communitiesCreationInfo, String rejectedMessage) {
    EmailWithChineseAndEnglishContext context =
        getRejectCreateApplicationContext(
            communitiesCreationInfo, communitiesCreationInfo.getApplicantId(), rejectedMessage);
    eventPublishService.publishEmailSendingEvent(context);
    eventPublishService.publishNotificationSendingEvent(
        getRejectCreateApplicationNotification(
            communitiesCreationInfo,
            communitiesCreationInfo.getApplicantId(),
            rejectedMessage,
            communitiesCreationInfo.getReviewTime()));
    return new ResponseData().statusCode(HttpStatus.SC_CREATED);
  }

  private ResponseData approveCommunity(
      CommunitiesCreationInfo communitiesCreationInfo, ReviewAction status) {
    CommunityData communityData = setCommunityData(communitiesCreationInfo.getBatchId());
    if ((communityDao.transferCommunityData(communityData) != 0)
        && (communityDao.transferCommunitySetting(
                communitiesCreationInfo.getBatchId(), communityData.getCommunityId())
            != 0)) {

      // create groups for admin/member
      List<UserInfo> appliedMemberList = communityDao.getCommunityAppliedMember(communitiesCreationInfo.getBatchId());
      String adminGroupId = getCommunityUserGroupId(Role.COMMUNITY_ADMIN, communityData.getCommunityId(), appliedMemberList);
      String memberGroupId = getCommunityUserGroupId(Role.COMMUNITY_MEMBER, communityData.getCommunityId(), appliedMemberList);

      // transfer group roles
      if((communityDao.transferCommunityRole(
              adminGroupId, memberGroupId,
              communitiesCreationInfo.getBatchId(), communityData.getCommunityId())
              != 0)) {
        CommunityInfo communityInfo = getCommunityInfoById(communityData.getCommunityId());
        communityInfo.setCommunityLanguage(
                communityDao.getLangByBatchId(communitiesCreationInfo.getBatchId()));
        List<String> members =
                communityDao.getAllMemberWithoutApplicantIdById(communityData.getCommunityId());

        sendApproveMail(communityInfo, communitiesCreationInfo);
        sendJoinMail(members, communityInfo);
        setSystemForum(communityInfo.getCommunityId());
        members.add(communityInfo.getCommunityCreateUserId());

        String userId =
                Objects.equals(status, ReviewAction.AUTO_APPROVED)
                        ? Constants.AUTO_USERID
                        : Utility.getUserIdFromSession();
        eventPublishService.publishActivityLogEvent(
                Utility.setActivityLogData(
                        userId,
                        Operation.CREATE.toString(),
                        PermissionObject.COMMUNITY.toString(),
                        communityData.getCommunityId(),
                        Constants.INFO_PROJECT_NAME,
                        Constants.CONTENT_EMPTY,
                        Constants.ATTACHMENTID_EMPTY));
        eventPublishService.publishActivityLogEvent(
                Utility.setActivityLogData(
                        userId,
                        Operation.REVIEW.toString(),
                        PermissionObject.COMMUNITY.toString(),
                        communityData.getCommunityId(),
                        Constants.INFO_PROJECT_NAME,
                        Constants.CONTENT_EMPTY,
                        Constants.ATTACHMENTID_EMPTY));

        eventPublishService.publishActivityLogMsgEvent(
                ActivityLogUtil.convertToActivityLogMsg(
                        App.COMMUNITY,
                        Constants.ACTIVITY_APP_VERSION,
                        userId,
                        Activity.CREATE,
                        ObjectType.COMMUNITYID,
                        String.valueOf(communityData.getCommunityId()),
                        ActivityLogUtil.getAnnotation(PermissionObject.COMMUNITY, Constants.CONTENT_EMPTY),
                        LogStatus.SUCCESS,
                        LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
                        null,
                        null));

        return new ResponseData()
                .statusCode(HttpStatus.SC_CREATED)
                .id(communityData.getCommunityId());
      }
      else {
        return new ResponseData().statusCode(HttpStatus.SC_METHOD_FAILURE);
      }
    } else {
      return new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
    }
  }

  private String getCommunityUserGroupId(Role communityRole, int getCommunityId, List<UserInfo> appliedMemberList) {
    String identity = (Role.COMMUNITY_ADMIN.equals(communityRole)) ? Identity.ADMIN.toString() : "";
    List<String> memberIdList = Optional.ofNullable(appliedMemberList).orElse(new ArrayList<>())
            .stream()
            .filter(item -> (identity.isEmpty() || item.getType().equalsIgnoreCase(identity)))
            .map(UserInfo::getUserId)
            .collect(Collectors.toList());
    return userGroupAdapter.createUserGroup(
            communityRole, getCommunityId, yamlConfig.getAppGroupRootId(), memberIdList);
  }

  private void sendApproveMail(
      CommunityInfo communityInfo, CommunitiesCreationInfo communitiesCreationInfo) {
    EmailWithChineseAndEnglishContext context =
        getApproveCreateApplicationContext(communityInfo, communityInfo.getCommunityCreateUserId());
    eventPublishService.publishEmailSendingEvent(context);
    eventPublishService.publishNotificationSendingEvent(
        getApproveCreateApplicationNotification(
            communityInfo,
            communityInfo.getCommunityCreateUserId(),
            communitiesCreationInfo.getReviewTime()));
  }

  private void sendJoinMail(List<String> members, CommunityInfo communityInfo) {
    if (!members.isEmpty()) {
      eventPublishService.publishNotificationSendingEvent(
          getJoinCommunityNotification(communityInfo, members));
    }
  }

  private CommunitiesCreationInfo setCommunitiesCreationInfo(
      CommunitiesCreationInfo communitiesCreationInfo, ApprovalStatus body) {
    long milliseconds = System.currentTimeMillis();
    if (Objects.equals(body.getStatus(), ReviewAction.AUTO_APPROVED)) {
      communitiesCreationInfo.setReviewerId(Constants.AUTO_USERID);
    } else if (Objects.equals(body.getStatus(), ReviewAction.APPROVED)
        || Objects.equals(body.getStatus(), ReviewAction.REJECTED)) {
      communitiesCreationInfo.setReviewerId(Utility.getUserIdFromSession());
    }
    communitiesCreationInfo.setReviewTime(milliseconds);
    communitiesCreationInfo.setStatus(body.getStatus().toString());

    return communitiesCreationInfo;
  }

  private CommunityData setCommunityData(Integer batchId) {
    CommunityData communityData = new CommunityData();
    communityData.setBatchId(batchId);
    communityData.setCommunityStatus(CommunityStatus.OPEN.toString());
    return communityData;
  }

  private boolean checkUserCanReviewCommunity(String userId) {
    return privilegeService.checkUserPrivilege(
        userId, 0, 0, PermissionObject.COMMUNITY.toString(), Operation.REVIEW.toString());
  }

  private EmailWithChineseAndEnglishContext getApproveCreateApplicationContext(
      CommunityInfo communityInfo, String applicantId) {
    String host = yamlConfig.getHost();
    String language =
        StringUtils.isEmpty(communityInfo.getCommunityLanguage().trim())
            ? AcceptLanguage.get()
            : communityInfo.getCommunityLanguage().trim();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYCREATIONAPPROVAL)
        .sender(communityInfo.getCommunityName())
        .desc(EmailConstants.APPROVE_CREATE_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.APPROVE_CREATE_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.APPROVE_CREATE_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  private EmailWithChineseAndEnglishContext getRejectCreateApplicationContext(
      CommunitiesCreationInfo communitiesCreationInfo, String applicantId, String content) {
    String link = String.format(EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getHost());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getMobileDownloadUrl());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYCREATIONREJECTION)
        .sender(communitiesCreationInfo.getCommunityName())
        .desc(EmailConstants.REJECT_CREATE_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.REJECT_CREATE_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.REJECT_CREATE_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .content(String.format(EmailConstants.REJECT_CREATE_APPLICATION_CONTENT_FORMAT, content))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY,
                    communitiesCreationInfo.getCommunityName()),
                null));
  }

  public Integer updateCommunity(Integer communityId, UpdatedCommunityData body) {
    if (!checkUserCanUpdate(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    if (body.getName().trim().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NAME_PLACEHOLDER);
    }
    if (body.getAdmins().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOTIFY_INPUT_PLACEHOLDER);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    CommunityType communityType = checkCommunityType(communityInfo);
    if (checkDuplicateCommunityName(communityInfo.getCommunityName(), body.getName().trim())
        && !Objects.equals(communityInfo.getCommunityName().trim(), body.getName().trim())) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NAME_REPEAT);
    }
    if ((communityInfo.getCommunityModifiedTime() != 0
        && communityInfo.getCommunityModifiedTime() != body.getCommunityModifiedTime())) {
      return HttpStatus.SC_CONFLICT;
    }
    int code = updateCommunityInfo(communityId, body, communityInfo.getCommunityGroupId());
    if (code == HttpStatus.SC_CREATED) {
      List<String> uncheckApplicants = communityDao.getApplicantIdWithUncheck(communityId);
      List<String> approvedApplicants =
          uncheckApplicants
              .stream()
              .filter(item -> body.getAdmins().contains(item))
              .collect(Collectors.toList());
      uncheckApplicants =
          uncheckApplicants
              .stream()
              .filter(item -> !approvedApplicants.contains(item))
              .collect(Collectors.toList());
      approvedApplicants.forEach(
          item ->
              communityDao.reviewTheMemberApplicationOfCommunity(
                  communityId,
                  item,
                  Utility.getUserIdFromSession(),
                  Instant.now().toEpochMilli(),
                  ReviewAction.APPROVED.toString()));
      /*
      if (CommunityType.PRIVATE.equals(communityType)
              && Objects.equals(body.getType(), UpdatedCommunityData.TypeEnum.PUBLIC)) {
        batchReviewMember(
                communityId, ReviewAction.AUTO_APPROVED, body.getMembers(), uncheckApplicants);
      }
       */
    }
    return code;
  }

  private boolean batchReviewMember(
      Integer communityId, ReviewAction action, List<String> sources, List<String> destinations) {
    boolean result = false;

    if (destinations != null) {
      ArrayList<String> destinationsClone = new ArrayList<>(destinations);
      destinationsClone.removeAll(sources);

      for (String applicantId : destinationsClone) {
        reviewMemberApplicationOfCommunity(communityId, applicantId, action);
      }
      result = true;
    }
    return result;
  }

  private Integer updateCommunityInfo(
      Integer communityId, UpdatedCommunityData body, String groupId) {

    CommunityInfo communityInfo = new CommunityInfo();
    boolean isAdmins = true;
    boolean isMembers = true;
    long milliseconds = System.currentTimeMillis();

    communityInfo.setCommunityId(communityId);
    communityInfo.setCommunityName(body.getName().trim());
    communityInfo.setCommunityDesc(body.getDesc().trim());
    communityInfo.setCommunityType(body.getType().toString());
    communityInfo.setCommunityStatus(body.getStatus().toString());
    communityInfo.setCommunityModifiedUserId(Utility.getUserIdFromSession());
    communityInfo.setCommunityModifiedTime(milliseconds);
    communityInfo.setCommunityCategory(body.getCategory().toString());

    if ((communityDao.update(communityInfo) > 0)
        && updateCommunitySeting(communityId, body.getNotificationType())) {
      //updateCommunityNotify(communityInfo, members, body.getMembers());

      isAdmins = overwriteCommunityMembers(body.getAdmins(), communityInfo.getCommunityId(), Role.COMMUNITY_ADMIN);
      // DMSVOC-772 [Community]在社群列表上看不到已加入的社團，原因是因為此處指定社群經理人，並未一起加入至成員
      // 拿"意圖更新的admin列表"查詢已經在社群中的成員
      List<User> existingMembers = getAllMemberOfCommunityById(
              communityId, null, body.getAdmins(), Role.COMMUNITY_MEMBER.getId(), -1);
      // "意圖更新的admin列表"扣除已經在社群中的成員
      List<String> filteredNewMembers = body.getAdmins().stream()
              .filter(item -> existingMembers.stream().noneMatch(member -> member.getId().equals(item)))
              .collect(Collectors.toList());
      // 將"意圖更新的admin列表"中的成員加入社群
      if (!filteredNewMembers.isEmpty()) {
        isMembers = appendCommunityMembers(filteredNewMembers, communityId, Role.COMMUNITY_MEMBER);
      }
      if (isAdmins && isMembers) {
        eventPublishService.publishCommunityChangingEvent(communityId);
        eventPublishService.publishActivityLogEvent(
            Utility.setActivityLogData(
                Utility.getUserIdFromSession(),
                Operation.UPDATE.toString(),
                PermissionObject.COMMUNITY.toString(),
                communityId,
                Constants.INFO_PROJECT_NAME,
                Constants.CONTENT_EMPTY,
                Constants.ATTACHMENTID_EMPTY));

        eventPublishService.publishActivityLogMsgEvent(
            ActivityLogUtil.convertToActivityLogMsg(
                App.COMMUNITY,
                Constants.ACTIVITY_APP_VERSION,
                Utility.getUserIdFromSession(),
                Activity.UPDATE,
                ObjectType.COMMUNITYID,
                String.valueOf(communityId),
                ActivityLogUtil.getAnnotation(PermissionObject.COMMUNITY, Constants.CONTENT_EMPTY),
                LogStatus.SUCCESS,
                LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
                null,
                null));

        return HttpStatus.SC_CREATED;
      } else {
        return HttpStatus.SC_NOT_FOUND;
      }
    } else {
      return HttpStatus.SC_NOT_FOUND;
    }
  }

  private String getCurrentGid(int communityId, Role role) {
    String currentGid;
    if (role.equals(Role.COMMUNITY_ADMIN)) {
      currentGid = getAdminUserAndGroupOfCommunity(communityId).stream().findFirst().orElse("");
    }
    else {
      currentGid = getMemberUserAndGroupOfCommunity(communityId).stream().findFirst().orElse("");
    }
    return currentGid;
  }

  private boolean overwriteCommunityMembers(List<String> memberIdList, int communityId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = getCurrentGid(communityId, role);
    return userGroupAdapter.overwriteUserGroupMembers(
            role,
            communityId,
            yamlConfig.getAppGroupRootId(),
            memberIdList,
            currentGid);
  }

  private boolean appendCommunityMembers(List<String> memberIdList, int communityId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = getCurrentGid(communityId, role);
    return userGroupAdapter.appendUserGroupMembers(
            currentGid,
            updatedMemberIdList);
  }

  private boolean deleteCommunityMembers(List<String> memberIdList, int communityId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = getCurrentGid(communityId, role);
    return userGroupAdapter.removeUserGroupMembers(
            currentGid,
            updatedMemberIdList);
  }

  private boolean updateCommunitySeting(int communityId, NotificationType notificationType) {
    String oldNotificationType = communityDao.getCommunityNotificationType(communityId);
    if (oldNotificationType == null) {
      return notificationType == null
          ? communityDao.insertCommunityNotificationType(communityId, "", AcceptLanguage.get()) != 0
          : communityDao.insertCommunityNotificationType(
                  communityId, notificationType.toString(), AcceptLanguage.get())
              != 0;
    } else {
      return notificationType == null
          ? communityDao.updateCommunitySeting(communityId, "") != 0
          : communityDao.updateCommunitySeting(communityId, notificationType.toString()) != 0;
    }
  }

  private void updateCommunityNotify(
      CommunityInfo communityInfo, List<String> originalMembers, List<String> newMembers) {
    ArrayList<String> originalMembersClone = new ArrayList<>(originalMembers);
    ArrayList<String> newMembersClone = new ArrayList<>(newMembers);

    newMembersClone.removeAll(originalMembers);
    if (!newMembersClone.isEmpty()) {
      eventPublishService.publishNotificationSendingEvent(
          getJoinCommunityNotification(communityInfo, newMembersClone));
    }

    originalMembersClone.removeAll(newMembers);
    if (!originalMembersClone.isEmpty()) {
      EmailWithChineseAndEnglishContext deleteContext =
          getRemoveFromCommunityContext(communityInfo, originalMembersClone);
      eventPublishService.publishEmailSendingEvent(deleteContext);
      eventPublishService.publishNotificationSendingEvent(
          getRemoveFromCommunityNotification(communityInfo, originalMembersClone));
      deleteUserFromCommunity(originalMembersClone, communityInfo.getCommunityId());
    }
  }

  private CommunityInfo getCommunityImgBannerById(int communityId) {
    CommunityInfo communityInfo = communityDao.getCommunityImgBannerById(communityId);
    if (communityInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOT_EXIST);
    }
    return communityInfo;
  }

  public Image getCommunityBanner(Integer communityId) {
    Image image = new Image();

    CommunityInfo communityInfo = getCommunityImgBannerById(communityId);
    image.setImage(communityInfo.getCommunityImgBanner());
    return image;
  }

  public Integer updateCommunityBanner(Integer communityId, Image body) {
    if (!checkUserCanUpdate(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }

    if (body.getImage().trim().isEmpty() || (body.getImage().length() == 0)) {
      return HttpStatus.SC_BAD_REQUEST;
    }

    CommunityInfo communityInfo = getCommunityImgBannerById(communityId);
    String newImage = resizeImage(body.getImage(), bannerWidth, bannerHeight);
    if (newImage == null) {
      communityInfo.setCommunityImgBanner(body.getImage());
    } else {
      communityInfo.setCommunityImgBanner(newImage);
    }

    if (communityDao.updateCommunityImgBanner(communityInfo) > 0) {
      return HttpStatus.SC_CREATED;
    }
    return HttpStatus.SC_NOT_FOUND;
  }

  public String getCommunityImgAvatarById(int communityId) {
    return Optional.ofNullable(communityDao.getCommunityAvatarById(communityId))
        .orElse(StringUtils.EMPTY);
  }

  public CommunityMedalDto getCommunityAvatarMedal(int communityId) {
    List<Medal> medals = medalService.getMedals(MedalIdType.COMMUNITY, String.valueOf(communityId));
    return getCommunityMedal(medals);
  }

  private CommunityMedalDto getCommunityMedal(List<Medal> medals) {
    CommunityMedalDto communityMedalDto = new CommunityMedalDto();
    communityMedalDto.setCertificates(
        CollectionUtils.emptyIfNull(medals)
            .stream()
            .sorted(Comparator.comparingInt(Medal::getCertificationOrder))
            .map(Medal::getCertification)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList()));
    CollectionUtils.emptyIfNull(medals)
        .stream()
        .filter(Medal::isSelected)
        .findFirst()
        .ifPresent(
            medal -> {
              communityMedalDto.setMedal(medal.getFrame());
              communityMedalDto.setMedalId(medal.getId());
              communityMedalDto.setTitle(medal.getTitle());
            });
    return communityMedalDto;
  }

  public Image getCommunityAvatar(Integer communityId) {
    Image image = new Image();
    image.setImage(communityDao.getCommunityAvatarById(communityId));
    return image;
  }

  public Integer updateCommunityAvatar(Integer communityId, AvatarImage body) {
    if (!checkUserCanUpdate(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }

    medalService.setCommunityActivateMedal(communityId, body.getMedalId());
    if (StringUtils.isBlank(body.getImage())) {
      return HttpStatus.SC_CREATED;
    }

    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    String newImage = resizeImage(body.getImage(), avatarWidth, avatarHeight);
    String communityAvatar = Objects.isNull(newImage) ? body.getImage() : newImage;

    if (communityDao.updateCommunityImgAvatar(communityId, communityAvatar) > 0) {
      eventPublishService.publishCommunityAvatarChangingEvent(communityId);
      return HttpStatus.SC_CREATED;
    }
    return HttpStatus.SC_NOT_FOUND;
  }

  public CommunityReviewList getAllCommunityCreation(boolean processed, int offset, int limit) {
    List<String> status;
    if (processed) {
      status =
          Arrays.stream(ReviewAction.values())
              .map(ReviewAction::toString)
              .collect(Collectors.toList());
    } else {
      status = Arrays.asList("");
    }
    List<CommunityReviewDetail> applicationList =
        getApplicationListOfReviewCommunity(status, offset, limit);
    int numFound = communityDao.countCommunityCreationApplicationList(status);
    return new CommunityReviewList().result(applicationList).numFound(numFound);
  }

  private List<CommunityReviewDetail> getApplicationListOfReviewCommunity(
      List<String> status, int offset, int limit) {
    if (!userService.isSysAdmin()) {
      throw new AuthenticationException(I18nConstants.MSG_NOT_SYSTEM_ADMIN);
    }

    List<CommunityReviewDetail> applicationList =
        communityDao.getAllCommunityCreationList(status, offset, limit);
    applicationList.forEach(
        item -> {
          String applicantName = userService.getUserById(item.getApplicantId()).getName();
          String reviewerName = userService.getUserById(item.getReviewerId()).getName();
          item.applicantName(applicantName).reviewerName(reviewerName);
        });
    return applicationList;
  }

  public int sendNotification(int communityId, NotificationDetail notificationDetail) {
    if (!checkUserCanNotify(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    EmailWithChineseAndEnglishContext context =
        getNotificationContext(notificationDetail, communityInfo);
    eventPublishService.publishEmailSendingEvent(context);
    eventPublishService.publishNotificationSendingEvent(
        getNotification(notificationDetail, communityInfo));
    return HttpStatus.SC_OK;
  }

  private EmailWithChineseAndEnglishContext getCommunityJoinApplicationContext(
      CommunityInfo communityInfo, String content) {
    /*
    List<String> adminIdList =
        getAdminListOfCommunity(communityInfo.getCommunityId(), communityInfo.getCommunityGroupId(), true)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    if (!Objects.equals(
        communityInfo.getCommunityCategory(), CommunityCategory.GENERAL.toString())) {
      adminIdList =
          getAdminListOfCommunity(
                  communityInfo.getCommunityId(), communityInfo.getCommunityGroupId(), true)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
    }
     */
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYJOINAPPLICATION)
        .sender(userName)
        .desc(String.format(EmailConstants.COMMUNITY_JOIN_APPLICATION_CHINESE_FORMAT, userName))
        .englishDesc(
            String.format(EmailConstants.COMMUNITY_JOIN_APPLICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.JOIN_COMMUNITY_APPLICATION_SUBJECT_FORMAT)
        //.to(userService.getEmailByUserId(adminIdList))
        .content(content)
        .link(link)
        .mobileLink(mobileLink)
        .communityInfo(communityInfo)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  private EmailWithChineseAndEnglishContext getApproveJoinApplicationContext(
      CommunityInfo communityInfo, String applicantId) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYJOINAPPROVAL)
        .sender(communityInfo.getCommunityName())
        .desc(EmailConstants.APPROVE_JOIN_COMMUNITY_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.APPROVE_JOIN_COMMUNITY_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.APPROVE_JOIN_COMMUNITY_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  private EmailWithChineseAndEnglishContext getRejectJoinApplicationContext(
      CommunityInfo communityInfo, String applicantId) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYJOINREJECTION)
        .sender(communityInfo.getCommunityName())
        .desc(EmailConstants.REJECT_JOIN_COMMUNITY_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.REJECT_JOIN_COMMUNITY_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.REJECT_JOIN_COMMUNITY_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  private EmailWithChineseAndEnglishContext getRemoveFromCommunityContext(
      CommunityInfo communityInfo, List<String> memberId) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .communityInfo(communityInfo)
        .type(EmailType.REMOVEFROMCOMMUNITY)
        .sender(Utility.getUserFromSession().getCommonName())
        .desc(
            String.format(
                EmailConstants.REMOVE_FROM_COMMUNITY_CHINESE_FORMAT,
                communityInfo.getCommunityName()))
        .englishDesc(
            String.format(
                EmailConstants.REMOVE_FROM_COMMUNITY_ENGLISH_FORMAT,
                communityInfo.getCommunityName()))
        .subject(EmailConstants.REMOVE_FROM_COMMUNITY_SUBJECT)
        .to(userService.getEmailByUserId(memberId))
        .link(link)
        .mobileLink(mobileLink);
  }

  public List<User> getAdminListOfCommunity(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int limit) {
    return getAdminListOfCommunityWithGroupData(gatherAllCommunityMembers(communityId,
            toGetDmsMember, userIdList, Role.COMMUNITY_ADMIN.getId(), limit));
  }

  private List<User> getAdminListOfCommunityWithGroupData(
      List<CommunityMember> communityMembers) {
    return communityMembers
        .stream()
        .filter(d -> d.getRoleId() == Role.COMMUNITY_ADMIN.getId())
        .map(this::convertToUser)
        .sorted(Comparator.comparing(User::getName))
        .collect(Collectors.toList());
  }

  private List<User> getGroupAdmin(List<GroupData> groupList) {
    List<User> userList = new ArrayList<>();
    groupList
        .stream()
        .filter(
            item ->
                GroupConstants.SYSTEM_GROUP_NAME_ADMIN.equals(item.getName())
                    || GroupConstants.SYSTEM_GROUP_NAME_MANAGER.equals(item.getName()))
        .forEach(item -> userList.addAll(item.getMembers()));
    return userList
        .stream()
        .map(item -> item.lock(true).status(userService.getUserStatus(item.getId())))
        .distinct()
        .collect(Collectors.toList());
  }

  private EmailWithChineseAndEnglishContext getNotificationContext(
      NotificationDetail notificationDetail, CommunityInfo communityInfo) {
    String userName = Utility.getUserFromSession().getCommonName();
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationDetail.getType())) ? EmailMemberType.ALLCOMMUNITYMEMBER : EmailMemberType.NONE)
        .communityInfo(communityInfo)
        .sender(communityInfo.getCommunityName())
        .desc(String.format(EmailConstants.COMMUNITY_NOTIFICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.COMMUNITY_NOTIFICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + notificationDetail.getSubject())
        .content(
            StringUtils.replace(
                notificationDetail.getContent(), Constants.LINE_BREAKS, Constants.HTML_LINE_BREAKS))
        .to(
            userService.getEmailByUserId(
                getRecipientList(communityInfo.getCommunityId(), notificationDetail)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                notificationDetail.getSubject(),
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName())));
  }

  private List<String> getRecipientList(int communityId, NotificationDetail notificationDetail) {
    List<String> userIdList;
    if (NotificationType.ALL.equals(notificationDetail.getType())) {
      userIdList = Arrays.asList("");
      /*
      String groupId = getCommunityInfoById(communityId).getCommunityGroupId();
      userIdList =
          getAllMemberOfCommunityById(communityId, true, null, -1, -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
       */
    } else {
      userIdList = notificationDetail.getRecipient();
    }
    List<String> orgRecipient =
        groupRecipientHandleService.getOrgRecipient(notificationDetail.getOrgMembers());
    List<String> bgbuRecipient =
        groupRecipientHandleService.getOrgRecipient(notificationDetail.getBgbus());
    return Stream.of(userIdList, orgRecipient, bgbuRecipient)
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  public String updatMemberRoleOfCommunity(
      Integer communityId, String memberId, com.delta.dms.community.swagger.model.Role role) {
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }

    if (checkLastAdminOfCommunity(memberId, communityId)
        && Objects.equals(role.getRole(), Identity.MEMBER)) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_LAST_ADMIN);
    }

    List<CommunityMember> communityMembers =
        gatherAllCommunityMembers(communityId,false, Collections.singletonList(memberId), -1, -1);
    if (communityMembers.isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_TRANSFER_GROUP_MEMBER);
    }
    if (Objects.equals(role.getRole(), Identity.ADMIN)) {
      addAdminIntoCommunity(Collections.singletonList(memberId), communityId);
    } else if (Objects.equals(role.getRole(), Identity.MEMBER)) {
      removeAdminFromCommunity(Collections.singletonList(memberId), communityId);
    }
    return I18nConstants.MSG_COMMUNITY_ROLE_TRANSFER_OK;
  }

  public boolean updateLastModifiedOfCommunity(
      Integer communityId, String userId, long milliseconds) {
    return communityDao.updateLastModifiedOfCommunity(communityId, userId, milliseconds) != 0;
  }

  public Identity getUserIdentityOfCommunity(String userId, int communityId, String groupId) {
    if (userService.isSysAdmin(userId)) {
      return Identity.ADMIN;
    }

    final List<CommunityMember> communityMembers =
            gatherAllCommunityMembers(communityId, null, Collections.singletonList(userId), -1, -1);
    final Set<String> admins =
        communityMembers
            .stream()
            .filter(COMMUNITY_ADMIN_PREDICATE)
            .map(CommunityMember::getUserId)
            .collect(Collectors.toSet());
    final Set<String> members =
        communityMembers
            .stream()
            .filter(COMMUNITY_MANAGER_PREDICATE)
            .map(CommunityMember::getUserId)
            .collect(Collectors.toSet());

    if (admins.contains(userId)) {
      return Identity.ADMIN;
    } else if (members.contains(userId)) {
      return Identity.MEMBER;
    } else if (checkApplicationExists(communityId, userId)) {
      return Identity.APPLICANT;
    } else {
      return Identity.GUEST;
    }
  }

  public ResponseData createAttachedCommunity(AttachedCommunityData communityData) {
    if (StringUtils.isEmpty(communityData.getGroupId())
        || StringUtils.isEmpty(communityData.getCreateUserId())) {
      return new ResponseData().statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    List<MyDmsGroupData> myDmsGroupDatas =
        userGroupAdapter.getPathInfoByGid(communityData.getGroupId());
    ResponseEntity<JsonNode> groupBeanByGidWithResponse =
        userGroupAdapter.getGroupBeanByGidWithResponse(communityData.getGroupId());
    if (!Objects.equals(groupBeanByGidWithResponse.getStatusCode().value(), HttpStatus.SC_OK)) {
      throw new CreationException(groupBeanByGidWithResponse.toString());
    }

    if (checkDuplicateCommunityGroupId(communityData.getGroupId())) {
      return new ResponseData().statusCode(HttpStatus.SC_ACCEPTED);
    }

    MyDmsGroupData myDmsGroupData = myDmsGroupDatas.stream().findFirst().orElse(null);
    if (myDmsGroupDatas.isEmpty() || StringUtils.isEmpty(myDmsGroupData.getGroupName().trim())) {
      return new ResponseData().statusCode(HttpStatus.SC_BAD_REQUEST);
    }
    String groupDesc =
        CommunityCategory.PROJECT.toString().equals(myDmsGroupData.getGroupType())
            ? Optional.ofNullable(groupBeanByGidWithResponse.getBody())
                .map(
                    body ->
                        Utility.getStringFromJsonNode(
                            body.path(GroupConstants.USERGROUP_DESCRIPTION)))
                .map(desc -> StringUtils.left(desc, MAX_COMMUNITY_DESC))
                .orElseGet(() -> StringUtils.EMPTY)
            : StringUtils.EMPTY;
    CommunityInfo communityInfo =
        setCommunityInfo(
            communityData, myDmsGroupData, getAttachedCommunityName(myDmsGroupDatas), groupDesc);
    List<GroupData> groupBean = userGroupAdapter.getUserGroupListFromChildren(
            groupBeanByGidWithResponse.getBody().path(GroupConstants.USERGROUP_CHILDREN));
    if (addCommunity(communityInfo)
        && communityDao.updateCommunityLastOrgIdSyncTime(
            communityInfo.getCommunityId(), System.currentTimeMillis()) != 0
        && addAttachedCommunityRole(true, groupBean, communityInfo.getCommunityId())) {
      eventPublishService.publishNotificationSendingEvent(
          getJoinCommunityNotification(communityInfo, Collections.singletonList(EmailMemberType.ALLCOMMUNITYMEMBER.toString())));
      setSystemForum(communityInfo.getCommunityId());
      eventPublishService.publishActivityLogEvent(
          Utility.setActivityLogData(
              communityData.getCreateUserId(),
              Operation.CREATE.toString(),
              PermissionObject.COMMUNITY.toString(),
              communityInfo.getCommunityId(),
              Constants.INFO_NAME_DMS,
              Constants.CONTENT_EMPTY,
              Constants.ATTACHMENTID_EMPTY));

      eventPublishService.publishActivityLogMsgEvent(
          ActivityLogUtil.convertToActivityLogMsg(
              App.COMMUNITY,
              Constants.ACTIVITY_APP_VERSION,
              communityData.getCreateUserId(),
              Activity.CREATE,
              ObjectType.COMMUNITYID,
              String.valueOf(communityInfo.getCommunityId()),
              ActivityLogUtil.getAnnotation(PermissionObject.COMMUNITY, Constants.CONTENT_EMPTY),
              LogStatus.SUCCESS,
              LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
              null,
              null));

      return new ResponseData()
          .statusCode(HttpStatus.SC_CREATED)
          .id(communityInfo.getCommunityId());
    } else {
      return new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
    }
  }

  private String getAttachedCommunityName(List<MyDmsGroupData> myDmsGroupDatas) {
    String communityName =
        myDmsGroupDatas.size() != 1
            ? myDmsGroupDatas.get(0).getGroupName().trim()
                + OPEN_PARENTHESIS
                + myDmsGroupDatas.get(1).getGroupName().trim()
                + CLOSE_PARENTHESIS
            : myDmsGroupDatas.get(0).getGroupName().trim();
    for (int i = 2; i < myDmsGroupDatas.size(); ++i) {
      if (!checkDuplicateCommunityName(StringUtils.EMPTY, communityName)) {
        break;
      }
      String content =
          Constants.COMMA_DELIMITER
              + myDmsGroupDatas.get(i).getGroupName().trim()
              + CLOSE_PARENTHESIS;
      communityName = communityName.replaceAll(CLOSE_PARENTHESIS_REGEXR, content);
    }

    return communityName;
  }

  private boolean checkDuplicateCommunityGroupId(String groupId) {
    return communityDao.checkcheckDuplicateCommunityGroupId(groupId) != 0;
  }

  private void setSystemForum(Integer communityId) {
    ForumData forumData = new ForumData();
    forumData.setCommunityId(communityId);
    forumData.setType(ForumType.PUBLIC);
    forumData.setName(MSG_FORUM_EXCHANGE_AREA);
    forumData.setStatus(ForumStatus.OPEN);
    forumData.setAdmins(new ArrayList<>());
    forumData.setMembers(new ArrayList<>());
    eventPublishService.publishCommunityCreatingEvent(forumData);
  }

  private CommunityInfo setCommunityInfo(
      AttachedCommunityData communityData,
      MyDmsGroupData myDmsGroupData,
      String communityName,
      String desc) {
    long milliseconds = System.currentTimeMillis();
    CommunityInfo communityInfo = new CommunityInfo();
    String userId = communityData.getCreateUserId();
    communityInfo.setCommunityName(communityName.trim());
    communityInfo.setCommunityDesc(desc.trim());
    communityInfo.setCommunityType(CommunityType.PRIVATE.toString());
    communityInfo.setCommunityCategory(myDmsGroupData.getGroupType());
    communityInfo.setCommunityStatus(CommunityStatus.OPEN.toString());
    communityInfo.setCommunityCreateUserId(userId);
    communityInfo.setCommunityCreateTime(milliseconds);
    communityInfo.setCommunityLastModifiedUserId(userId);
    communityInfo.setCommunityLastModifiedTime(milliseconds);
    communityInfo.setCommunityGroupId(communityData.getGroupId());
    return communityInfo;
  }

  private boolean addCommunity(CommunityInfo communityInfo) {
    return communityDao.add(communityInfo) != 0;
  }

  private boolean addAttachedCommunityRole(boolean isNewCommunity, List<GroupData> groupBean, Integer communityId) {
    if(isNewCommunity) {
      String adminGroupId = getCommunityUserGroupId(Role.COMMUNITY_ADMIN, communityId, null);
      String memberGroupId = getCommunityUserGroupId(Role.COMMUNITY_MEMBER, communityId, null);
      communityDao.addRoleIntoCommunity(Collections.singletonList(adminGroupId), communityId, Role.COMMUNITY_ADMIN.getId(), true);
      communityDao.addRoleIntoCommunity(Collections.singletonList(memberGroupId), communityId, Role.COMMUNITY_MEMBER.getId(), true);
    }

    communityDao.deleteGroupMemberInCommunity(communityId);
    return addAdminGroupIdToCommunityRole(communityId, groupBean)
        && addManagerGroupIdToCommunityRole(communityId, groupBean)
        && addMemberGroupIdToCommunityRole(communityId, groupBean)
        && addKnowledgeAdminGroupIdToCommunityRole(communityId, groupBean)
        && addSupplierKuGroupIdToCommunityRole(communityId, groupBean)
        && addKmKnowledgeUnitGroupIdToCommunityRole(communityId, groupBean)
        && addKmGroupIdToCommunityRole(communityId, groupBean)
        && addCustomGroupIdToCommunityRole(communityId, groupBean);
  }

  private boolean addAdminGroupIdToCommunityRole(Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_ADMIN.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_ADMIN.getId(), Role.COMMUNITY_MEMBER.getId()), false);
  }

  private boolean addGroupToCommunityRoleByCondition(
      Integer communityId,
      List<GroupData> groupBean,
      Predicate<GroupData> predicate,
      List<Integer> roleIds,
      Boolean toIncludeGroupList) {

    // 獲取符合條件的gid list
    List<String> gids = getGids(groupBean, predicate, toIncludeGroupList);

    // 獲取符合條件的gid data map
    Map<String, GroupData> gidToGroupDataMap = getGidToGroupDataMap(groupBean, predicate, toIncludeGroupList);

    // 比較gids 跟gidToGroupDataMap 的key列表有沒有相同
    Set<String> gidSet = gids.stream().collect(Collectors.toSet());
    Set<String> mapKeySet = gidToGroupDataMap.keySet();

    // 如果不相同報錯
    if (!gidSet.equals(mapKeySet)) {
      throw new RuntimeException("The list of gids and the key set of the gidToGroupDataMap are not the same.");
    }

    // 對每個role id進行操作，確保所有group都能成功添加到對應的community role中
    return roleIds
            .stream()
            .map(roleId -> {
              boolean allMatches = true;
              for (String gid : gids) {
                GroupData groupData = gidToGroupDataMap.get(gid);
                boolean isFromGroupList = groupData !=
                        null && groupData.getGroupList() != null && groupData.getGroupList().contains(gid);

                // 創建並設定community role
                CommunityRoleInfo roleInfo = setCommunityRoleInfo(
                        communityId, gid, roleId, false,
                        groupData.getName(), groupData.getId(), isFromGroupList);

                // 將roleInfo添加到community中，若添加失敗則記錄操作未完全成功
                if (communityDao.addCommunityRole(roleInfo) == 0) {
                  allMatches = false;
                  break;
                }
              }
              return allMatches;
            })
            .allMatch(Boolean.TRUE::equals);
  }

  private List<String> getGids(List<GroupData> groupBean, Predicate<GroupData> predicate, Boolean toIncludeGroupList) {
    List<GroupData> rawList = groupBean
            .stream()
            .filter(predicate)
            .distinct()
            .collect(Collectors.toList());
    List<String> finalList = new ArrayList<>();
    for(GroupData item : rawList) {
      finalList.add(item.getId());

      // need to add all group list gid as well
      if(toIncludeGroupList) {
        for(String list : item.getGroupList()) {
          finalList.add(list);
        }
      }
    }
    return finalList
        .stream()
        .distinct()
        .collect(Collectors.toList());
  }

  private Map<String, GroupData> getGidToGroupDataMap(
          List<GroupData> groupBean, Predicate<GroupData> predicate, boolean toIncludeGroupList) {

    // 使用stream過濾並收集符合條件的 groupData
    List<GroupData> filteredGroupData = groupBean.stream()
            .filter(predicate)
            .distinct()
            .collect(Collectors.toList());

    // 創建一個Map來存放 group id 與 groupData 的關係
    Map<String, GroupData> gidToGroupDataMap = new HashMap<>();

    for (GroupData groupData : filteredGroupData) {
      // 將 getGroupList 中的 id 與對應 groupData 放入 Map 中
      gidToGroupDataMap.put(groupData.getId(), groupData);

      // 如果 toIncludeGroupList = true 且 getGroupList 不為空
      if (toIncludeGroupList && groupData.getGroupList() != null) {
        for (String groupId : groupData.getGroupList()) {

          // 同樣將 getGroupList 中的 id 與對應 groupData 放入 Map 中
          gidToGroupDataMap.put(groupId, groupData);
        }
      }
    }
    return gidToGroupDataMap;
  }

  private boolean addManagerGroupIdToCommunityRole(Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_MANAGER.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_ADMIN.getId(), Role.COMMUNITY_MEMBER.getId()), false);
  }

  private boolean addMemberGroupIdToCommunityRole(Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_ALLMEMBERS.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), true);
  }

  private boolean addCustomGroupIdToCommunityRole(Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group ->
            StringUtils.defaultString(group.getName()).matches(GroupConstants.CUSTOM_GROUP_FILTER),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), true);
  }

  private boolean addKnowledgeAdminGroupIdToCommunityRole(
      Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), false);
  }

  private boolean addSupplierKuGroupIdToCommunityRole(
      Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_SUPPLIER_KU.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), true);
  }

  private boolean addKmKnowledgeUnitGroupIdToCommunityRole(
      Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), true);
  }

  private boolean addKmGroupIdToCommunityRole(Integer communityId, List<GroupData> groupBean) {
    return addGroupToCommunityRoleByCondition(
        communityId,
        groupBean,
        group -> GroupConstants.SYSTEM_GROUP_NAME_KM.equals(group.getName()),
        Arrays.asList(Role.COMMUNITY_MEMBER.getId()), true);
  }

  private CommunityRoleInfo setCommunityRoleInfo(
          Integer communityId, String gid, Integer roleId, boolean isGeneratedFromApp,
          String mainGroupName, String mainGroupId, boolean isFromGroupList) {

    CommunityRoleInfo communityRoleInfo = new CommunityRoleInfo();
    communityRoleInfo.setCommunityId(communityId);
    communityRoleInfo.setGroupId(gid);
    communityRoleInfo.setRoleId(roleId);
    communityRoleInfo.setIsGeneratedFromApp(isGeneratedFromApp);
    communityRoleInfo.setMainGroupId(mainGroupId);
    communityRoleInfo.setMainGroupName(mainGroupName);
    communityRoleInfo.setIsFromGroupList(isFromGroupList);
    return communityRoleInfo;
  }

  public MyDmsGroupData getOrgGroupItem(String groupId) {
    if(StringUtils.isBlank(groupId)) {
      return null;
    }

    List<MyDmsGroupData> myDmsGroupDatas = userGroupAdapter.getPathInfoByGid(groupId);
    ResponseEntity<JsonNode> groupBeanByGidWithResponse =
            userGroupAdapter.getGroupBeanByGidWithResponse(groupId);
    if (!Objects.equals(groupBeanByGidWithResponse.getStatusCode().value(), HttpStatus.SC_OK)) {
      throw new CreationException(groupBeanByGidWithResponse.toString());
    }

    MyDmsGroupData myDmsGroupData = myDmsGroupDatas.stream().findFirst().orElse(null);
    if (myDmsGroupData == null || StringUtils.isEmpty(myDmsGroupData.getGroupName().trim())) {
      return null;
    }

    return myDmsGroupData;
  }

  public Integer updateAttachedCommunity(
      String groupId, UpdateAttachedCommunityData communityData) {
    if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(communityData.getUpdateUserId())) {
      return HttpStatus.SC_BAD_REQUEST;
    }

    CommunityInfo communityInfo = getCommunityInfoByGroupId(groupId);
    List<MyDmsGroupData> myDmsGroupDatas = userGroupAdapter.getPathInfoByGid(groupId);
    ResponseEntity<JsonNode> groupBeanByGidWithResponse =
        userGroupAdapter.getGroupBeanByGidWithResponse(groupId);
    if (!Objects.equals(groupBeanByGidWithResponse.getStatusCode().value(), HttpStatus.SC_OK)) {
      communityDao.updateCommunityLastOrgIdSyncTime(communityInfo.getCommunityId(),
              System.currentTimeMillis()); // update last sync flag
      throw new CreationException(groupBeanByGidWithResponse.toString());
    }

    MyDmsGroupData myDmsGroupData = myDmsGroupDatas.stream().findFirst().orElse(null);
    if (myDmsGroupData == null || StringUtils.isEmpty(myDmsGroupData.getGroupName().trim())) {
      communityDao.updateCommunityLastOrgIdSyncTime(communityInfo.getCommunityId(),
              System.currentTimeMillis()); // update last sync flag
      return HttpStatus.SC_NO_CONTENT;
    }

    //syncCommunityUserId(communityInfo.getCommunityId(), groupId);
    //eventPublishService.publishSyncForumUserIdEvent(communityInfo.getCommunityId(), groupId);
    String groupDesc =
        CommunityCategory.PROJECT.toString().equals(myDmsGroupData.getGroupType())
            ? Optional.ofNullable(groupBeanByGidWithResponse.getBody())
                .map(
                    body ->
                        Utility.getStringFromJsonNode(
                            body.path(GroupConstants.USERGROUP_DESCRIPTION)))
                .map(desc -> StringUtils.left(desc, MAX_COMMUNITY_DESC))
                .orElseGet(() -> StringUtils.EMPTY)
            : StringUtils.EMPTY;
    CommunityInfo updateCommunityInfo =
        setUpdateCommunityInfo(
            communityInfo.getCommunityId(),
            communityData.getUpdateUserId(),
            getUpdatedAttachedCommunityName(myDmsGroupDatas, communityInfo.getCommunityName()),
            myDmsGroupData,
            communityInfo,
            groupDesc);
    List<GroupData> groupBean = userGroupAdapter.getUserGroupListFromChildren(
            groupBeanByGidWithResponse.getBody().path(GroupConstants.USERGROUP_CHILDREN));
    if (communityDao.update(updateCommunityInfo) != 0
        && communityDao.updateCommunityLastOrgIdSyncTime(
                updateCommunityInfo.getCommunityId(),System.currentTimeMillis()) != 0
        && addAttachedCommunityRole(false, groupBean, communityInfo.getCommunityId())) {
      eventPublishService.publishCommunityChangingEvent(communityInfo.getCommunityId());
      eventPublishService.publishActivityLogEvent(
          Utility.setActivityLogData(
              communityData.getUpdateUserId(),
              Operation.UPDATE.toString(),
              PermissionObject.COMMUNITY.toString(),
              communityInfo.getCommunityId(),
              Constants.INFO_NAME_DMS,
              Constants.CONTENT_EMPTY,
              Constants.ATTACHMENTID_EMPTY));

      eventPublishService.publishActivityLogMsgEvent(
          ActivityLogUtil.convertToActivityLogMsg(
              App.COMMUNITY,
              Constants.ACTIVITY_APP_VERSION,
              communityData.getUpdateUserId(),
              Activity.UPDATE,
              ObjectType.COMMUNITYID,
              String.valueOf(communityInfo.getCommunityId()),
              ActivityLogUtil.getAnnotation(PermissionObject.COMMUNITY, Constants.CONTENT_EMPTY),
              LogStatus.SUCCESS,
              LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
              null,
              null));

      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_NOT_FOUND;
    }
  }

  public void syncCommunityUserId(int communityId, String groupId) {
    List<String> userList = communityDao.getAllMemberWithoutGroupId(communityId);

    List<GroupData> groupList =
        userGroupAdapter
            .getOrgGroupUserGroupByGid(Arrays.asList(groupId))
            .getOrDefault(groupId, new ArrayList<>());
    List<User> adminList = getGroupAdmin(groupList);
    List<User> memberList = getGroupMember(groupList);

    List<String> userIdListWithGroupData =
        Stream.concat(adminList.stream(), memberList.stream())
            .distinct()
            .map(User::getId)
            .collect(Collectors.toList());

    List<String> deleteUserList =
        userList.stream().filter(userIdListWithGroupData::contains).collect(Collectors.toList());

    if (!deleteUserList.isEmpty()) {
      //communityDao.deleteMemberFromCommunity(deleteUserList, communityId);
    }
  }

  private String getUpdatedAttachedCommunityName(
      List<MyDmsGroupData> myDmsGroupDatas, String oldCommunityName) {
    if (CollectionUtils.isEmpty(myDmsGroupDatas)) {
      return oldCommunityName;
    }
    String communityName =
        myDmsGroupDatas.size() > 1
            ? myDmsGroupDatas.get(0).getGroupName().trim()
                + OPEN_PARENTHESIS
                + myDmsGroupDatas.get(1).getGroupName().trim()
                + CLOSE_PARENTHESIS
            : myDmsGroupDatas.get(0).getGroupName().trim();
    for (int i = 2; i < myDmsGroupDatas.size(); ++i) {
      if (!checkDuplicateCommunityName(oldCommunityName, communityName)
          || Objects.equals(oldCommunityName, communityName)) {
        break;
      }
      String content =
          Constants.COMMA_DELIMITER
              + myDmsGroupDatas.get(i).getGroupName().trim()
              + CLOSE_PARENTHESIS;
      communityName =
          new StringBuilder(
                  communityName.substring(
                      NumberUtils.INTEGER_ZERO, communityName.length() - NumberUtils.INTEGER_ONE))
              .append(content)
              .toString();
    }
    return communityName;
  }

  private CommunityInfo getCommunityInfoByGroupId(String groupId) {
    CommunityInfo communityInfo = communityDao.getCommunityInfoByGroupId(groupId);
    if (communityInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOT_EXIST);
    }
    return communityInfo;
  }

  private CommunityInfo setUpdateCommunityInfo(
      Integer communityId,
      String updateUserId,
      String communityName,
      MyDmsGroupData myDmsGroupData,
      CommunityInfo communityInfo,
      String desc) {
    long milliseconds = System.currentTimeMillis();
    CommunityInfo updateCommunityInfo = new CommunityInfo();

    updateCommunityInfo.setCommunityId(communityId);
    updateCommunityInfo.setCommunityName(communityName.trim());
    updateCommunityInfo.setCommunityDesc(desc.trim());
    updateCommunityInfo.setCommunityModifiedUserId(updateUserId);
    updateCommunityInfo.setCommunityModifiedTime(milliseconds);
    updateCommunityInfo.setCommunityType(communityInfo.getCommunityType());
    updateCommunityInfo.setCommunityStatus(communityInfo.getCommunityStatus());
    updateCommunityInfo.setCommunityCategory(myDmsGroupData.getGroupType());
    return updateCommunityInfo;
  }

  private String resizeImage(String base64, int width, int height) {
    String newBase64 = null;
    try {
      BufferedImage orignImage =
          base64ToImage(
              base64.replaceAll(Constants.BASE64_CONTEXT, Constants.IMAGE_CONTEXT_REPLACEMENT));
      if (orignImage != null) {
        BufferedImage newImage = resize(orignImage, width, height);
        newBase64 = Constants.BASE64_JPEG_CONTEXT + imageTobase64(newImage);
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return newBase64;
  }

  private BufferedImage base64ToImage(String base64String) throws IOException {
    byte[] bytes = decoder.decode(base64String);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    return ImageIO.read(byteArrayInputStream);
  }

  private BufferedImage resize(BufferedImage originImage, int width, int height) {
    int originalWidth = originImage.getWidth();
    int originalHeight = originImage.getHeight();
    double rate = (double) originalWidth / originalHeight;
    double targetRate = (double) width / height;

    if (originalWidth < width && originalHeight < height) {
      width = originalWidth;
      height = originalHeight;
    } else if (rate > targetRate) {
      height = (int) (width / rate);
    } else {
      width = (int) (height * rate);
    }

    BufferedImage newImage = new BufferedImage(width, height, originImage.getType());
    Graphics graphics = newImage.getGraphics();
    graphics.drawImage(originImage, 0, 0, width, height, null);
    graphics.dispose();
    return newImage;
  }

  private String imageTobase64(BufferedImage image) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    ImageIO.write(image, Constants.JPG, byteArrayOutputStream);
    byte[] imageBytes = byteArrayOutputStream.toByteArray();
    return encoder.encodeToString(imageBytes).trim();
  }

  public int deleteCommunityApplication(int communityId, ApplicationDetail applicationDetail) {
    if (!checkUserCanDelete(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new AuthenticationException(I18nConstants.MSG_COMMUNITY_DELETE_NOT_ADMIN);
    }
    if (checkDeleteApplicationExist(communityId)) {
      throw new IllegalArgumentException(
          I18nConstants.MSG_COMMUNITY_DELETE_APPLICATION_ALREADY_EXIST);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (!communityInfo.getCommunityGroupId().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_DELETE_ATTACHED);
    }
    long now = new Date().getTime();
    if (addDeleteCommunityApplication(communityInfo, applicationDetail, now)) {
      eventPublishService.publishEmailSendingEvent(
          getCommunityDeleteApplicationContext(communityInfo, applicationDetail));
      eventPublishService.publishNotificationSendingEvent(
          getCommunityDeleteApplicationNotification(communityInfo, applicationDetail, now));
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }

  private boolean checkUserCanDelete(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId, communityId, 0, PermissionObject.COMMUNITY.toString(), Operation.DELETE.toString());
  }

  private boolean checkDeleteApplicationExist(int communityId) {
    return getDeleteApplicationDetail(communityId) != null;
  }

  private DeleteApplicationDetail getDeleteApplicationDetail(int communityId) {
    return communityDao.getDeleteApplication(communityId);
  }

  private boolean addDeleteCommunityApplication(
      CommunityInfo communityInfo, ApplicationDetail applicationDetail, long now) {
    return communityDao.addDeleteCommunityApplication(
            communityInfo.getCommunityId(),
            communityInfo.getCommunityName(),
            Utility.getUserIdFromSession(),
            applicationDetail.getSubject(),
            applicationDetail.getDesc(),
            now)
        != 0;
  }

  private EmailWithChineseAndEnglishContext getCommunityDeleteApplicationContext(
      CommunityInfo communityInfo, ApplicationDetail applicationDetail) {
    List<String> systemAdminList = userService.getSystemAdminIds();
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYDELETIONAPPLICATION)
        .sender(userName)
        .desc(String.format(EmailConstants.COMMUNITY_DELETE_APPLICATION_CHINESE_FORMAT, userName))
        .englishDesc(
            String.format(EmailConstants.COMMUNITY_DELETE_APPLICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DELETE_COMMUNITY_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(systemAdminList))
        .content(applicationDetail.getDesc())
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  public void deleteAllMemberJoinApplicationOfCommunity(int communityId) {
    communityDao.deleteAllMemberJoinApplicationOfCommunity(communityId);
  }

  public int reviewDeletingApplicationOfCommunity(int communityId, ApprovalStatus approvalStatus) {
    if (approvalStatus.getStatus() == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    if (!userService.isSysAdmin()) {
      throw new AuthenticationException(I18nConstants.MSG_COMMUNITY_DELETE_NOT_SYSTEM_ADMIN);
    }
    DeleteApplicationDetail deleteApplicationDetail = getDeleteApplicationDetail(communityId);
    if (deleteApplicationDetail == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_DELETE_APPLICATION_NOT_EXIST);
    }
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (communityInfo.getCommunityGroupId().isEmpty()) {
      setDeleteCommunityInfo(communityInfo, deleteApplicationDetail.getApplicantId());
      return deleteGeneralCommunity(communityInfo, approvalStatus);
    } else {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_DELETE_ATTACHED);
    }
  }

  private int deleteGeneralCommunity(CommunityInfo communityInfo, ApprovalStatus approvalStatus) {
    String userId = communityInfo.getCommunityDeleteUserId();
    String reviewer = Utility.getUserIdFromSession();
    communityDao.reviewDeleteApplication(
        communityInfo.getCommunityId(),
        reviewer,
        communityInfo.getCommunityDeleteTime(),
        approvalStatus.getStatus().toString(),
        approvalStatus.getRejectedMessage());
    if (ReviewAction.APPROVED.equals(approvalStatus.getStatus())) {
      eventPublishService.publishCommunityDeletingEvent(
          communityInfo.getCommunityId(), communityInfo.getCommunityDdfId());
      if (communityDao.delete(communityInfo) > 0) {
        /*
        List<String> memberList =
            getAllMemberOfCommunityById(
                    communityInfo.getCommunityId(),false, null, -1, -1)
                .stream()
                .map(User::getId)
                .distinct()
                .filter(item -> !item.equals(userId))
                .collect(Collectors.toList());
         */
        //if (!memberList.isEmpty()) {
          eventPublishService.publishEmailSendingEvent(
              getCommunityDeletionContext(communityInfo));
          eventPublishService.publishNotificationSendingEvent(
              getCommunityDeletionNotification(communityInfo));
        //}
        eventPublishService.publishNotificationSendingEvent(
            getApproveCommunityDeletionNotification(communityInfo));
        return HttpStatus.SC_OK;
      } else {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
      }
    } else {
      eventPublishService.publishEmailSendingEvent(
          getRejectedCommunityDeletionContext(communityInfo, approvalStatus.getRejectedMessage()));
      eventPublishService.publishNotificationSendingEvent(
          getRejectedCommunityDeletionNotification(
              communityInfo, approvalStatus.getRejectedMessage()));
      return HttpStatus.SC_OK;
    }
  }

  private void setDeleteCommunityInfo(CommunityInfo communityInfo, String deleteUserId) {
    communityInfo.setCommunityStatus(CommunityStatus.DELETE.toString());
    communityInfo.setCommunityDeleteUserId(deleteUserId);
    communityInfo.setCommunityDeleteTime(new Date().getTime());
  }

  private EmailWithChineseAndEnglishContext getCommunityDeletionContext(CommunityInfo communityInfo) {
    String communityName = communityInfo.getCommunityName();
    String userName = Utility.getUserFromSession().getCommonName();
    String link = String.format(EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getHost());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getMobileDownloadUrl());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYDELETION)
        .extraMemberType(EmailMemberType.ALLCOMMUNITYMEMBER)
        .communityInfo(communityInfo)
        .sender(userName)
        .desc(
            String.format(EmailConstants.COMMUNITY_DELETE_CHINESE_FORMAT, communityName, userName))
        .englishDesc(
            String.format(EmailConstants.COMMUNITY_DELETE_ENGLISH_FORMAT, communityName, userName))
        .subject(EmailConstants.DELETE_COMMUNITY_SUBJECT)
        //.to(userService.getEmailByUserId(userList))
        .link(link)
        .mobileLink(mobileLink);
  }

  private EmailWithChineseAndEnglishContext getRejectedCommunityDeletionContext(
      CommunityInfo communityInfo, String reason) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            host,
            language,
            communityInfo.getCommunityId());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            communityInfo.getCommunityId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.COMMUNITYDELETIONREJECTION)
        .sender(communityInfo.getCommunityName())
        .desc(EmailConstants.COMMUNITY_DELETE_REJECTED_CHINESE_FORMAT)
        .englishDesc(EmailConstants.COMMUNITY_DELETE_REJECTED_ENGLISH_FORMAT)
        .subject(EmailConstants.DELETE_COMMUNITY_REJECTED_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(communityInfo.getCommunityDeleteUserId())))
        .content(String.format(EmailConstants.APPLICATION_CHINESE_CONTENT_FORMAT, reason))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName()),
                null));
  }

  public DeleteApplicationResult getApplicationOfDeletingCommunity(
      boolean processed, int offset, int limit) {
    List<String> status;
    if (processed) {
      status =
          Arrays.stream(ReviewAction.values())
              .map(ReviewAction::toString)
              .collect(Collectors.toList());
    } else {
      status = Arrays.asList("");
    }
    List<DeleteApplicationDetail> applicationList =
        getApplicationListOfDeletingCommunity(status, offset, limit);
    int numFound = communityDao.countDeleteApplicationList(status);
    return new DeleteApplicationResult().result(applicationList).numFound(numFound);
  }

  private List<DeleteApplicationDetail> getApplicationListOfDeletingCommunity(
      List<String> status, int offset, int limit) {
    if (!userService.isSysAdmin()) {
      throw new AuthenticationException(I18nConstants.MSG_NOT_SYSTEM_ADMIN);
    }
    List<DeleteApplicationDetail> applicationList =
        communityDao.getDeleteApplicationList(status, offset, limit);
    applicationList.forEach(
        item -> {
          String applicantName = userService.getUserById(item.getApplicantId()).getName();
          String reviewerName = userService.getUserById(item.getReviewerId()).getName();
          item.applicantName(applicantName).reviewerName(reviewerName);
        });
    return applicationList;
  }

  public int lockCommunity(String groupId, String userId) {
    CommunityInfo communityInfo = getCommunityInfoByGroupId(groupId);
    int communityId = communityInfo.getCommunityId();

    if (!checkUserCanLockAttachedCommunity(communityId, userId)) {
      throw new AuthenticationException("");
    }
    long lockedTime = new Date().getTime();
    eventPublishService.publishCommunityLockingEvent(communityId, userId, lockedTime);
    setLockedAttachedCommunity(communityInfo, userId, lockedTime);
    if (communityDao.update(communityInfo) > 0) {
      communityDao.deleteAllMemberJoinApplicationOfCommunity(communityId);
      /*
      final List<String> adminIdList =
          getAdminListOfCommunity(communityId, communityInfo.getCommunityGroupId(), true)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
      final List<String> memberIdList =
          getAllMemberOfCommunityById(communityId, communityInfo.getCommunityGroupId(), true)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
      communityDao.deleteAllMemberOfCommunity(communityId);
      communityDao.deleteAllMemberJoinApplicationOfCommunity(communityId);
      addAdminIntoCommunity(adminIdList, communityId);
      addMemberIntoCommunity(memberIdList, communityId);
       */
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }

  private boolean checkUserCanLockAttachedCommunity(int communityId, String userId) {
    List<String> extendedAdminList =
        userGroupAdapter.getPrivilegedUserGroupIdByUserIdAndFilter(userId, true);
    List<String> groupList = communityDao.getAdminGroupOfCommunity(communityId);
    for (String groupId : groupList) {
      if (extendedAdminList.contains(groupId)) {
        return true;
      }
    }
    return false;
  }

  private void setLockedAttachedCommunity(
      CommunityInfo communityInfo, String userId, long lockedTime) {
    String communityName = communityInfo.getCommunityName();
    communityInfo.setCommunityName(
        String.format(LOCKED_ATTACHED_COMMUNITY_NAME_FORMAT, communityName));
    communityInfo.setCommunityCategory(CommunityCategory.GENERAL.toString());
    communityInfo.setCommunityStatus(CommunityStatus.LOCKED.toString());
    communityInfo.setCommunityModifiedUserId(userId);
    communityInfo.setCommunityModifiedTime(lockedTime);
  }

  public boolean addAdminIntoCommunity(List<String> adminId, int communityId) {
    return appendCommunityMembers(adminId, communityId, Role.COMMUNITY_ADMIN);
  }

  public boolean removeAdminFromCommunity(List<String> adminId, int communityId) {
    return deleteCommunityMembers(adminId, communityId, Role.COMMUNITY_ADMIN);
  }

  public List<String> getCommunityDependentGroupList(int communityId) {
    return getCommunityRoleList(-1, true, communityId)
            .stream()
            .map(CommunityRoleInfo::getGroupId)
            .collect(Collectors.toList());
  }

  public List<CommunityRoleInfo> getCommunityRoleList(
          int roleId, Boolean isApplicationGroup,int communityId) {
    return communityDao.getCommunityRoleList(roleId, isApplicationGroup, communityId);
  }

  public List<CommunityRoleInfo> getMainGroupCommunityRoleList(
          int roleId, Boolean isApplicationGroup,int communityId) {
    return communityDao.getMainGroupCommunityRoleList(roleId, isApplicationGroup, communityId);
  }

  public List<String> getMemberUserAndGroupOfCommunity(int communityId) {
    return communityDao.getMemberUserAndGroupOfCommunity(communityId);
  }

  public List<String> getAdminUserAndGroupOfCommunity(int communityId) {
    return communityDao.getAdminUserAndGroupOfCommunity(communityId);
  }

  private Notification getCommunityCreateApplicationNotification(
      CommunitiesCreationInfo community) {
    List<String> systemAdminList = userService.getSystemAdminIds();
    UserSession user = Utility.getUserFromSession();
    return new Notification()
        .userId(systemAdminList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.COMMUNITYCREATIONAPPLICATION)
        .title(user.getCommonName())
        .priority(NotificationConstants.PRIORITY_5)
        .communityId(community.getBatchId())
        .communityName(community.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .senderId(user.getCommonUUID())
        .time(community.getApplicationTime());
  }

  private Notification getApproveCreateApplicationNotification(
      CommunityInfo communityInfo, String applicantId, long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.COMMUNITYCREATIONAPPROVAL)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .time(now);
  }

  private Notification getRejectCreateApplicationNotification(
      CommunitiesCreationInfo communitiesCreationInfo,
      String applicantId,
      String content,
      long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.COMMUNITYCREATIONREJECTION)
        .title(communitiesCreationInfo.getCommunityName())
        .content(content)
        .communityName(communitiesCreationInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .time(now);
  }

  private Notification getCommunityJoinApplicationNotification(
      CommunityInfo communityInfo, String content, long now) {
    /*
    List<String> adminIdList =
        getAdminListOfCommunity(communityInfo.getCommunityId(), communityInfo.getCommunityGroupId(), true)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
     */
    UserSession user = Utility.getUserFromSession();
    return new Notification()
        //.userId(adminIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.COMMUNITYJOINAPPLICATION)
        .title(user.getCommonName())
        .content(content)
        .priority(NotificationConstants.PRIORITY_5)
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .senderId(user.getCommonUUID())
        .time(now);
  }

  private Notification getApproveJoinApplicationNotification(
      CommunityInfo communityInfo, String applicantId, long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.COMMUNITYJOINAPPROVAL)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .time(now);
  }

  private Notification getRejectJoinApplicationNotification(
      CommunityInfo communityInfo, String applicantId, long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.COMMUNITYJOINREJECTION)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .time(now);
  }

  private Notification getJoinCommunityNotification(
      CommunityInfo communityInfo, List<String> memberId) {
    return new Notification()
        .userId(memberId.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.JOINCOMMUNITY)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .time(new Date().getTime());
  }

  private Notification getRemoveFromCommunityNotification(
      CommunityInfo communityInfo, List<String> memberId) {
    return new Notification()
        .userId(memberId.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.REMOVEFROMCOMMUNITY)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .time(new Date().getTime());
  }

  private Notification getNotification(
      NotificationDetail notificationDetail, CommunityInfo communityInfo) {
    List<String> userIdList = getRecipientList(communityInfo.getCommunityId(), notificationDetail);
    return new Notification()
        .userId(userIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .extraMemberType((NotificationType.ALL.equals(notificationDetail.getType())) ? EmailMemberType.ALLCOMMUNITYMEMBER : EmailMemberType.NONE)
        .type(EmailType.COMMUNITYNOTIFICATION)
        .title(communityInfo.getCommunityName())
        .content(notificationDetail.getContent())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .senderId(Utility.getUserIdFromSession())
        .time(new Date().getTime());
  }

  private Notification getCommunityDeleteApplicationNotification(
      CommunityInfo communityInfo, ApplicationDetail applicationDetail, long now) {
    List<String> systemAdminList = userService.getSystemAdminIds();
    UserSession user = Utility.getUserFromSession();
    return new Notification()
        .userId(systemAdminList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.COMMUNITYDELETIONAPPLICATION)
        .title(user.getCommonName())
        .content(applicationDetail.getDesc())
        .priority(NotificationConstants.PRIORITY_5)
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .senderId(Utility.getUserIdFromSession())
        .time(now);
  }

  private Notification getCommunityDeletionNotification(
      CommunityInfo communityInfo) {
    return new Notification()
        //.userId(userList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.COMMUNITYDELETION)
        .extraMemberType(EmailMemberType.ALLCOMMUNITYMEMBER)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .senderId(Utility.getUserIdFromSession())
        .time(communityInfo.getCommunityDeleteTime());
  }

  private Notification getApproveCommunityDeletionNotification(CommunityInfo communityInfo) {
    return new Notification()
        .userId(communityInfo.getCommunityDeleteUserId())
        .type(EmailType.COMMUNITYDELETIONAPPROVAL)
        .title(communityInfo.getCommunityName())
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .time(communityInfo.getCommunityDeleteTime());
  }

  private Notification getRejectedCommunityDeletionNotification(
      CommunityInfo communityInfo, String reason) {
    return new Notification()
        .userId(communityInfo.getCommunityDeleteUserId())
        .type(EmailType.COMMUNITYDELETIONREJECTION)
        .title(communityInfo.getCommunityName())
        .content(reason)
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.GENERAL)
        .time(communityInfo.getCommunityDeleteTime());
  }

  public int createCommunityAnnouncement(int communityId, String text) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (!checkUserCanUpdate(
        Utility.getCurrentUserIdWithGroupId(), communityInfo.getCommunityId())) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }

    List<String> communityMemberRoleList =
            getCommunityRoleList(Role.COMMUNITY_MEMBER.getId(), null, communityId)
                    .stream()
                    .map(CommunityRoleInfo::getUserId)
                    .distinct()
                    .collect(toList());
    List<String> communityAdminRoleList =
            getCommunityRoleList(Role.COMMUNITY_ADMIN.getId(), null, communityId)
                    .stream()
                    .map(CommunityRoleInfo::getUserId)
                    .distinct()
                    .collect(toList());
    Map<String, List<UserGroupEntity>> roleMap =
        fileService.getRoleMap(Utility.getUserIdFromSession(), new ArrayList<>());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(
            Utility.getUserIdFromSession(), communityAdminRoleList, communityMemberRoleList);
    String htmlText =
        richTextHandlerService
            .replaceRichTextImageSrcWithImageDataHiveUrl(
                ForumType.PUBLIC, roleMap, privilegeMap, text)
            .getText();
    String originText = communityDao.getCommunityAnnouncement(communityId);
    if (!StringUtils.isEmpty(originText)) {
      richTextHandlerService.deleteRemovedImageInRichText(originText, htmlText, communityId);
    }
    if (communityDao.insertCommunityAnnouncementText(
            communityInfo.getCommunityId(),
            htmlText,
            Utility.getUserIdFromSession(),
            System.currentTimeMillis())
        == 0) {
      return HttpStatus.SC_NOT_FOUND;
    }
    eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(communityId);
    return HttpStatus.SC_OK;
  }

  public int deleteCommunityAnnouncement(int communityId) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (!checkUserCanDelete(
        Utility.getCurrentUserIdWithGroupId(), communityInfo.getCommunityId())) {
      throw new AuthenticationException(I18nConstants.MSG_COMMUNITY_DELETE_NOT_ADMIN);
    }

    richTextHandlerService.deleteRemovedImageInRichText(
        communityDao.getCommunityAnnouncement(communityId), "", communityId);
    if (communityDao.deleteCommunityAnnouncementText(communityId) == 0) {
      return HttpStatus.SC_NOT_FOUND;
    }
    eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(communityId);
    return HttpStatus.SC_OK;
  }

  public String getCommunityAnnouncement(int communityId) {
    return communityDao.getCommunityAnnouncement(communityId);
  }

  public ActiveMemberListResult getActiveMemberListOfCommunity(int communityId, int limit) {
    Map<String, ActiveMemberInfo> lastWeekOfMembers =
        getActiveMemberMap(communityId, limit, Constants.LAST_WEEK_START, Constants.LAST_WEEK_END);
    Map<String, ActiveMemberInfo> weekBeforeLastWeekOfMembers =
        getActiveMemberMap(
            communityId,
            limit,
            Constants.WEEK_BEFORE_LAST_WEEK_START,
            Constants.WEEK_BEFORE_LAST_WEEK_END);

    List<ActiveMemberInfoDetail> activeMemberInfoDetails =
        lastWeekOfMembers
            .entrySet()
            .stream()
            .filter(Objects::nonNull)
            .map(
                item -> {
                  int lastWeekRank = item.getValue().getRank();
                  int weekBeforeLastWeekRank =
                      weekBeforeLastWeekOfMembers.get(item.getKey()) == null
                          ? 0
                          : weekBeforeLastWeekOfMembers.get(item.getKey()).getRank();
                  return new ActiveMemberInfoDetail()
                      .replies(item.getValue().getCount())
                      .tend(weekBeforeLastWeekRank == 0 ? 0 : weekBeforeLastWeekRank - lastWeekRank)
                      .rank(lastWeekRank)
                      .user(transferInternalTalentUserToActiveMemberBaseInfo(item.getKey()));
                })
            .collect(Collectors.toList());
    return new ActiveMemberListResult()
        .result(activeMemberInfoDetails)
        .numFound(activeMemberInfoDetails.size());
  }

  private ActiveMemberBaseInfo transferInternalTalentUserToActiveMemberBaseInfo(String userId) {
    UserSession user = userService.getUserById(Arrays.asList(userId), new ArrayList<>()).get(0);
    return new ActiveMemberBaseInfo()
        .id(userId)
        .name(user.getCommonName())
        .department(user.getProfileDeptName())
        .ext(user.getProfilePhone())
        .imgAvatar(user.getCommonImage());
  }

  private Map<String, ActiveMemberInfo> getActiveMemberMap(
      int communityId, int limit, int start, int end) {
    return communityDao
        .getCommunityOfActiveMembers(communityId, limit, start, end)
        .stream()
        .collect(Collectors.toMap(ActiveMemberInfo::getId, item -> item));
  }

  protected String getCommunityNotificationType(int communityId) {
    String notificationType = communityDao.getCommunityNotificationType(communityId);
    if (StringUtils.isEmpty(notificationType)) {
      return "";
    } else {
      return notificationType;
    }
  }

  public int closeGeneralCommunity(int communityId, long communityModifiedTime) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (!checkUserCanClose(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }

    if ((communityInfo.getCommunityModifiedTime() != 0
        && communityInfo.getCommunityModifiedTime() != communityModifiedTime)) {
      return HttpStatus.SC_CONFLICT;
    }

    if (!Objects.equals(CommunityCategory.GENERAL.toString(), communityInfo.getCommunityCategory())
        || !Objects.equals(CommunityStatus.OPEN.toString(), communityInfo.getCommunityStatus())) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_GENERAL_CLOSE_HINT);
    }

    long closedTime = new Date().getTime();
    String userId = Utility.getUserIdFromSession();
    eventPublishService.publishCommunityLockingEvent(communityId, userId, closedTime);
    setClosedGeneralCommunity(communityInfo, userId, closedTime);
    if (communityDao.update(communityInfo) > 0) {
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_NOT_FOUND;
    }
  }

  public boolean checkUserCanClose(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId, communityId, 0, PermissionObject.COMMUNITY.toString(), Operation.CLOSE.toString());
  }

  private void setClosedGeneralCommunity(
      CommunityInfo communityInfo, String userId, long closedTime) {
    String communityName = communityInfo.getCommunityName();
    communityInfo.setCommunityName(
        String.format(CLOSED_GENERAL_COMMUNITY_NAME_FORMAT, communityName));
    communityInfo.setCommunityCategory(CommunityCategory.GENERAL.toString());
    communityInfo.setCommunityStatus(CommunityStatus.CLOSED.toString());
    communityInfo.setCommunityModifiedUserId(userId);
    communityInfo.setCommunityModifiedTime(closedTime);
  }

  public int reopenGeneralCommunity(int communityId, long communityModifiedTime) {
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    if (!checkUserCanReopen(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }

    if ((communityInfo.getCommunityModifiedTime() != 0
        && communityInfo.getCommunityModifiedTime() != communityModifiedTime)) {
      return HttpStatus.SC_CONFLICT;
    }

    if (!Objects.equals(CommunityCategory.GENERAL.toString(), communityInfo.getCommunityCategory())
        || !Objects.equals(CommunityStatus.CLOSED.toString(), communityInfo.getCommunityStatus())) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_GENERAL_REOPEN_HINT);
    }

    long reopenedTime = new Date().getTime();
    String userId = Utility.getUserIdFromSession();
    eventPublishService.publishCommunityReopeningEvent(communityId, userId, reopenedTime);
    setReopenGeneralCommunity(communityInfo, userId, reopenedTime);
    if (communityDao.update(communityInfo) > 0) {
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_NOT_FOUND;
    }
  }

  public boolean checkUserCanReopen(String userId, int communityId) {
    return privilegeService.checkUserPrivilege(
        userId, communityId, 0, PermissionObject.COMMUNITY.toString(), Operation.REOPEN.toString());
  }

  private void setReopenGeneralCommunity(
      CommunityInfo communityInfo, String userId, long closedTime) {
    String communityName = communityInfo.getCommunityName();
    communityInfo.setCommunityName(removeClosedMark(communityName));
    communityInfo.setCommunityCategory(CommunityCategory.GENERAL.toString());
    communityInfo.setCommunityStatus(CommunityStatus.OPEN.toString());
    communityInfo.setCommunityModifiedUserId(userId);
    communityInfo.setCommunityModifiedTime(closedTime);
  }

  private String removeClosedMark(String name) {
    Pattern pattern = Pattern.compile(REOPENED_GENERAL_COMMUNITY_NAME_REGEX);
    Matcher matcher = pattern.matcher(name);
    return matcher.replaceAll(REOPENED_GENERAL_COMMUNITY_NAME_REGEX_REPLACEMENT);
  }

  public CommunityResultList getCommunityList(CommunitySearchRequestEntity request) {
    CommunitySearchRequestEntity searchRequest = setSearchRequestInfo(request);
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    searchRequest.setDL(false);
    //判斷此登入者是否為DL專用帳號，如果是的話，把isDL設定為true，並且取得community白名單
    if (dlInfo.isDL == true)
    	searchRequest.setDL(dlInfo.isDL)
    				 .setAllowCommunityId(dlInfo.getAllowCommunityId());
    List<CommunityInfo> communityInfoList = communityDao.getCommunityByCategory(searchRequest);
    Map<String, List<Medal>> medals =
        medalService.getMedals(
            MedalIdType.COMMUNITY,
            communityInfoList
                .parallelStream()
                .map(CommunityInfo::getCommunityId)
                .map(String::valueOf)
                .collect(Collectors.toSet()));
    List<CommunityResultDetail> communityResultDetail =
        setCommunityResultDetail(communityInfoList, medals);
    CommunityResultList communityResultList = new CommunityResultList();
    communityResultList.setResult(communityResultDetail);
    communityResultList.setNumFound(getCommunityCategoryCount(searchRequest));
    return communityResultList;
  }

  private List<CommunityResultDetail> setCommunityResultDetail(
      List<CommunityInfo> communityInfoList, Map<String, List<Medal>> medalMap) {
    Map<String, IdName> userIdNameMap =
        userService
            .getUserByIds(
                communityInfoList
                    .parallelStream()
                    .map(CommunityInfo::getCommunityLastModifiedUserId)
                    .distinct()
                    .collect(Collectors.toList()))
            .stream()
            .collect(
                LinkedHashMap::new,
                (map, item) ->
                    map.put(item.getId(), new IdName().id(item.getId()).name(item.getName())),
                Map::putAll);
    Map<String, String> attachedCommunityNameMap = getAttachedCommunityNameMap(communityInfoList);
    return communityInfoList
        .stream()
        .map(
            item ->
                new CommunityResultDetail()
                    .type(SearchType.COMMUNITY)
                    .data(
                        new SearchCommunityData()
                            .id(item.getCommunityId())
                            .name(
                                attachedCommunityNameMap.getOrDefault(
                                    item.getCommunityGroupId(), item.getCommunityName()))
                            .category(CommunityCategory.fromValue(item.getCommunityCategory()))
                            .avatar(item.getCommunityImgAvatar())
                            .medalInfo(
                                getCommunityMedal(
                                    medalMap.get(String.valueOf(item.getCommunityId()))))
                            .lastModifiedTime(item.getCommunityLastModifiedTime())
                            .lastModifiedUser(
                                Optional.ofNullable(
                                        userIdNameMap.get(item.getCommunityLastModifiedUserId()))
                                    .orElseGet(IdName::new))
                            .memberCount(
                            	gatherAllCommunityMemberCount(item.getCommunityId(), null, null, -1)
                            	))
                    .read(Boolean.TRUE))
        .collect(Collectors.toList());
  }

  private Map<String, String> getAttachedCommunityNameMap(List<CommunityInfo> communityInfoList) {
    Map<String, String> groupIdOriginalNameMap =
        communityInfoList
            .parallelStream()
            .filter(
                item ->
                    CommunityCategory.DEPARTMENT.toString().equals(item.getCommunityCategory())
                        || CommunityCategory.PROJECT.toString().equals(item.getCommunityCategory()))
            .filter(item -> StringUtils.isNotBlank(item.getCommunityGroupId()))
            .collect(
                HashMap::new,
                (map, item) -> map.put(item.getCommunityGroupId(), item.getCommunityName()),
                Map::putAll);
    return getAttachedCommunityNames(groupIdOriginalNameMap, AcceptLanguage.get());
  }

  public List<IdNameDto> getPrivilegedAllCommunity() {
    return communityDao
        .getPrivilegedAllCommunity(Utility.getCurrentUserIdWithGroupId())
        .stream()
        .map(
            item ->
                new IdNameDto().id(Integer.valueOf(item.getId().toString())).name(item.getName()))
        .collect(Collectors.toList());
  }

  public int getCommunityIdByCommunityName(String communityName) {
    return communityDao.getCommunityIdByCommunityName(communityName);
  }

  public int getCommunityIdByForumId(int forumId) {
    return communityDao.getCommunityIdByForumId(forumId);
  }

  public boolean needChangeAttachedCommunityName(String lang) {
    return Locale.US.toLanguageTag().equalsIgnoreCase(lang);
  }

  public Map<String, String> getAttachedCommunityNames(
      Map<String, String> orgIdWithOriginalName, String lang) {
    Map<String, List<MyDmsGroupData>> pathMap =
        userGroupAdapter.getPathInfoByGidsWithLang(
            new ArrayList<>(orgIdWithOriginalName.keySet()), lang);
    return orgIdWithOriginalName
        .entrySet()
        .parallelStream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    getUpdatedAttachedCommunityName(
                        pathMap.get(entry.getKey()), entry.getValue())));
  }

  public List<CommunityMember> gatherAllCommunityMembers(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int role, int limit) {
    return collectMemberService.getCommunityMembers(
            communityId, toGetDmsMember, userIdList, role, limit);
  }

  public Integer gatherAllCommunityMemberCount(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int role) {
	    return collectMemberService.getCommunityMemberCount(
                communityId, toGetDmsMember, userIdList, role);
  }

  public CommunitySearchRequestEntity setSearchRequestInfo(CommunitySearchRequestEntity request) {
    request
        .setCheckRole(needCheckingRole(request.getScope()))
        .setRoleId(
            (SearchScope.MINEADMIN.equals(request.getScope())
                    ? Role.COMMUNITY_ADMIN
                    : Role.COMMUNITY_MEMBER)
                .getId())
        .setUserIdWithGid(getUserIdBySearchScope(request.getScope(), request.getUserId()))
        .setOffset(request.getOffset())
        .setLimit(request.getLimit());
    Optional.ofNullable(request.getSort())
        .filter(Objects::nonNull)
        .ifPresent(
            order ->
                request
                    .setSortField(getSortField(request.getSort().getProperty()).toString())
                    .setSortOrder(request.getSort().getDirection().toString()));
    return request;
  }

  private boolean needCheckingRole(SearchScope scope) {
    switch (scope) {
      case ALL:
        return false;
      case MINE:
        return true;
      default:
        return !userService.isSysAdmin();
    }
  }

  private String getUserIdBySearchScope(SearchScope scope, String userId) {
    return scope.equals(SearchScope.ALL) ? "" : getUserIdWithoutGroupId(userId);
  }
}
