package com.delta.dms.community.service;

import static com.delta.dms.community.utils.Constants.MINUTE_TO_SECOND_MULTIPLY;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.config.DrcSyncConfig;
import com.delta.dms.community.dao.entity.*;
import com.delta.dms.community.enums.DrcSyncType;
import com.delta.dms.community.swagger.model.*;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.ForumDao;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.exception.CommunityException;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.NotificationConstants;
import com.delta.dms.community.utils.Utility;

/**
 *
 *
 * <ul>
 *   <li>Create/update forum rule: assume Community Administrator : A B C and Community Member : A B
 *       C D E F
 *       <ul>
 *         <li>for public forum:
 *             <ul>
 *               <li>Forum Administrator : A B C D (added the D member， A B C are fixed)
 *               <li>Forum Member : A B C D E F (all are fixed)
 *             </ul>
 *         <li>for private forum:
 *             <ul>
 *               <li>Forum Administrator : A B C D (added the D member， A B C are fixed)
 *               <li>Forum Member : A B C D E F (A B C will be members，selected D E F members from
 *                   community members)
 *             </ul>
 *       </ul>
 *   <li>get forum members rules:
 *       <ul>
 *         <li>for public forum:
 *             <ul>
 *               <li>Forum Administrator : Community Administrator + Forum Administrator
 *               <li>Forum Member : Community Member
 *             </ul>
 *         <li>for private forum:
 *             <ul>
 *               <li>Forum Administrator : Community Administrator + Forum Administrator
 *               <li>Forum Member : Forum Member
 *             </ul>
 *       </ul>
 * </ul>
 *
 * @author KEN.YK.WANG
 */
@Service
@Transactional
public class ForumService {

  private ForumDao forumDao;
  private UserService userService;
  private CommunityService communityService;
  private PrivilegeService privilegeService;
  private EventPublishService eventPublishService;
  private YamlConfig yamlConfig;
  private GroupRecipientHandleService groupRecipientHandleService;
  private AuthService authService;
  private final TopicService topicService;
  private final UserGroupAdapter userGroupAdapter;
  private final DrcSyncConfig drcSyncConfig;

  private static final int MAXIMUM_OF_TOP_FORUM = 5;
  private static final int DEFAULT_FORUM_ORDER = 0;
  private static final int BATCH_SIZE = 1000;

  public ForumService(
          ForumDao forumDao,
          UserService userService,
          @Lazy TopicService topicService,
          CommunityService communityService,
          PrivilegeService privilegeService,
          EventPublishService eventPublishService,
          YamlConfig yamlConfig,
          GroupRecipientHandleService groupRecipientHandleService,
          AuthService authService,
          UserGroupAdapter userGroupAdapter,
          DrcSyncConfig drcSyncConfig) {
    this.forumDao = forumDao;
    this.userService = userService;
    this.topicService = topicService;
    this.communityService = communityService;
    this.privilegeService = privilegeService;
    this.eventPublishService = eventPublishService;
    this.yamlConfig = yamlConfig;
    this.groupRecipientHandleService = groupRecipientHandleService;
    this.authService = authService;
    this.userGroupAdapter = userGroupAdapter;
    this.drcSyncConfig = drcSyncConfig;
  }

  public ForumSearchResult searchForumListOfCommunity(
      int communityId,
      List<ForumType> forumTypeList,
      int offset,
      int limit,
      Order sort,
      Boolean withTopping) {
    List<ForumListDetail> topingForumListDetailList = new ArrayList<>();
    int page = (offset / limit);
    int mod = (offset % limit);
    if (Boolean.TRUE.equals(withTopping)) {
      topingForumListDetailList = searchTopingForumOfCommunityByType(communityId, forumTypeList);
    }
    limit -= topingForumListDetailList.size();
    offset = mod + page * limit;
    List<ForumListDetail> forumListDetailList =
        searchForumOfCommunityByType(communityId, forumTypeList, offset, limit, sort);
    int numFound = countForumOfCommunity(communityId, forumTypeList);
    return new ForumSearchResult()
        .result(forumListDetailList)
        .toppingResult(topingForumListDetailList)
        .numFound(numFound);
  }

  public List<ForumListDetail> searchTopingForumOfCommunityByType(
      int communityId, List<ForumType> forumType) {
    List<ForumInfo> forumInfoList =
        forumDao.getTopingForumOfCommunityByTypeWithSortAndLimit(
            communityId, forumType.stream().map(ForumType::toString).collect(Collectors.toList()));
    return getForumListDetails(forumInfoList);
  }

  public List<ForumListDetail> searchForumOfCommunityByType(
      int communityId, List<ForumType> forumType, int offset, int limit, Order sort) {
    List<ForumInfo> forumInfoList =
        searchForumInfoList(communityId, forumType, offset, limit, sort);
    return getForumListDetails(forumInfoList);
  }

  private List<ForumListDetail> getForumListDetails(List<ForumInfo> forumInfoList) {
    return forumInfoList
        .stream()
        .map(
            item ->
                new ForumListDetail()
                    .id(item.getForumId())
                    .name(item.getForumName())
                    .desc(item.getForumDesc())
                    .type(ForumType.fromValue(item.getForumType()))
                    .lastModifiedTime(item.getForumLastModifiedTime())
                    .lastModifiedUser(userService.getUserById(item.getForumLastModifiedUserId()))
                    .modifiedTime(item.getForumModifiedTime())
                    .access(checkCurrentUserAccessibilityOfForum(item))
                    .tag(getTagOfForum(item.getForumId()))
                    .admins(getAdminListOfForum(item.getForumId(), -1, -1))
                    .toppingOrder(item.getForumToppingOrder()))
        .collect(Collectors.toList());
  }

  public List<ForumInfo> searchForumInfoList(
      int communityId, List<ForumType> forumType, int offset, int limit, Order sort) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    String sortField =
        SortField.PRIVILEGE.toString().equals(sort.getProperty())
            ? SortField.PRIVILEGE.toString()
            : getSortField(sort.getProperty()).toString();
    return forumDao.getForumOfCommunityByTypeWithSortAndLimit(
        userService.isSysAdmin(),
        Utility.getCurrentUserIdWithGroupId(),
        communityId,
        forumType.stream().map(ForumType::toString).collect(Collectors.toList()),
        offset,
        limit,
        sortField,
        sort.getDirection().toString(),
        dlInfo.isDL,
        dlInfo.getAllowForumId());
  }

  private ForumFieldName getSortField(String sortField) {
    if (SortField.UPDATETIME.toString().equals(sortField)) {
      return ForumFieldName.FORUM_LAST_MODIFIED_TIME;
    } else if (SortField.TYPE.toString().equals(sortField)) {
      return ForumFieldName.FORUM_TYPE;
    } else if (SortField.NAME.toString().equals(sortField)) {
      return ForumFieldName.FORUM_NAME;
    }
    return ForumFieldName.FORUM_LAST_MODIFIED_TIME;
  }

  private Access checkCurrentUserAccessibilityOfForum(ForumInfo forumInfo) {
    if (ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))
        && !checkUserCanRead(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      if (checkApplicationExists(forumInfo.getForumId(), Utility.getUserIdFromSession())) {
        return Access.APPLICATION;
      } else {
        return Access.INACCESSIBLE;
      }
    }
    return Access.ACCESSIBLE;
  }

  private boolean checkUserCanRead(String userId, ForumInfo forumInfo) {
    return checkUserPermission(userId, forumInfo, Operation.READ);
  }

  private boolean checkUserPermission(String userId, ForumInfo forumInfo, Operation operation) {
    PermissionObject permissionObject =
        !ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))
            ? PermissionObject.PUBLICFORUM
            : PermissionObject.PRIVATEFORUM;
    return privilegeService.checkUserPrivilege(
        userId,
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        permissionObject.toString(),
        operation.toString());
  }

  private boolean checkUserCanEditMember(String userId, ForumInfo forumInfo) {
    return privilegeService.checkUserPrivilege(
        userId,
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        PermissionObject.FORUMMEMBER.toString(),
        Operation.REVIEW.toString());
  }

  public boolean checkUserIsMemberOfForum(String userId, ForumInfo forumInfo) {
    if (ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))) {
      return checkUserIsMemberOfPrivateForum(userId, forumInfo);
    } else {
      return checkUserIsMemberOfPublicForum(userId, forumInfo);
    }
  }

  private boolean checkUserIsMemberOfPublicForum(String userId, ForumInfo forumInfo) {
    List<User> memberInForum = getMemberOfForumWithFilters(false,
            forumInfo.getForumId(), -1, -1, EMPTY,
            Collections.singletonList(userId), null, EMPTY);
    return (!memberInForum.isEmpty());
  }

  private boolean checkUserIsMemberOfPrivateForum(String userId, ForumInfo forumInfo) {
    List<User> memberInForum = getMemberOfForumWithFilters(false,
            forumInfo.getForumId(), -1, -1, EMPTY,
            Collections.singletonList(userId), null, EMPTY);
    return (!memberInForum.isEmpty());
  }

  private boolean checkUserRoleOfForum(String userId, int forumId, List<Integer> roleIdList) {
    List<User> totalCount = forumDao.getMemberListWithSortAndLimit(true, forumId,
            true, roleIdList, EMPTY,
            Collections.singletonList(userId), null, -1, -1, EMPTY);
    Optional<User> optional = totalCount.stream().findFirst();
    long total = 0;
    if (optional.isPresent()) {
      total = optional.get().getTotalCount();
    }

    return total >= 1;
  }

  public MemberListResult getAdminListOfForumWithFilter(int forumId, String userId, int offset, int limit, SortField sortType) {
    List<String> userListId = (userId.isEmpty()) ? null : Collections.singletonList(userId);
    List<Integer> roleIdList = Arrays.asList(Role.FORUM_ADMIN.getId(), Role.COMMUNITY_ADMIN.getId());
    List<User> memberList = forumDao
            .getMemberListWithSortAndLimit(false, forumId, true, roleIdList,
                    EMPTY, userListId, null ,offset, limit, sortType.toString())
            .parallelStream()
            .map(user -> user.lock(true))
            .collect(Collectors.toList());
    List<String> userIdList = memberList
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());

    int numFound = extractUserCount(forumDao
            .getMemberListWithSortAndLimit(true, forumId, true, roleIdList,
                    EMPTY, userListId, null ,offset, limit, sortType.toString()));
    List<UserSession> userList = userService.getUserById(userIdList, new ArrayList<>());
    List<MemberInfoDetail> memberInfoList =
            getMemberInfoDetail(memberList, userList, true, false);
    return new MemberListResult().result(memberInfoList).numFound(numFound);
/*
    return forumDao
            .getMemberListWithSortAndLimit(false, forumId, true, roleIdList,
                    EMPTY, userListId, null ,offset, limit, sortType.toString())
            .parallelStream()
            .map(user -> user.lock(true))
            .collect(Collectors.toList());
 */
  }

  public List<User> getAdminListOfForum(int forumId) {
    return getAdminListOfForum(forumId, -1, -1)
        .parallelStream()
        .map(user -> user.lock(true))
        .collect(Collectors.toList());
  }

  public List<User> getAdminListOfForum(int forumId, int offset, int limit) {
    List<Integer> roleIdList = Arrays.asList(Role.FORUM_ADMIN.getId(), Role.COMMUNITY_ADMIN.getId());
    return forumDao
            .getMemberListWithSortAndLimit(false, forumId, true, roleIdList,
                    EMPTY, null, null ,offset, limit, EMPTY);
  }

  public int getAdminCountOfForum(int forumId) {
    List<Integer> roleIdList = Arrays.asList(Role.FORUM_ADMIN.getId(), Role.COMMUNITY_ADMIN.getId());
    Optional<User> optional = forumDao
            .getMemberListWithSortAndLimit(true, forumId, true, roleIdList,
                    EMPTY, null, null , -1, -1, EMPTY)
            .stream()
            .findFirst();
    int total = 0;
    if (optional.isPresent()) {
      total = optional.get().getTotalCount();
    }
    return total;
  }

  public List<String> getAdminRoleListOfForum(int forumId) {
    List<Integer> roleIdList = Arrays.asList(Role.COMMUNITY_ADMIN.getId(), Role.FORUM_ADMIN.getId());
    return forumDao.getForumRole(forumId, roleIdList)
            .stream()
            .map(RoleDetailEntity::getGroupId)
            .collect(Collectors.toList());
  }

  public List<String> getMemberRoleListOfForum(int forumId) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    List<Integer> roleIdList = new ArrayList<>(Arrays.asList(Role.COMMUNITY_ADMIN.getId(),
            Role.FORUM_ADMIN.getId(),
            Role.FORUM_MEMBER.getId()));
    if (ForumType.PUBLIC.equals(ForumType.fromValue(forumInfo.getForumType()))) {
      roleIdList.add(Role.COMMUNITY_MEMBER.getId());
    }
    return forumDao.getForumRole(forumId, roleIdList)
            .stream()
            .map(RoleDetailEntity::getGroupId)
            .collect(Collectors.toList());
  }

  public List<String> getRoleListOfMainGroupForum(int forumId, boolean isAdminRole) {
    List<Integer> roleIdList;
    if (isAdminRole){
      roleIdList = Arrays.asList(Role.COMMUNITY_ADMIN.getId(), Role.FORUM_ADMIN.getId());
    } else {
      // 否則，獲取論壇資訊以判斷其類型
      ForumInfo forumInfo = getForumInfoById(forumId);
      roleIdList = new ArrayList<>(Arrays.asList(Role.COMMUNITY_ADMIN.getId(),
              Role.FORUM_ADMIN.getId(),
              Role.FORUM_MEMBER.getId()));
      // 如果是公開論壇，則還應包含community成員角色
      if (ForumType.PUBLIC.equals(ForumType.fromValue(forumInfo.getForumType()))) {
        roleIdList.add(Role.COMMUNITY_MEMBER.getId());
      }
    }
    // 使用roleIdList進行查詢，獲取角色ID對應的群組ID列表
    return forumDao.getMainGroupRoleListOfForum(forumId, roleIdList)
            .stream()
            .map(RoleDetailEntity::getGroupId)
            .collect(Collectors.toList());
  }

  public int countForumOfCommunity(int communityId, List<ForumType> forumTypeList) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    return forumDao.countForumOfCommunity(
        communityId,
        forumTypeList.parallelStream().map(ForumType::toString).collect(Collectors.toList()),dlInfo.isDL, dlInfo.getAllowForumId());
  }

  public ForumInfo getForumInfoById(int forumId) {
    ForumInfo forumInfo = forumDao.getForumById(forumId, AcceptLanguage.getLanguageForDb());
    if (forumInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_NOT_EXIST);
    }
    return forumInfo;
  }

  public String addMemberApplicationOfPrivateForum(
      int forumId, ApplicationDetail applicationDetail) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    ForumType forumType = ForumType.fromValue(forumInfo.getForumType());
    if (ForumType.PRIVATE.equals(forumType)) {
      return addMemberApplication(forumInfo, applicationDetail);
    } else {
      return I18nConstants.MSG_DONT_NEED_APPLICATION;
    }
  }

  private String addMemberApplication(ForumInfo forumInfo, ApplicationDetail applicationDetail) {
    if (applicationDetail.getSubject().isEmpty()) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    boolean isForumMember =
        checkUserIsMemberOfPrivateForum(Utility.getUserIdFromSession(), forumInfo);
    if (isForumMember) {
      return I18nConstants.MSG_ALREADY_FORUM_MEMBER;
    }
    return addMemberApplicationBasedOnCommunityType(forumInfo, applicationDetail);
  }

  private String addMemberApplicationBasedOnCommunityType(
      ForumInfo forumInfo, ApplicationDetail applicationDetail) {
    CommunityInfo communityInfo = communityService.getCommunityInfoById(forumInfo.getCommunityId());
    CommunityType communityType = CommunityType.fromValue(communityInfo.getCommunityType());
    if (CommunityType.PUBLIC.equals(communityType)
        || CommunityType.ACTIVITY.equals(communityType)) {
      return addMemberApplicationWithCommunityInfo(
          forumInfo, communityInfo, applicationDetail.getDesc());
    } else if (CommunityType.PRIVATE.equals(communityType)) {
      return addMemberApplicationOfPrivateCommunity(
          forumInfo, communityInfo, applicationDetail.getDesc());
    } else {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  private String addMemberApplicationOfPrivateCommunity(
      ForumInfo forumInfo, CommunityInfo communityInfo, String desc) {
    boolean isCommunityMember =
        checkUserIsMemberOfCommunity(
            Utility.getUserIdFromSession(),
            forumInfo.getCommunityId(),
            communityInfo.getCommunityGroupId());
    if (isCommunityMember) {
      return addMemberApplicationWithCommunityInfo(forumInfo, communityInfo, desc);
    } else {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_PRIVATE_COMMUNITY_MEMBER);
    }
  }

  private boolean checkUserIsMemberOfCommunity(String userId, int communityId, String groupId) {
    return communityService.checkUserIsMemberOfCommunity(userId, communityId, groupId);
  }

  private String addMemberApplicationWithCommunityInfo(
      ForumInfo forumInfo, CommunityInfo communityInfo, String desc) {
    if (!checkApplicationExists(forumInfo.getForumId(), Utility.getUserIdFromSession())) {
      return addMemberApplicationAndSendMail(forumInfo, communityInfo, desc);
    }
    return I18nConstants.MSG_JOIN_FORUM_APPLICATION_SENT_FAILED;
  }

  private String addMemberApplicationAndSendMail(
      ForumInfo forumInfo, CommunityInfo communityInfo, String desc) {
    long now = new Date().getTime();
    int row =
        forumDao.addUserIntoForumJoinReview(
            forumInfo.getForumId(), Utility.getUserIdFromSession(), desc, now);
    if (row != 0) {
      EmailWithChineseAndEnglishContext context =
          getForumJoinApplicationContext(forumInfo, communityInfo, desc);
      context.setPriority(EmailConstants.HIGH_PRIORITY_MAIL);
      eventPublishService.publishEmailSendingEvent(context);
      eventPublishService.publishNotificationSendingEvent(
          getForumJoinApplicationNotification(forumInfo, desc, now));
      return I18nConstants.MSG_JOIN_FORUM_APPLICATION_SENT;
    }
    return I18nConstants.MSG_JOIN_FORUM_APPLICATION_SENT_FAILED;
  }

  private boolean checkApplicationExists(int forumId, String applicantId) {
    return forumDao.checkApplicationExists(forumId, applicantId) == 1;
  }

  public List<ApplicantDetail> getApplicantList(int forumId) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }
    Map<String, String> applicantIdAndApplicationDescMap =
        forumDao
            .getApplicantList(forumId)
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

  public boolean checkUserIsAdmin(String userId, ForumInfo forumInfo) {
    List<Integer> roleIdList = Arrays.asList(Role.FORUM_ADMIN.getId(), Role.COMMUNITY_ADMIN.getId());
    return checkUserRoleOfForum(userId, forumInfo.getForumId(), roleIdList);
  }

  private boolean checkUserIsAdminOfForum(String userId, int forumId) {
    return checkUserRoleOfForum(userId, forumId, Collections.singletonList(Role.FORUM_ADMIN.getId()));
  }

  private boolean checkUserIsAdminOfCommunity(String userId, int communityId) {
    List<User> adminListOfCommunity =
        communityService.getAdminListOfCommunity(communityId, null,
                Collections.singletonList(userId), -1);
    return adminListOfCommunity.stream().map(User::getId).anyMatch(item -> item.equals(userId));
  }

  private EmailWithChineseAndEnglishContext getForumJoinApplicationContext(
      ForumInfo forumInfo, CommunityInfo communityInfo, String content) {
    List<String> adminIdList =
        getAdminListOfForum(forumInfo.getForumId(), -1, -1)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.FORUM_HOME_URI_FORMAT, host, language, forumInfo.getForumId());
    String mobileLink =
        String.format(
            EmailConstants.FORUM_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            forumInfo.getForumId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.FORUMJOINAPPLICATION)
        .sender(userName)
        .desc(String.format(EmailConstants.FORUM_JOIN_APPLICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.FORUM_JOIN_APPLICATION_CHINESE_FORMAT, userName))
        .subject(EmailConstants.JOIN_FORUM_APPLICATION_SUBJECT_FORMAT)
        .to(userService.getEmailByUserId(adminIdList))
        .content(content)
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName()),
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName())));
  }

  public String reviewMemberApplicationOfForum(
      int forumId, String applicantId, ReviewAction action) {
    if (!checkApplicationExists(forumId, applicantId)) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_APPLICATION_NOT_EXIST);
    }
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }
    long now = new Date().getTime();
    int row =
        forumDao.reviewTheMemberApplicationOfForum(
            forumId, applicantId, Utility.getUserIdFromSession(), now, String.valueOf(action));
    if (row == 1) {
      CommunityInfo communityInfo =
          communityService.getCommunityInfoById(forumInfo.getCommunityId());
      if (ReviewAction.APPROVED.equals(action) || ReviewAction.AUTO_APPROVED.equals(action)) {
        if (reviewMemberApplicationBasedOnCommunityType(forumInfo, communityInfo, applicantId)) {
          EmailWithChineseAndEnglishContext context =
              getApproveJoinApplicationContext(forumInfo, communityInfo, applicantId);
          eventPublishService.publishEmailSendingEvent(context);
          eventPublishService.publishNotificationSendingEvent(
              getApproveJoinApplicationNotification(forumInfo, applicantId, now));
          eventPublishService.publishForumChangingNoDdfUpdateEvent(
              forumInfo.getCommunityId(),
              forumInfo.getForumId(),
              Utility.getUserIdFromSession(),
              System.currentTimeMillis());
          return I18nConstants.MSG_FORUM_APPLICATION_APPROVED;
        } else {
          return I18nConstants.MSG_FORUM_APPLICATION_REVIEW_FAILED;
        }
      } else if (ReviewAction.REJECTED.equals(action)) {
        EmailWithChineseAndEnglishContext context =
            getRejectJoinApplicationContext(forumInfo, communityInfo, applicantId);
        eventPublishService.publishEmailSendingEvent(context);
        eventPublishService.publishNotificationSendingEvent(
            getRejectJoinApplicationNotification(forumInfo, applicantId, now));
        return I18nConstants.MSG_FORUM_APPLICATION_REJECTED;
      } else {
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
      }
    } else {
      return I18nConstants.MSG_FORUM_APPLICATION_REVIEW_FAILED;
    }
  }

  private boolean reviewMemberApplicationBasedOnCommunityType(
      ForumInfo forumInfo, CommunityInfo communityInfo, String applicantId) {
    CommunityType communityType = CommunityType.fromValue(communityInfo.getCommunityType());
    if (CommunityType.PUBLIC.equals(communityType)
        || CommunityType.ACTIVITY.equals(communityType)) {
      return addMemberIntoForumAndCommunity(
          Arrays.asList(applicantId), forumInfo.getForumId(), forumInfo.getCommunityId());
    } else if (CommunityType.PRIVATE.equals(communityType)) {
      boolean isCommunityMember =
          checkUserIsMemberOfCommunity(
              applicantId, forumInfo.getCommunityId(), communityInfo.getCommunityGroupId());
      if (isCommunityMember) {
        return addMemberIntoForum(Arrays.asList(applicantId), forumInfo.getForumId());
      } else {
        throw new UnauthorizedException(I18nConstants.MSG_NOT_PRIVATE_COMMUNITY_MEMBER);
      }
    } else {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  private boolean addMemberIntoForumAndCommunity(
      List<String> userIdList, int forumId, int communityId) {
    boolean addCommunityMember = communityService.addMemberIntoCommunity(userIdList, communityId);
    boolean addForumMember = addMemberIntoForum(userIdList, forumId);
    return addCommunityMember && addForumMember;
  }

  public void addMemberIntoForum(int forumId, List<String> userIdList) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (!ForumType.PRIVATE.toString().equals(forumInfo.getForumType())) {
      throw new IllegalArgumentException(
          I18nConstants.MSG_CANNOT_ADD_FORUM_MEMBER_NOT_PRIVATE_FORUM);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_ADD_FORUM_MEMBER_NOT_MANAGER);
    }
    CommunityInfo communityInfo = communityService.getCommunityInfoById(forumInfo.getCommunityId());
    if (CommunityType.PRIVATE.toString().equals(communityInfo.getCommunityType())) {
      List<User> communityMemberList =
          communityService.getAllMemberOfCommunityById(
              communityInfo.getCommunityId(),null, userIdList, -1, -1);
      userIdList =
          userIdList
              .stream()
              .filter(
                  item -> {
                    if (communityMemberList
                        .stream()
                        .noneMatch(communityMember -> communityMember.getId().equals(item))) {
                      throw new IllegalArgumentException(
                          I18nConstants.MSG_CANNOT_ADD_FORUM_MEMBER_NOT_COMMUNITY_MEMBER);
                    }
                    return true;
                  })
              .collect(Collectors.toList());
    }
    if (!addMemberIntoForum(userIdList, forumId)) {
      throw new CommunityException(I18nConstants.MSG_ADD_FORUM_MEMBER_FAILED);
    } else {
      eventPublishService.publishNotificationSendingEvent(
          getJoinForumNotification(forumInfo, userIdList));
    }
  }

  public boolean addMemberIntoForum(List<String> userIdList, int forumId) {
    if (userIdList.isEmpty()) {
      return true;
    }
    return appendForumMembers(userIdList, forumId, Role.FORUM_MEMBER);
  }

  private boolean appendForumMembers(List<String> memberIdList, int forumId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = getCurrentGid(forumId, role);
    return userGroupAdapter.appendUserGroupMembers(
            currentGid,
            updatedMemberIdList);
  }

  private boolean deleteForumMembers(List<String> memberIdList, int forumId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = getCurrentGid(forumId, role);
    return userGroupAdapter.removeUserGroupMembers(
            currentGid,
            updatedMemberIdList);
  }

  private String getCurrentGid(int forumId, Role role) {
    return forumDao.getForumRole(forumId, Collections.singletonList(role.getId()))
            .stream()
            .map(RoleDetailEntity::getGroupId)
            .findFirst()
            .orElse("");
  }

  private EmailWithChineseAndEnglishContext getApproveJoinApplicationContext(
      ForumInfo forumInfo, CommunityInfo communityInfo, String applicantId) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.FORUM_HOME_URI_FORMAT, host, language, forumInfo.getForumId());
    String mobileLink =
        String.format(
            EmailConstants.FORUM_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            forumInfo.getForumId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.FORUMJOINAPPROVAL)
        .sender(forumInfo.getForumName())
        .desc(EmailConstants.APPROVE_JOIN_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.APPROVE_JOIN_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.APPROVE_JOIN_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName()),
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName())));
  }

  private EmailWithChineseAndEnglishContext getRejectJoinApplicationContext(
      ForumInfo forumInfo, CommunityInfo communityInfo, String applicantId) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.FORUM_HOME_URI_FORMAT, host, language, forumInfo.getForumId());
    String mobileLink =
        String.format(
            EmailConstants.FORUM_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            forumInfo.getForumId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.FORUMJOINREJECTION)
        .sender(forumInfo.getForumName())
        .desc(EmailConstants.REJECT_JOIN_APPLICATION_CHINESE_FORMAT)
        .englishDesc(EmailConstants.REJECT_JOIN_APPLICATION_ENGLISH_FORMAT)
        .subject(EmailConstants.REJECT_JOIN_APPLICATION_SUBJECT)
        .to(userService.getEmailByUserId(Arrays.asList(applicantId)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName()),
                String.format(
                    EmailConstants.TITLE_FORMAT_COMMUNITY, communityInfo.getCommunityName())));
  }

  public int sendNotification(int forumId, NotificationDetail notificationDetail) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserIsAdmin(Utility.getUserIdFromSession(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }
    EmailWithChineseAndEnglishContext context =
        getNotificationContext(notificationDetail, forumInfo);
    eventPublishService.publishEmailSendingEvent(context);
    eventPublishService.publishNotificationSendingEvent(
        getNotification(notificationDetail, forumInfo));
    return HttpStatus.SC_OK;
  }

  private EmailWithChineseAndEnglishContext getNotificationContext(
      NotificationDetail notificationDetail, ForumInfo forumInfo) {
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.FORUM_HOME_URI_FORMAT, host, language, forumInfo.getForumId());
    String mobileLink =
        String.format(
            EmailConstants.FORUM_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            forumInfo.getForumId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.FORUMNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationDetail.getType())) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .forumInfo(forumInfo)
        .sender(forumInfo.getForumName())
        .desc(String.format(EmailConstants.FORUM_NOTIFICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.FORUM_NOTIFICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + notificationDetail.getSubject())
        .content(
            StringUtils.replace(
                notificationDetail.getContent(), Constants.LINE_BREAKS, Constants.HTML_LINE_BREAKS))
        .to(
            userService.getEmailByUserId(
                getRecipientList(forumInfo.getForumId(), notificationDetail)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                notificationDetail.getSubject(),
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName())));
  }

  private List<String> getRecipientList(int forumId, NotificationDetail notificationDetail) {
    List<String> userIdList;
    if (NotificationType.ALL.equals(notificationDetail.getType())) {
      userIdList = Arrays.asList("");
      //userIdList = getMemberOfForum(forumId, -1, -1).stream().map(User::getId).collect(Collectors.toList());
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

  public List<User> getMemberOfForum(int forumId, int offset, int limit) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    List<Integer> roleIdList = new ArrayList<Integer>(Arrays.asList(Role.COMMUNITY_ADMIN.getId(),
            Role.FORUM_ADMIN.getId(), Role.FORUM_MEMBER.getId()));
    if (ForumType.PUBLIC.toString().equals(forumInfo.getForumType())) {
      roleIdList.add(Role.COMMUNITY_MEMBER.getId());
    }
    return forumDao
            .getMemberListWithSortAndLimit(false, forumId, true, roleIdList,
                    EMPTY, null, null , offset, limit, EMPTY);
  }

  public List<User> getMemberOfForumWithFilters(boolean toGetCount, int forumId, int offset, int limit,
      String filterName, List<String> userIdList, List<String> excludeUserIdList, String sortField) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    List<Integer> roleIdList = new ArrayList<Integer>(Arrays.asList(Role.COMMUNITY_ADMIN.getId(),
            Role.FORUM_ADMIN.getId(), Role.FORUM_MEMBER.getId()));
    if (ForumType.PUBLIC.toString().equals(forumInfo.getForumType())) {
      roleIdList.add(Role.COMMUNITY_MEMBER.getId());
    }
    return forumDao
            .getMemberListWithSortAndLimit(toGetCount, forumId, true, roleIdList,
                    filterName, userIdList, excludeUserIdList , offset, limit, sortField);
  }

  private int extractUserCount(List<User> result) {
    Optional<User> optional = result
            .stream()
            .findFirst();
    int total = 0;
    if (optional.isPresent()) {
      total = optional.get().getTotalCount();
    }
    return total;
  }

  public int getMemberCountOfForum(int forumId) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    List<Integer> roleIdList = new ArrayList<Integer>(Arrays.asList(Role.COMMUNITY_ADMIN.getId(),
            Role.FORUM_ADMIN.getId(), Role.FORUM_MEMBER.getId()));
    if (ForumType.PUBLIC.toString().equals(forumInfo.getForumType())) {
      roleIdList.add(Role.COMMUNITY_MEMBER.getId());
    }

    Optional<User> optional = forumDao
            .getMemberListWithSortAndLimit(true, forumId, true, roleIdList,
                    EMPTY, null, null , -1, -1, EMPTY)
            .stream()
            .findFirst();
    int total = 0;
    if (optional.isPresent()) {
      total = optional.get().getTotalCount();
    }
    return total;
  }

  private List<User> getUserNotInForumOfCommunity(int forumId) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    List<User> forumMemberList = getMemberOfForum(forumId, -1, -1);
    if (ForumType.PRIVATE.toString().equals(forumInfo.getForumType())) {
      String groupId =
          communityService.getCommunityInfoById(forumInfo.getCommunityId()).getCommunityGroupId();
      List<User> communityMemberList =
          communityService.getAllMemberOfCommunityById(forumInfo.getCommunityId(), null, null, -1, -1);
      return communityMemberList
          .stream()
          .filter(
              item ->
                  forumMemberList
                      .stream()
                      .noneMatch(forumMember -> forumMember.getId().equals(item.getId())))
          .distinct()
          .sorted(Comparator.comparing(User::getName))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  public String deleteMemberFromForum(String memberId, int forumId) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (checkUserIsAdminOfCommunity(memberId, forumInfo.getCommunityId())) {
      throw new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_ADMIN_CANNOT_LEAVE_FORUM);
    }
    // 依附型公開討論版 無法退出
    if (ForumType.PUBLIC.toString().equals(forumInfo.getForumType())
            && communityService.isFromDmsGroupMember(memberId, forumInfo.getCommunityId())) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_REMOVE_GROUP_MEMBER);
    }
    if (!ForumType.PRIVATE.toString().equals(forumInfo.getForumType())) {
      throw new IllegalArgumentException(
          I18nConstants.MSG_CANNOT_DELETE_FORUM_MEMBER_NOT_PRIVATE_FORUM);
    }
    if (checkMemberIdIsCurrentUserId(memberId)) {
      return leaveForum(forumInfo, memberId);
    } else {
      throw new IllegalArgumentException(I18nConstants.MSG_LEFT_FORUM_FAILED);
    }
  }

  private boolean checkMemberIdIsCurrentUserId(String memberId) {
    return Utility.getUserIdFromSession().equals(memberId);
  }

  private String leaveForum(ForumInfo forumInfo, String memberId) {
    if (deleteUserFromForum(forumInfo, memberId)) {
      return I18nConstants.MSG_LEFT_FORUM;
    } else {
      throw new CommunityException(I18nConstants.MSG_LEFT_FORUM_FAILED);
    }
  }

  public void deleteUserFromForum(int forumId, List<String> deletingMembers) {
    //deletingMembers.stream().forEach(item -> forumDao.deleteUserFromForum(forumId, item));
  }

  private boolean deleteUserFromForum(ForumInfo forumInfo, String userId) {
    boolean isDeleted = deleteForumMembers(Collections.singletonList(userId),
              forumInfo.getForumId(), Role.FORUM_MEMBER)
            && deleteForumMembers(Collections.singletonList(userId),
              forumInfo.getForumId(), Role.FORUM_ADMIN);
    if (isDeleted) {
      eventPublishService.publishForumChangingNoDdfUpdateEvent(
          forumInfo.getCommunityId(),
          forumInfo.getForumId(),
          Utility.getUserIdFromSession(),
          System.currentTimeMillis());
    }
    return isDeleted;
  }

  private boolean checkDuplicateForumName(
      Integer communityId, String originalName, String forumName) {
    Integer checkValue = 0;

    checkValue = forumDao.checkDuplicateForumName(communityId, originalName, forumName);
    return checkValue != 0;
  }

  private boolean addTagIntoForumTag(Integer forumId, List<String> forumTags) {
    return forumDao.addTagIntoForumTag(forumId, forumTags) != 0;
  }

  private boolean addAdminAndMemberIntoForum(
      List<String> admins, List<String> members, int forumId) {
    return addAdminIntoForum(admins, forumId) && addMemberIntoForum(members, forumId);
  }

  private boolean addAdminIntoForum(List<String> userIdList, int forumId) {
    if (userIdList.isEmpty()) {
      return true;
    }
    return forumDao.addRoleIntoForum(userIdList, forumId, Role.FORUM_ADMIN.getId()) != 0;
  }

  public ResponseData createForum(ForumData forumData) {
    ResponseData code = new ResponseData().statusCode(HttpStatus.SC_NOT_FOUND);
    final long milliseconds = System.currentTimeMillis();
    CommunityInfo communityInfo = communityService.getCommunityInfoById(forumData.getCommunityId());
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (forumData.getName().trim().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_CREATE_HINT);
    }

    if (checkDuplicateForumName(
        forumData.getCommunityId(), StringUtils.EMPTY, forumData.getName().trim())) {
      return new ResponseData().statusCode(HttpStatus.SC_ACCEPTED);
    }

    if (Objects.equals(forumData.getType(), ForumType.PRIVATE)
        && forumData.getMembers().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_MEMBERS_INPUT);
    }

    if (checkIsCommunityMember(
        forumData.getAdmins(),
        communityInfo.getCommunityId(),
        communityInfo.getCommunityGroupId())) {
      throw new IllegalArgumentException(
          I18nConstants.MSG_CANNOT_ADD_FORUM_MEMBER_NOT_COMMUNITY_MEMBER);
    }

    ForumInfo forumInfo = setForumInfo(forumData, milliseconds);
    if ((forumDao.add(forumInfo) == 1)) {
      if ((forumData.getTag() != null)
          && !forumData.getTag().isEmpty()
          && !addTagIntoForumTag(forumInfo.getForumId(), forumData.getTag())) {
        throw new IllegalArgumentException(I18nConstants.MSG_FORUM_TAG_INPUT);
      }

      // send notification to members
      if(createUserGroupsForForum(forumData, communityInfo, forumInfo)) {
        if (Objects.equals(forumData.getType(), ForumType.SYSTEM)) {
          code.id(forumInfo.getForumId()).statusCode(HttpStatus.SC_CREATED);
        } else if (isPublicForum(forumData.getType())) {
          //List<String> members = getMemberList(forumData.getCommunityId(), forumData.getAdmins());
          eventPublishService.publishNotificationSendingEvent(
                  getJoinForumNotification(forumInfo, Collections.singletonList(EmailMemberType.ALLPUBLICFORUMMEMBER.toString())));
          code.id(forumInfo.getForumId()).statusCode(HttpStatus.SC_CREATED);
        } else if (Objects.equals(forumData.getType(), ForumType.PRIVATE)) {
          //List<String> members = forumDao.getMemberListOfForumWithCreateUserId(forumInfo.getForumId());
          eventPublishService.publishNotificationSendingEvent(
                  getJoinForumNotification(forumInfo, Collections.singletonList(EmailMemberType.ALLPRIVATEFORUMMEMBER.toString())));
          code.id(forumInfo.getForumId()).statusCode(HttpStatus.SC_CREATED);
        }
      }
      eventPublishService.publishForumChangingEvent(
          forumInfo.getCommunityId(),
          forumInfo.getForumId(),
          forumInfo.getForumCreateUserId(),
          milliseconds);
      String userId = setUserId(forumData.getType());
      publishActivityLogEventFromForum(
          userId,
          forumInfo,
          Operation.CREATE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);
    }
    return code;
  }

  private Map<String, List<String>> prepareMemberList(ForumType forumType, List<String> inputAdmins, List<String> inputMembers) {
    List<String> adminList = null;
    List<String> memberList = null;
    if (Objects.equals(forumType, ForumType.SYSTEM) || isPublicForum(forumType)) {
      adminList = inputAdmins;
    }
    else {
      adminList = inputAdmins;
      memberList = Stream.concat(inputAdmins.stream(), inputMembers.stream())
              .distinct()
              .collect(Collectors.toList());
    }

    Map<String, List<String>> result = new HashedMap<>();
    result.put(Identity.ADMIN.name(), adminList);
    result.put(Identity.MEMBER.name(), memberList);
    return result;
  }

  private boolean createUserGroupsForForum(ForumData forumData, CommunityInfo communityInfo, ForumInfo forumInfo) {
    Map<String, List<String>> preparedList = prepareMemberList(forumData.getType(), forumData.getAdmins(), forumData.getMembers());
    List<String> adminList = preparedList.get(Identity.ADMIN.name());
    List<String> memberList = preparedList.get(Identity.MEMBER.name());

    String adminGroupId = getForumUserGroupId(Role.FORUM_ADMIN, forumInfo.getForumId(),
            communityInfo.getCommunityId(), adminList);
    String memberGroupId = getForumUserGroupId(Role.FORUM_MEMBER, forumInfo.getForumId(),
            communityInfo.getCommunityId(), memberList);
    int adminRow = forumDao.addRoleIntoForum(Collections.singletonList(adminGroupId), forumInfo.getForumId(), Role.FORUM_ADMIN.getId());
    int memberRow = forumDao.addRoleIntoForum(Collections.singletonList(memberGroupId), forumInfo.getForumId(), Role.FORUM_MEMBER.getId());

    return (adminRow > 0 && memberRow > 0);
  }

  private String getForumUserGroupId(Role forumRole, int forumId, int communityId, List<String> appliedMemberList) {
    //String parentMemberGid = communityService.getMemberUserAndGroupOfCommunity(communityId).stream().findFirst().orElse("");
    String parentMemberGid = yamlConfig.getAppGroupRootId();
    List<String> memberIdList = Optional.ofNullable(appliedMemberList).orElse(new ArrayList<>());
    return userGroupAdapter.createUserGroup(forumRole, forumId, parentMemberGid, memberIdList);
  }

  private boolean checkIsCommunityMember(
      List<String> members, int communityId, String groupId) {
    if (members.isEmpty()) {
      return false;
    }

    List<String> adminListOfCommunity =
        communityService
            .getAllMemberOfCommunityById(communityId, null, members, Role.COMMUNITY_MEMBER.getId(), -1)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());

    ArrayList<String> membersClone = new ArrayList<>(members);
    ArrayList<String> adminListOfCommunityClone = new ArrayList<>(adminListOfCommunity);
    membersClone.removeAll(adminListOfCommunityClone);
    return !membersClone.isEmpty();
  }

  private String setUserId(ForumType type) {
    String userId = null;
    if (Objects.equals(type, ForumType.SYSTEM)) {
      userId = ForumType.SYSTEM.toString();
    } else if (isPublicForum(type) || Objects.equals(type, ForumType.PRIVATE)) {
      userId = Utility.getUserIdFromSession();
    }
    return userId;
  }

  private ForumInfo setForumInfo(ForumData forumData, long milliseconds) {
    ForumInfo forumInfo = new ForumInfo();
    forumInfo.setCommunityId(forumData.getCommunityId());
    forumInfo.setForumType(forumData.getType().toString());
    forumInfo.setForumName(forumData.getName().trim());
    forumInfo.setForumStatus(forumData.getStatus().toString());
    forumInfo.setForumCreateTime(milliseconds);
    forumInfo.setForumModifiedTime(milliseconds);
    forumInfo.setForumLastModifiedTime(milliseconds);
    if (Objects.equals(forumData.getType(), ForumType.SYSTEM)) {
      forumInfo.setForumCreateUserId(ForumType.SYSTEM.toString());
      forumInfo.setForumLastModifiedUserId(ForumType.SYSTEM.toString());
    } else if (isPublicForum(forumData.getType())
        || Objects.equals(forumData.getType(), ForumType.PRIVATE)) {
      String userId =
          Utility.getUserIdFromSession() == null
              ? ForumType.SYSTEM.toString()
              : Utility.getUserIdFromSession();
      forumInfo.setForumCreateUserId(userId);
      forumInfo.setForumModifiedUserId(userId);
      forumInfo.setForumLastModifiedUserId(userId);
    }
    return forumInfo;
  }

  public List<String> getAllPrivateForumMemberList(int forumId, String createdByUserId) {
    List<Integer> roleIdList = Arrays.asList(Role.FORUM_MEMBER.getId(), Role.FORUM_ADMIN.getId());
    List<String> excludeUserList = Arrays.asList(createdByUserId);
    List<String> userIdList = forumDao
                    .getMemberListWithSortAndLimit(false, forumId, true,
                            roleIdList, EMPTY, null, excludeUserList,-1, -1, EMPTY)
                    .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    return userIdList;
  }

  public List<String> getAllPublicForumMemberList(int forumId, String createdByUserId) {
    List<Integer> roleIdList = Arrays.asList(Role.COMMUNITY_MEMBER.getId(),
            Role.COMMUNITY_ADMIN.getId(),
            Role.FORUM_ADMIN.getId());
    List<String> excludeUserList = Arrays.asList(createdByUserId);
    List<String> userIdList = forumDao
            .getMemberListWithSortAndLimit(false, forumId, true,
                    roleIdList, EMPTY, null, excludeUserList,-1, -1, EMPTY)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    return userIdList;
  }

  private List<String> getMemberList(int communityId, List<String> forumAdmin) {
    String groupId = communityService.getCommunityInfoById(communityId).getCommunityGroupId();
    List<String> communityMemberList =
        communityService
            .getAllMemberOfCommunityById(communityId, null, null, -1, -1)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    List<String> users =
        Stream.concat(communityMemberList.stream(), forumAdmin.stream())
            .distinct()
            .collect(Collectors.toList());
    users.remove(Utility.getUserIdFromSession());
    return users;
  }

  private List<String> getMemberByRole(Integer forumId, Integer roleId) {
    return forumDao
            .getMemberListWithSortAndLimit(false, forumId, true,
                    Collections.singletonList(roleId), EMPTY, null, null,-1, -1, EMPTY)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
  }

  private EmailWithChineseAndEnglishContext getReomveForumContext(
      ForumInfo forumInfo, List<String> members) {
    String host = yamlConfig.getHost();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.FORUM_HOME_URI_FORMAT, host, language, forumInfo.getForumId());
    String mobileLink =
        String.format(
            EmailConstants.FORUM_HOME_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            forumInfo.getForumId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.REMOVEFROMFORUM)
        .sender(Utility.getUserFromSession().getCommonName())
        .desc(
            String.format(
                EmailConstants.FORUM_REMOVE_FORUM_CHINESE_FORMAT, forumInfo.getForumName()))
        .englishDesc(
            String.format(
                EmailConstants.FORUM_REMOVE_FORUM_ENGLISH_FORMAT, forumInfo.getForumName()))
        .subject(EmailConstants.JOIN_REMOVE_FORUM_SUBJECT_FORMAT)
        .to(userService.getEmailByUserId(members))
        .link(link)
        .mobileLink(mobileLink);
  }

  private void updateForumNotify(
      ForumInfo forumInfo, List<String> originalMembers, List<String> newMembers) {
    ArrayList<String> originalMembersClone = new ArrayList<>(originalMembers);
    ArrayList<String> newMembersClone = new ArrayList<>(newMembers);

    newMembersClone.removeAll(originalMembers);
    if (!newMembersClone.isEmpty()) {
      eventPublishService.publishNotificationSendingEvent(
          getJoinForumNotification(forumInfo, newMembersClone));
    }

    originalMembersClone.removeAll(newMembers);
    if (!originalMembersClone.isEmpty()) {
      EmailWithChineseAndEnglishContext deleteContext =
          getReomveForumContext(forumInfo, originalMembersClone);
      eventPublishService.publishEmailSendingEvent(deleteContext);
      eventPublishService.publishNotificationSendingEvent(
          getRemoveForumNotification(forumInfo, originalMembersClone));
    }
  }

  public Integer updateForum(int forumId, UpdatedForumData forumData) {
    int row = HttpStatus.SC_NOT_FOUND;
    if (forumData.getName().trim().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_CREATE_HINT);
    }

    if (Objects.equals(forumData.getType(), ForumType.PRIVATE)
        && forumData.getMembers().isEmpty()) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_MEMBERS_INPUT);
    }
    ForumInfo forumInfo = getForumInfoById(forumId);

    // get originalType of the forum
    ForumType originalType = ForumType.fromValue(forumInfo.getForumType());

    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserCanUpdate(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }

    if (checkDuplicateForumName(
            forumData.getCommunityId(), forumInfo.getForumName(), forumData.getName().trim())
        && !Objects.equals(forumInfo.getForumName(), forumData.getName())) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_NAME_REPEAT);
    }

    if (forumInfo.getForumModifiedTime() != 0
        && forumInfo.getForumModifiedTime() != forumData.getForumModifiedTime()) {
      return HttpStatus.SC_CONFLICT;
    }

    CommunityInfo communityInfo = communityService.getCommunityInfoById(forumData.getCommunityId());
    // DMSCOMMU-1163 檢查admins中是否有人不是社群成員，如果有成員不屬於社群成員，直接從跟新內容移除該成員
    if(!forumData.getAdmins().isEmpty()) {
      forumData.setAdmins(communityService
              .getAllMemberOfCommunityById(
                      communityInfo.getCommunityId(),
                      null,
                      forumData.getAdmins(), Role.COMMUNITY_MEMBER.getId(), -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList()));
    }

    long milliseconds = System.currentTimeMillis();
    fillInForumInfo(forumInfo, forumId, forumData, milliseconds);

    Map<String, List<String>> preparedList = prepareMemberList(forumData.getType(), forumData.getAdmins(), forumData.getMembers());
    List<String> adminList = preparedList.get(Identity.ADMIN.name());
    List<String> memberList = preparedList.get(Identity.MEMBER.name());
    List<String> filteredMemberList = new ArrayList<>();
    // 每个批次处理batchSize个元素
    if(memberList != null) for (int i = 0; i < memberList.size(); i += BATCH_SIZE) {
      // 获取子列表，范围是从i到i + batchSize，最后一个批次可能小于1000个
      List<String> subList = memberList.subList(i, Math.min(i + BATCH_SIZE, memberList.size()));

      // 调用 communityService.getAllMemberOfCommunityById 并收集结果
      List<String> processedList = communityService
              .getAllMemberOfCommunityById(
                      communityInfo.getCommunityId(),
                      null,
                      subList, Role.COMMUNITY_MEMBER.getId(), -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());

      filteredMemberList.addAll(processedList);
    }

    if (forumDao.update(forumInfo) > 0
            && overwriteForumMembers(adminList, forumInfo.getForumId(), Role.FORUM_ADMIN)
            && overwriteForumMembers(filteredMemberList, forumInfo.getForumId(), Role.FORUM_MEMBER)) {

      // notification
      List<String> admins = getMemberByRole(forumId, Role.FORUM_ADMIN.getId());
      List<String> members = getMemberByRole(forumId, Role.FORUM_MEMBER.getId());
      if (isPublicForum(forumData.getType())
              || Objects.equals(forumData.getType(), ForumType.SYSTEM)) {
        updateForumNotify(forumInfo, admins, forumData.getAdmins());
        eventPublishService.publishForumChangingEvent(
            forumInfo.getCommunityId(),
            forumInfo.getForumId(),
            forumInfo.getForumModifiedUserId(),
            milliseconds);
        row = HttpStatus.SC_CREATED;
      } else if (Objects.equals(forumData.getType(), ForumType.PRIVATE)) {
        updateForumNotify(forumInfo, members, forumData.getMembers());
        eventPublishService.publishForumChangingEvent(
            forumInfo.getCommunityId(),
            forumInfo.getForumId(),
            forumInfo.getForumModifiedUserId(),
            milliseconds);
        row = HttpStatus.SC_CREATED;
      }

      forumDao.deleteAllTags(forumId);
      if ((forumData.getTag() != null)
          && !forumData.getTag().isEmpty()
          && !addTagIntoForumTag(forumInfo.getForumId(), forumData.getTag())) {
        throw new IllegalArgumentException(I18nConstants.MSG_FORUM_TAG_INPUT);
      }
      publishActivityLogEventFromForum(
          Utility.getUserIdFromSession(),
          forumInfo,
          Operation.UPDATE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);

      // add new logic: when forumData's type is PRIVATE or PUBLIC, operate on all topics
      if (drcSyncConfig.getCommunityId().contains(forumInfo.getCommunityId())) {
        ForumType forumType = forumData.getType();
        boolean isTypeChanged = (originalType == ForumType.PUBLIC && forumType == ForumType.PRIVATE)
                || (originalType == ForumType.PRIVATE && forumType == ForumType.PUBLIC);
        if (isTypeChanged) {
          // get all topics with status open and locked
          List<TopicInfo> topics = topicService.getAllByForumIdAdnStatus(forumInfo.getForumId(), TopicStatus.OPEN.toString());
          topics.addAll(topicService.getAllByForumIdAdnStatus(forumInfo.getForumId(), TopicStatus.LOCKED.toString()));

          DrcSyncType syncType = ((forumType == ForumType.PRIVATE) ? DrcSyncType.DELETE : DrcSyncType.CREATE);

          for (TopicInfo topicInfo : topics) {
            eventPublishService.publishDrcSyncEvent(
                    drcSyncConfig.getDatabase(),
                    syncType,
                    topicInfo.getTopicId(),
                    topicInfo.getTopicTitle(),
                    topicInfo.getTopicText(),
                    forumInfo.getCommunityId(),
                    forumInfo.getForumId());
          }
        }
      }
    }
    return row;
  }

  private boolean overwriteForumMembers(List<String> memberIdList, int forumId, Role role) {
    List<String> updatedMemberIdList = Optional.ofNullable(memberIdList).orElse(new ArrayList<>());
    String currentGid = forumDao.getForumRole(forumId, Collections.singletonList(role.getId()))
            .stream()
            .map(RoleDetailEntity::getGroupId)
            .findFirst()
            .orElse("");
    return userGroupAdapter.overwriteUserGroupMembers(
            role,
            forumId,
            yamlConfig.getAppGroupRootId(),
            updatedMemberIdList,
            currentGid);
  }

  private void fillInForumInfo(
      ForumInfo forumInfo, int forumId, UpdatedForumData forumData, long milliseconds) {
    forumInfo.setForumId(forumId);
    forumInfo.setCommunityId(forumData.getCommunityId());
    forumInfo.setForumType(forumData.getType().toString());
    forumInfo.setForumName(forumData.getName().trim());
    forumInfo.setForumStatus(forumData.getStatus().toString());
    forumInfo.setForumModifiedUserId(Utility.getUserIdFromSession());
    forumInfo.setForumModifiedTime(milliseconds);
  }

  private boolean checkUserCanUpdate(String userId, ForumInfo forumInfo) {
    return checkUserPermission(userId, forumInfo, Operation.UPDATE);
  }

  private EmailWithChineseAndEnglishContext getDeleteForumContext(
      ForumInfo forumInfo) {
    String userName = Utility.getUserFromSession().getCommonName();
    String link = String.format(EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getHost());
    String mobileLink =
        String.format(
            EmailConstants.COMMUNITY_DEFAULT_URI_FORMAT, yamlConfig.getMobileDownloadUrl());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.FORUMDELETION)
        .extraMemberType(EmailMemberType.ALLFORUMMEMBER)
        .forumInfo(forumInfo)
        .sender(userName)
        .desc(
            String.format(
                EmailConstants.FORUM_DELETE_FORUM_CHINESE_FORMAT,
                forumInfo.getForumName(),
                userName))
        .englishDesc(
            String.format(
                EmailConstants.FORUM_DELETE_FORUM_ENGLISH_FORMAT,
                forumInfo.getForumName(),
                userName))
        .subject(EmailConstants.JOIN_REMOVE_DELETE_SUBJECT_FORMAT)
        .link(link)
        .mobileLink(mobileLink);
  }

  public String deleteForum(Integer forumId, ForumStatusInput deleteStatus) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserCanDelete(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }

    long milliseconds = System.currentTimeMillis();
    forumInfo.setForumStatus(deleteStatus.getStatus().toString());
    forumInfo.setForumDeleteUserId(Utility.getUserIdFromSession());
    forumInfo.setForumDeleteTime(milliseconds);

    if (drcSyncConfig.getCommunityId().contains(forumInfo.getCommunityId()) &&
            forumInfo.isPublicForum()) {
        List<TopicInfo> topics = new ArrayList<>();
        topics.addAll(topicService.getAllByForumIdAdnStatus(forumInfo.getForumId(), TopicStatus.OPEN.toString()));
        topics.addAll(topicService.getAllByForumIdAdnStatus(forumInfo.getForumId(), TopicStatus.LOCKED.toString()));
        for (TopicInfo topicInfo : topics) {
          eventPublishService.publishDrcSyncEvent(
                  drcSyncConfig.getDatabase(),
                  DrcSyncType.DELETE,
                  topicInfo.getTopicId(),
                  topicInfo.getTopicTitle(),
                  topicInfo.getTopicText(),
                  forumInfo.getCommunityId(),
                  forumInfo.getForumId());
        }
    }

    eventPublishService.publishForumDeletingEvent(
        forumInfo.getCommunityId(),
        forumId,
        Utility.getUserIdFromSession(),
        milliseconds,
        forumInfo.getForumDdfId());
    if (forumDao.delete(forumInfo) > 0) {
      // notify members
      EmailWithChineseAndEnglishContext deleteContext =
              getDeleteForumContext(forumInfo);
      eventPublishService.publishEmailSendingEvent(deleteContext);
      eventPublishService.publishNotificationSendingEvent(
              getDeleteForumNotification(forumInfo));

      // update info
      if (forumInfo.getForumToppingOrder() != DEFAULT_FORUM_ORDER) {
        forumDao.updateNormalForum(forumId);
      }
      publishActivityLogEventFromForum(
          Utility.getUserIdFromSession(),
          forumInfo,
          Operation.DELETE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);
      return I18nConstants.MSG_DELETE_FORUM;
    }
    return "";
  }

  private boolean checkUserCanDelete(String userId, ForumInfo forumInfo) {
    return checkUserPermission(userId, forumInfo, Operation.DELETE);
  }

  public boolean updateLastModifiedOfForum(int forumId, String userId, long time) {
    return forumDao.updateLastModifiedOfForum(forumId, userId, time) != 0;
  }

  // 在forum, topic page, 沒有admin邏輯
  public Identity getUserIdentityOfForum(String userId, ForumInfo forumInfo, boolean isIgnore) {
    if (userService.isSysAdmin(userId)) {
      return Identity.OWNER;
    }
    if (checkUserIsAdmin(userId, forumInfo)) {
      return Identity.OWNER;
    }
    if (checkUserIsMemberOfForum(userId, forumInfo)) {
      return Identity.MEMBER;
    }
    if (ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType())) && !isIgnore) {
      throw new UnauthorizedException(I18nConstants.MSG_MUST_BE_MEMBER);
    }
    return Identity.GUEST;
  }

  public List<Tag> getTagOfForum(int forumId) {
    return forumDao.getTagOfForum(forumId);
  }

  public MemberListResult getMemberListResult(
      int forumId, int offset, int limit, SortField sortType, String q,
      String userId, Boolean isImgAvatar) {
    List<String> userListId = (userId.isEmpty()) ? null : Collections.singletonList(userId);
    List<User> memberList = getMemberOfForumWithFilters(
            false, forumId, offset, limit, q,
            userListId, null, sortType.toString());
    List<String> userIdList = memberList
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    int numFound = extractUserCount(getMemberOfForumWithFilters(
            true, forumId, offset, limit, q,
            userListId, null, sortType.toString()));

    List<UserSession> userList = userService.getUserById(userIdList, new ArrayList<>());
    List<MemberInfoDetail> memberInfoList =
        getMemberInfoDetail(memberList, userList, true, isImgAvatar);
    return new MemberListResult().result(memberInfoList).numFound(numFound);
  }

  private List<MemberInfoDetail> getMemberInfoDetail(
      List<User> memberList,
      List<UserSession> userList,
      boolean checkIsAdmin,
      Boolean isImgAvatar) {
    Map<String, UserSession> userMap =
        userList
            .stream()
            .collect(Collectors.toMap(UserSession::getCommonUUID, Function.identity()));
    return memberList
        .stream()
        .map(
            item ->
                transferInternalTalentUserToMemberInfoDetail(
                        userMap.getOrDefault(item.getId(), new UserSession()), item.getId(), isImgAvatar)
                    .isAdmin(
                            checkIsAdmin
                                ? item.getRoleId().equals(Role.FORUM_ADMIN.getId()) || item.getRoleId().equals(Role.COMMUNITY_ADMIN.getId())
                                : false))
        .collect(Collectors.toList());
  }

  private MemberInfoDetail transferInternalTalentUserToMemberInfoDetail(
      UserSession user, String userId, Boolean isImgAvatar) {
    return new MemberInfoDetail()
        .id(userId)
        .name(user.getCommonName())
        .mail(user.getProfileMail())
        .department(user.getProfileDeptName())
        .ext(user.getProfilePhone())
        .imgAvatar(Boolean.TRUE.equals(isImgAvatar) ? user.getCommonImage() : StringUtils.EMPTY)
        .status(UserStatus.fromValue(user.getStatus()));
  }

  public MemberListResult getUserListNotInForumOfCommunity(
      int forumId, int offset, int limit, SortField sortType, String q,
      String userId, Boolean isImgAvatar) {
    List<String> userListId = (userId.isEmpty()) ? null : Collections.singletonList(userId);
    List<Integer> roleIdList = Collections.singletonList(Role.COMMUNITY_MEMBER.getId());
    List<User> memberList = forumDao
            .getNotMemberListWithSortAndLimit(false, forumId,
                    true, roleIdList, q, userListId,
                    null, offset, limit, sortType.toString());
    List<String> userIdList = memberList
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    int numFound = extractUserCount(forumDao
            .getNotMemberListWithSortAndLimit(true, forumId,
                    true, roleIdList, q, userListId,
                    null, offset, limit, sortType.toString()));

    List<UserSession> userList = userService.getUserById(userIdList, new ArrayList<>());
    List<MemberInfoDetail> memberInfoList =
            getMemberInfoDetail(memberList, userList, false, isImgAvatar);
    return new MemberListResult().result(memberInfoList).numFound(numFound);
  }

  public String updateMemberRoleOfForum(
      int forumId, String memberId, com.delta.dms.community.swagger.model.Role role) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkUserCanEditMember(Utility.getCurrentUserIdWithGroupId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_FORUM_MANAGER);
    }
    if (communityService.isFromDmsGroupMember(memberId, forumInfo.getCommunityId())) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_TRANSFER_GROUP_MEMBER);
    }

    List<User> adminListOfCommunity =
        communityService.getAdminListOfCommunity(forumInfo.getCommunityId(), null,
                Collections.singletonList(memberId), -1);
    if (adminListOfCommunity.stream().map(User::getId).anyMatch(item -> item.equals(memberId))) {
      throw new IllegalArgumentException(
          I18nConstants.MSG_COMMUNITY_ADMIN_CANNOT_TRANSFER_FORUM_ROLE);
    }

    boolean result = setRoleOfUser(forumId, memberId, role.getRole());
    if (result) {
      return I18nConstants.MSG_FORUM_ROLE_TRANSFER_OK;
    } else {
      throw new CommunityException(I18nConstants.MSG_FORUM_ROLE_TRANSFER_FAILED);
    }
  }

  private boolean setRoleOfUser(int forumId, String userId, Identity identity) {
    boolean result = false;
    if (Identity.OWNER.equals(identity) || Identity.ADMIN.equals(identity)) {
      result = appendForumMembers(Collections.singletonList(userId), forumId, Role.FORUM_ADMIN);
    } else if (Identity.MEMBER.equals(identity)) {
      result = deleteForumMembers(Collections.singletonList(userId), forumId, Role.FORUM_ADMIN);
    } else {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_ROLE_TRANSFER_FAILED);
    }
    return result;
  }

  public void publishActivityLogEventFromForum(
      String userId,
      ForumInfo forumInfo,
      Operation operation,
      String origin,
      String content,
      String attachmentId) {
    PermissionObject permissionObject =
        !ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))
            ? PermissionObject.PUBLICFORUM
            : PermissionObject.PRIVATEFORUM;

    eventPublishService.publishActivityLogEvent(
        Utility.setActivityLogData(
            userId,
            operation.toString(),
            permissionObject.toString(),
            forumInfo.getForumId(),
            origin,
            content,
            attachmentId));

    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            ActivityLogUtil.getAppName(origin),
            Constants.ACTIVITY_APP_VERSION,
            userId,
            ActivityLogUtil.getOperationEnumOfActivityLog(operation),
            ActivityLogUtil.getObjectType(ObjectType.FORUMID, attachmentId),
            ActivityLogUtil.getObjectId(forumInfo.getForumId(), attachmentId),
            ActivityLogUtil.getAnnotation(permissionObject, content),
            LogStatus.SUCCESS,
            LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
            ObjectType.COMMUNITYID,
            String.valueOf(forumInfo.getCommunityId())));
  }

  public void deleteAllMemberJoinApplicationOfForum(int forumId) {
    forumDao.deleteAllMemberJoinApplicationOfForum(forumId);
  }

  public int lockForum(int forumId, String userId, long lockedTime) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    eventPublishService.publishForumLockingEvent(forumId, userId, lockedTime);
    setLockedForum(forumInfo, userId, lockedTime);
    if (forumDao.update(forumInfo) > 0) {
      if (ForumType.PRIVATE.toString().equals(forumInfo.getForumType())) {
        /*
        String groupId =
            communityService.getCommunityInfoById(forumInfo.getCommunityId()).getCommunityGroupId();
        List<String> communityAdminList =
            communityService
                .getAdminListOfCommunity(forumInfo.getCommunityId(), null, null, -1)
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
        final List<String> adminIdList =
            getAdminListOfForum(forumId, -1, -1)
                .stream()
                .map(User::getId)
                .filter(item -> !communityAdminList.contains(item))
                .collect(Collectors.toList());
        final List<String> memberIdList =
            getMemberOfForum(forumId, -1, -1).stream().map(User::getId).collect(Collectors.toList());
        forumDao.deleteAllMemberByforumId(forumId);
        forumDao.deleteAllMemberJoinApplicationOfForum(forumId);
        addAdminIntoForum(adminIdList, forumId);
        addMemberIntoForum(memberIdList, forumId);
         */
      }
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }

  private void setLockedForum(ForumInfo forumInfo, String userId, long lockedTime) {
    forumInfo.setForumStatus(ForumStatus.LOCKED.toString());
    forumInfo.setForumModifiedUserId(userId);
    forumInfo.setForumModifiedTime(lockedTime);
  }

  private Notification getForumJoinApplicationNotification(
      ForumInfo forumInfo, String content, long now) {
    List<String> adminIdList =
        getAdminListOfForum(forumInfo.getForumId(), -1, -1)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    UserSession user = Utility.getUserFromSession();
    return new Notification()
        .userId(adminIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.FORUMJOINAPPLICATION)
        .title(user.getCommonName())
        .content(content)
        .priority(NotificationConstants.PRIORITY_5)
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .senderId(user.getCommonUUID())
        .time(now);
  }

  private Notification getApproveJoinApplicationNotification(
      ForumInfo forumInfo, String applicantId, long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.FORUMJOINAPPROVAL)
        .title(forumInfo.getForumName())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .time(now);
  }

  private Notification getRejectJoinApplicationNotification(
      ForumInfo forumInfo, String applicantId, long now) {
    return new Notification()
        .userId(applicantId)
        .type(EmailType.FORUMJOINREJECTION)
        .title(forumInfo.getForumName())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .time(now);
  }

  private Notification getNotification(NotificationDetail notificationDetail, ForumInfo forumInfo) {
    return new Notification()
        .userId(
            getRecipientList(forumInfo.getForumId(), notificationDetail)
                .stream()
                .collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.FORUMNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationDetail.getType())) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .title(forumInfo.getForumName())
        .content(notificationDetail.getContent())
        .senderId(Utility.getUserIdFromSession())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .time(new Date().getTime());
  }

  private Notification getJoinForumNotification(ForumInfo forumInfo, List<String> members) {
    return new Notification()
        .userId(members.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.JOINFORUM)
        .title(forumInfo.getForumName())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumCreatedById(forumInfo.getForumCreateUserId())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .forumCreatedById(Utility.getUserIdFromSession())
        .time(new Date().getTime());
  }

  private Notification getRemoveForumNotification(ForumInfo forumInfo, List<String> members) {
    return new Notification()
        .userId(members.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.REMOVEFROMFORUM)
        .title(forumInfo.getForumName())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .time(new Date().getTime());
  }

  private Notification getDeleteForumNotification(ForumInfo forumInfo) {
    return new Notification()
        .type(EmailType.FORUMDELETION)
        .extraMemberType(EmailMemberType.ALLFORUMMEMBER)
        .title(forumInfo.getForumName())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .forumType(ForumType.fromValue(forumInfo.getForumType()))
        .senderId(Utility.getUserIdFromSession())
        .time(new Date().getTime());
  }

  public String updatePinOfForum(int forumId, Pin pin) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (!checkUserCanPin(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    if (pin.getPin().equals(PinEnum.PIN)) {
      int number = forumDao.countMaxNumberOfTopForum(forumInfo.getCommunityId());
      if (number == MAXIMUM_OF_TOP_FORUM) {
        throw new CommunityException(I18nConstants.MSG_TOPFORUM_MAXNUMBER);
      }
      if (forumDao.updateTopForum(forumInfo.getCommunityId(), forumId) != 0) {
        return I18nConstants.MSG_SET_TOPFORUM_SUCCESSFUL;
      } else {
        throw new CommunityException(I18nConstants.MSG_SET_TOPFORUM_FAIL);
      }
    } else {
      if (forumInfo.getForumToppingOrder() == DEFAULT_FORUM_ORDER) {
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
      }
      forumDao.updateNormalForum(forumId);
      return I18nConstants.MSG_SET_NORMALFORUM_SUCCESSFUL;
    }
  }

  public String updatePriorityOfForum(int forumId, int toppingOrder) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    if (!checkUserCanPin(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    if (forumDao.swapPriorityOfForum(forumId, toppingOrder) != 0) {
      return Constants.RESPONSE_SUCCESS;
    } else {
      return Constants.RESPONSE_ERROR;
    }
  }

  public HotForumSearchResult searchHotForumList(int communityId, int offset, int limit) {
    List<ForumListDetail> hotForumListDetailList =
        searchHotForumOfCommunityByType(communityId, offset, limit);
    int numFound = hotForumListDetailList.size();
    return new HotForumSearchResult().result(hotForumListDetailList).numFound(numFound);
  }

  public List<ForumListDetail> searchHotForumOfCommunityByType(
      int communityId, int offset, int limit) {
    List<ForumInfo> forumInfoList = forumDao.getHotForumOfCommunity(communityId, offset, limit, false, "",
            yamlConfig.getHotLastingMin() * MINUTE_TO_SECOND_MULTIPLY);
    return getForumListDetails(forumInfoList);
  }

  private boolean checkUserCanPin(ForumInfo forumInfo) {
    return checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, Operation.PIN);
  }

  public List<IdNameDto> getPrivilegedForumOfCommunity(int communityId) {
    return forumDao
        .getPrivilegedForumOfCommunity(
            communityId, userService.isSysAdmin(), Utility.getCurrentUserIdWithGroupId())
        .stream()
        .map(
            item ->
                new IdNameDto().id(Integer.valueOf(item.getId().toString())).name(item.getName()))
        .collect(Collectors.toList());
  }

  public List<String> getDeletingMemberInfo(
      List<User> forumAdminList, List<User> forumMemberList, List<User> communityAllMemberList) {
    List<User> forumAllMemberList =
        Stream.concat(forumAdminList.stream(), forumMemberList.stream())
            .distinct()
            .collect(Collectors.toList());
    ArrayList<String> communityAllMembersClone =
        new ArrayList<>(
            communityAllMemberList.stream().map(User::getId).collect(Collectors.toList()));
    ArrayList<String> forumAllMembersClone =
        new ArrayList<>(forumAllMemberList.stream().map(User::getId).collect(Collectors.toList()));
    forumAllMembersClone.removeAll(communityAllMembersClone);

    return forumAllMembersClone;
  }

  public List<ForumInfo> getAllForumInfoByCommunityIdAndStatus(
      int communityId, String forumStatus) {
    return forumDao.getAllByCommunityIdAndStatus(communityId, forumStatus);
  }

  public int moveForum(int forumId, ForumMoveData forumMoveData) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    CommunityInfo communityInfo =
        communityService.getCommunityInfoById(forumMoveData.getCommunityId());
    List<Integer> adminCommunityIdList = communityService.getPrivilegedAllCommunity()
            .parallelStream()
            .map(IdNameDto::getId)
            .collect(Collectors.toList());
    if (!userService.isSysAdmin() && !adminCommunityIdList.containsAll(
        Arrays.asList(communityInfo.getCommunityId(), forumInfo.getCommunityId()))) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_MOVE_FORUM_NOT_COMMUNITY_ADMIN);
    }

    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (checkDuplicateForumName(
        forumMoveData.getCommunityId(), StringUtils.EMPTY, forumInfo.getForumName())) {
      throw new IllegalArgumentException(I18nConstants.MSG_FORUM_NAME_REPEAT);
    }
    if (forumInfo.getForumModifiedTime() != 0
        && forumInfo.getForumModifiedTime() != forumMoveData.getForumModifiedTime()) {
      return HttpStatus.SC_CONFLICT;
    }
    if (CommunityStatus.LOCKED.toString().equals(communityInfo.getCommunityStatus())
        || CommunityStatus.CLOSED.toString().equals(communityInfo.getCommunityStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }

    if (DEFAULT_FORUM_ORDER != forumInfo.getForumToppingOrder()) {
      updatePinOfForum(forumId, new Pin().pin(PinEnum.UNPIN));
    }

    communityService.addMemberIntoCommunity(
        getMemberOfForum(forumId, -1, -1).parallelStream().map(User::getId).collect(Collectors.toList()),
        forumMoveData.getCommunityId());

    forumInfo.setCommunityId(forumMoveData.getCommunityId());
    forumInfo.setForumModifiedTime(System.currentTimeMillis());
    forumInfo.setForumModifiedUserId(Utility.getUserIdFromSession());
    forumDao.update(forumInfo);
    /*
    communityService.syncCommunityUserId(
        communityInfo.getCommunityId(), communityInfo.getCommunityGroupId());
    eventPublishService.publishSyncForumUserIdInHomePageEvent(
        communityInfo.getCommunityId(), forumId, communityInfo.getCommunityGroupId());
     */
    eventPublishService.publishForumChangingEvent(
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        forumInfo.getForumModifiedUserId(),
        forumInfo.getForumModifiedTime());
    return HttpStatus.SC_OK;
  }

  public int reopenForum(int forumId, String userId, long reopenedTime) {
    ForumInfo forumInfo = getForumInfoById(forumId);
    eventPublishService.publishForumReopeningEvent(forumId, userId, reopenedTime);
    setReopenedForum(forumInfo, userId, reopenedTime);
    if (forumDao.update(forumInfo) > 0) {
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }

  private void setReopenedForum(ForumInfo forumInfo, String userId, long reopenedTime) {
    forumInfo.setForumStatus(ForumStatus.OPEN.toString());
    forumInfo.setForumModifiedUserId(userId);
    forumInfo.setForumModifiedTime(reopenedTime);
  }

  public CommunityResultList getForumListOfCommunity(
      int communityId, List<ForumType> forumTypeList, int offset, int limit, Order sort) {
    if (offset >= NumberUtils.INTEGER_ZERO && limit > NumberUtils.INTEGER_ZERO) {
      int toppingForumNumber = forumDao.getToppingForumOfCommunity(communityId, -1, -1).size();
      int page = (offset / limit);
      int mod = (offset % limit);
      limit = toppingForumNumber > limit ? NumberUtils.INTEGER_ZERO : limit - toppingForumNumber;
      offset = mod + page * limit;
    }
    List<ForumInfo> forumInfoList =
        searchForumInfoList(communityId, forumTypeList, offset, limit, sort);
    List<CommunityResultDetail> forumDataList =
        transferForumInfoToCommunityResultDetail(forumInfoList);
    int numFound = countForumOfCommunity(communityId, forumTypeList);
    return new CommunityResultList().result(forumDataList).numFound(numFound);
  }

  private List<CommunityResultDetail> transferForumInfoToCommunityResultDetail(
      List<ForumInfo> forumInfoList) {
    if (forumInfoList.isEmpty()) {
      return Collections.emptyList();
    }
    CommunityInfo communityInfo =
        communityService.getCommunityInfoById(
            forumInfoList
                .parallelStream()
                .map(ForumInfo::getCommunityId)
                .filter(id -> id > 0)
                .distinct()
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException(I18nConstants.MSG_COMMUNITY_NOT_EXIST)));
    String lang = AcceptLanguage.get();
    if (communityService.needChangeAttachedCommunityName(lang)) {
      communityInfo.setCommunityName(
          communityService
              .getAttachedCommunityNames(
                  Collections.singletonMap(
                      communityInfo.getCommunityGroupId(), communityInfo.getCommunityName()),
                  lang)
              .getOrDefault(communityInfo.getCommunityGroupId(), communityInfo.getCommunityName()));
    }
    Identity identityOfCommunity =
        communityService.getUserIdentityOfCommunity(
            Utility.getUserIdFromSession(),
            communityInfo.getCommunityId(),
            communityInfo.getCommunityGroupId());
    Map<String, IdName> userIdNameMap =
        userService
            .getUserByIds(
                forumInfoList
                    .parallelStream()
                    .map(ForumInfo::getForumLastModifiedUserId)
                    .distinct()
                    .collect(Collectors.toList()))
            .stream()
            .collect(
                LinkedHashMap::new,
                (map, item) ->
                    map.put(item.getId(), new IdName().id(item.getId()).name(item.getName())),
                Map::putAll);
    Set<Integer> applicatantForumIds =
        forumDao.getApplicantForumsByUserId(Utility.getUserIdFromSession());
    Map<Integer, List<RoleDetailEntity>> forumRoleRawMap =
        forumDao
            .getRolesByForumIds(
                Utility.getUserIdFromSession(),
                forumInfoList
                    .parallelStream()
                    .map(ForumInfo::getForumId)
                    .collect(Collectors.toList()))
            .stream()
            .collect(Collectors.groupingBy(RoleDetailEntity::getScopeId))
            .entrySet()
            .stream()
            .collect(
                    Collectors.toMap(
                            Entry::getKey,
                            entry -> entry.getValue()
            ));
    Map<Integer, Integer> forumRoleMap = new HashMap<>(Collections.emptyMap());
    for(Integer forumId : forumRoleRawMap.keySet()) {
      List<RoleDetailEntity> roleList = forumRoleRawMap.get(forumId);
      int targetRoleId = getProperForumRoleId(roleList);
      forumRoleMap.put(forumId, targetRoleId);
    }
    return forumInfoList
        .stream()
        .map(
            forumInfo -> {
              Identity forumIdentity =
                  getForumIdentity(
                      forumInfo, identityOfCommunity, forumRoleMap, applicatantForumIds);
              ForumType forumType = ForumType.fromValue(forumInfo.getForumType());
              return new CommunityResultDetail()
                  .type(SearchType.FORUM)
                  .read(getReadPermission(forumIdentity, forumType))
                  .data(
                      new SearchForumData()
                          .id(forumInfo.getForumId())
                          .name(forumInfo.getForumName())
                          .status(ForumStatus.fromValue(forumInfo.getForumStatus()))
                          .modifiedTime(forumInfo.getForumModifiedTime())
                          .lastModifiedUser(
                              Optional.ofNullable(
                                      userIdNameMap.get(forumInfo.getForumLastModifiedUserId()))
                                  .orElseGet(IdName::new))
                          .lastModifiedTime(forumInfo.getForumLastModifiedTime())
                          .type(forumType)
                          .tag(
                              getTagOfForum(forumInfo.getForumId())
                                  .parallelStream()
                                  .map(Tag::getLabel)
                                  .collect(Collectors.toList()))
                          .source(
                              Collections.singletonList(
                                  new Source()
                                      .id(communityInfo.getCommunityId())
                                      .type(SearchType.COMMUNITY)
                                      .name(communityInfo.getCommunityName())))
                          .identity(forumIdentity)
                          .toppingOrder(forumInfo.getForumToppingOrder()));
            })
        .collect(Collectors.toList());
  }

  private int getProperForumRoleId(List<RoleDetailEntity> roleList) {
    // 1 > 3 > 4 > 2
    List<Integer> roleIdList = roleList.stream().map(RoleDetailEntity::getRoleId)
            .collect(Collectors.toList());
    if (roleIdList.contains(Role.COMMUNITY_ADMIN.getId())) {
      return Role.COMMUNITY_ADMIN.getId();
    } else if (roleIdList.contains(Role.FORUM_ADMIN.getId())) {
      return Role.FORUM_ADMIN.getId();
    } else if (roleIdList.contains(Role.FORUM_MEMBER.getId())) {
      return Role.FORUM_MEMBER.getId();
    } else if (roleIdList.contains(Role.COMMUNITY_MEMBER.getId())) {
      return Role.COMMUNITY_MEMBER.getId();
    } else {
      return 0;
    }
  }

  private boolean getReadPermission(Identity identity, ForumType forumType) {
    if (ForumType.PUBLIC == forumType) {
      return true;
    }
    return Identity.GUEST != identity && Identity.APPLICANT != identity;
  }

  private Identity getForumIdentity(
      ForumInfo forumInfo,
      Identity communityIdentity,
      Map<Integer, Integer> forumRoleMap,
      Set<Integer> applicatantForumIds) {
    int forumRoleId = forumRoleMap.getOrDefault(forumInfo.getForumId(), NumberUtils.INTEGER_ZERO);
    if (Identity.ADMIN == communityIdentity) {
      return Identity.ADMIN;
    }
    if (Role.FORUM_ADMIN.getId() == forumRoleId) {
      return Identity.OWNER;
    }
    if (Role.FORUM_MEMBER.getId() == forumRoleId) {
      return Identity.MEMBER;
    }
    if (ForumType.PRIVATE == ForumType.fromValue(forumInfo.getForumType())) {
      if (applicatantForumIds.contains(forumInfo.getForumId())) {
        return Identity.APPLICANT;
      }
    } else {
      if (Identity.MEMBER == communityIdentity) {
        return Identity.MEMBER;
      }
    }
    return Identity.GUEST;
  }

  public CommunityResultList getHotForumListOfCommunity(int communityId, int offset, int limit) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    List<ForumInfo> forumInfoList = forumDao.getHotForumOfCommunity(communityId, offset, limit, dlInfo.isDL, dlInfo.getAllowForumId(),
            yamlConfig.getHotLastingMin() * MINUTE_TO_SECOND_MULTIPLY);
    List<CommunityResultDetail> forumDataList =
        transferForumInfoToCommunityResultDetail(forumInfoList);
    int numFound = forumInfoList.size();
    return new CommunityResultList().result(forumDataList).numFound(numFound);
  }

  public CommunityResultList getToppingForumListOfCommunity(
      int communityId, int offset, int limit) {
    List<ForumInfo> forumInfoList = forumDao.getToppingForumOfCommunity(communityId, offset, limit);
    List<CommunityResultDetail> forumDataList =
        transferForumInfoToCommunityResultDetail(forumInfoList);
    int numFound = forumInfoList.size();
    return new CommunityResultList().result(forumDataList).numFound(numFound);
  }

  List<String> validateForumMembers(ForumInfo forumInfo, List<String> recipient) {
    List<String> userList = ofNullable(recipient).orElseGet(ArrayList::new);
    if (isPublicForum(forumInfo.getForumType())) {
      return userList;
    }
    Map<String, String> forumMemberMap =
            getMemberOfForumWithFilters(false, forumInfo.getForumId(), -1,
                    -1, EMPTY, userList, null, EMPTY)
            .parallelStream()
            .map(User::getId)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), Function.identity()));
    return userList
        .parallelStream()
        .filter(item -> Objects.nonNull(forumMemberMap.get(item)))
        .collect(Collectors.toList());
  }

  boolean isPublicForum(String forumType) {
    return isPublicForum(ForumType.fromValue(forumType));
  }

  private boolean isPublicForum(ForumType forumType) {
    return ForumType.PUBLIC == forumType;
  }
}
