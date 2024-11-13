package com.delta.dms.community.service;

import static com.delta.dms.community.swagger.model.ForumType.PUBLIC;
import static com.delta.dms.community.utils.Constants.MINUTE_TO_SECOND_MULTIPLY;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.dao.entity.*;
import com.delta.dms.community.enums.DrcSyncType;
import com.delta.dms.community.swagger.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.delta.datahive.DDFQuery.QueryTerm;
import com.delta.datahive.DDFQuery.QueryTree;
import com.delta.datahive.activitylog.UserEchoAPI;
import com.delta.datahive.activitylog.UserEchoException;
import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.Echo;
import com.delta.datahive.activitylog.args.EchoGroup;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.datahive.types.DDFStatus;
import com.delta.datahive.types.SortOrder;
import com.delta.datahive.types.SortableField;
import com.delta.datahive.types.Sorting;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.config.IssueTrackConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.config.DrcSyncConfig;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.enums.DdfDocCat;
import com.delta.dms.community.exception.CommunityException;
import com.delta.dms.community.exception.DuplicationException;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.exception.UpdateConflictException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.service.privilege.topic.TopicPrivilegeService;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.NotificationConstants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class TopicService {

  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final int CONTENT_LENGTH = 80;
  private static final String ORDER_NULL_LAST = "-";
  private static final int MAX_TOPPING_LIMIT = 3;
  private static final int DEFAULT_TOPPING_ORDER = 0;
  private static final int ABNORMAL_FORUM_ID = -100;

  private final ForumService forumService;
  private final UserService userService;
  private final FileService fileService;
  private final RichTextHandlerService richTextHandlerService;
  private final PrivilegeService privilegeService;
  private final EmojiService emojiService;
  private final BookmarkService bookmarkService;
  private final EventPublishService eventPublishService;
  private final CommunityService communityService;
  private final ConclusionAlertService conclusionAlertService;
  private final OrgGroupExtensionService orgGroupExtensionService;
  private final GroupRecipientHandleService groupRecipientHandleService;
  private final TopicPrivilegeService topicPrivilegeService;
  private final UserGroupAdapter userGroupAdapter;
  private final TopicDao topicDao;
  private final YamlConfig yamlConfig;
  private final IssueTrackConfig issueTrackConfig;
  private final DrcSyncConfig drcSyncConfig;
  private final AuthService authService;

  public TopicSearchResult searchTopicListOfCommunity(
      int communityId, int offset, int limit, Order sort, String topicState) {
    List<String> state = getTopicStateList(topicState);
    boolean isSysAdmin = userService.isSysAdmin();
    List<TopicListDetail> topicListDetailList =
        getTopicByCommunityId(communityId, offset, limit, sort, state, isSysAdmin);
    int numFound = countUserCanReadTopicOfCommunity(communityId, state, null, isSysAdmin);
    return new TopicSearchResult()
        .result(topicListDetailList)
        .toppingResult(new ArrayList<>())
        .numFound(numFound);
  }

  public List<TopicListDetail> getTopicByCommunityId(
      int communityId,
      int offset,
      int limit,
      Order sort,
      List<String> topicState,
      boolean isSysAdmin) {
	    //取得DLInfo
		DLInfo dlInfo = authService.getDLUserInfo();
    List<TopicInfo> topicInfoList =
        topicDao.getTopicOfCommunityWithSortAndLimit(
            communityId,
            offset,
            limit,
            getSortField(sort.getProperty()).toString(),
            sort.getDirection().toString(),
            isSysAdmin,
            Utility.getCurrentUserIdWithGroupId(),
            topicState,
            null,
            dlInfo.isDL,
            dlInfo.allowForumId);
    Map<Integer, ForumInfo> forumInfoMap = new HashMap<>();
    topicInfoList
        .stream()
        .map(TopicInfo::getForumId)
        .distinct()
        .forEach(item -> forumInfoMap.put(item, forumService.getForumInfoById(item)));
    Map<Integer, Identity> forumIdentityMap = new HashMap<>();
    topicInfoList
        .stream()
        .map(TopicInfo::getForumId)
        .distinct()
        .forEach(
            item ->
                forumIdentityMap.put(item, getUserIdentityOfForum(forumInfoMap.get(item), false)));
    return topicInfoList
        .stream()
        .map(
            item ->
                transferTopicInfoToTopicListDetail(item)
                    .belongForum(
                        new IdNameDto()
                            .id(item.getForumId())
                            .name(forumInfoMap.get(item.getForumId()).getForumName()))
                    .identity(
                        getUserIdentityOfTopic(
                            Utility.getUserIdFromSession(),
                            forumIdentityMap.get(item.getForumId()),
                            item.getTopicCreateUserId())))
        .collect(Collectors.toList());
  }

  protected List<String> getTopicStateList(String topicState) {
    TopicState state = TopicState.fromValue(topicState);
    if (null == state) {
      return Collections.emptyList();
    }
    if (TopicState.CONCLUDED == state) {
      return Arrays.asList(TopicState.BRIEFCONCLUDED.toString(), TopicState.CONCLUDED.toString());
    } else {
      return Arrays.asList(state.toString());
    }
  }

  private TopicFieldName getSortField(String sortField) {
    if (SortField.UPDATETIME.toString().equals(sortField)) {
      return TopicFieldName.TOPIC_LAST_MODIFIED_TIME;
    } else if (SortField.TYPE.toString().equals(sortField)) {
      return TopicFieldName.TOPIC_TYPE;
    } else if (SortField.STATE.toString().equals(sortField)) {
      return TopicFieldName.TOPIC_STATE;
    } else if (SortField.BOOKMARKCREATETIME.toString().equals(sortField)) {
      return TopicFieldName.BOOKMARK_CREATE_TIME;
    }
    return TopicFieldName.TOPIC_LAST_MODIFIED_TIME;
  }

  private Identity getUserIdentityOfForum(ForumInfo forumInfo, boolean isIgnore) {
    return forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, isIgnore);
  }

  private TopicListDetail transferTopicInfoToTopicListDetail(TopicInfo topicInfo) {
    return new TopicListDetail()
        .id(topicInfo.getTopicId())
        .title(topicInfo.getTopicTitle())
        .type(TopicType.fromValue(topicInfo.getTopicType()))
        .state(TopicState.fromValue(topicInfo.getTopicState()))
        .status(TopicStatus.fromValue(topicInfo.getTopicStatus()))
        .situation(TopicSituation.fromValue(topicInfo.getTopicSituation()))
        .createUser(userService.getUserById(topicInfo.getTopicCreateUserId()))
        .lastModifiedTime(topicInfo.getTopicLastModifiedTime())
        .lastModifiedUser(userService.getUserById(topicInfo.getTopicLastModifiedUserId()))
        .toppingOrder(topicInfo.getTopicToppingOrder());
  }

  public int countTopicOfCommunity(int communityId) {
    return topicDao.countTopicOfCommunity(communityId);
  }

  private int countUserCanReadTopicOfCommunity(
      int communityId, List<String> topicState, String topicType, boolean isSysAdmin) {
	    //取得DLInfo
		DLInfo dlInfo = authService.getDLUserInfo();
    return topicDao.countUserCanReadTopicOfCommunity(
        communityId, isSysAdmin, Utility.getCurrentUserIdWithGroupId(), topicState, topicType, dlInfo.isDL, dlInfo.allowForumId);
  }

  public TopicSearchResult searchTopicListOfForum(
      int forumId, int offset, int limit, Order sort, boolean withTopping, String topicState) {
    ForumInfo forumInfo = forumService.getForumInfoById(forumId);
    if (!checkCurrentUserCanRead(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_PRIVATE_FORUM);
    }
    Identity identity =
        forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false);
    List<TopicListDetail> toppingTopicListDetailList = new ArrayList<>();
    int page = (offset / limit);
    int mod = (offset % limit);
    if (withTopping) {
      toppingTopicListDetailList = getToppingTopicByForumId(forumInfo, identity);
    }
    limit -= toppingTopicListDetailList.size();
    offset = mod + page * limit;
    List<String> state = getTopicStateList(topicState);
    List<TopicListDetail> topicListDetailList =
        getTopicByForumId(forumInfo, identity, offset, limit, sort, state);
    int numFound = countTopicOfForum(forumId, state, null);
    return new TopicSearchResult()
        .toppingResult(toppingTopicListDetailList)
        .result(topicListDetailList)
        .numFound(numFound);
  }

  public boolean checkCurrentUserCanRead(ForumInfo forumInfo) {
    return checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, Operation.READ);
  }

  private boolean checkUserPermission(String userId, ForumInfo forumInfo, Operation operation) {
    PermissionObject permissionObject = getPermissionObjectOfTopic(forumInfo);
    return privilegeService.checkUserPrivilege(
        userId,
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        permissionObject.toString(),
        operation.toString());
  }

  private PermissionObject getPermissionObjectOfTopic(ForumInfo forumInfo) {
    return !ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))
        ? getDetailedPublicForumTopicByCommunityId(forumInfo.getCommunityId())
        : PermissionObject.PRIVATEFORUMTOPIC;
  }

  private PermissionObject getDetailedPublicForumTopicByCommunityId(int communityId) {
    CommunityInfo communityInfo = communityService.getCommunityInfoById(communityId);
    return CommunityType.ACTIVITY.equals(CommunityType.fromValue(communityInfo.getCommunityType()))
        ? PermissionObject.ACTIVETOPIC
        : PermissionObject.PUBLICFORUMTOPIC;
  }

  public List<TopicListDetail> getToppingTopicByForumId(ForumInfo forumInfo, Identity identity) {
    List<TopicInfo> toppingTopicList =
        topicDao.getToppingTopicByForumId(forumInfo.getForumId(), 0, 3);
    return toppingTopicList
        .stream()
        .map(
            item ->
                transferTopicInfoToTopicListDetail(item)
                    .belongForum(
                        new IdNameDto().id(forumInfo.getForumId()).name(forumInfo.getForumName()))
                    .identity(
                        getUserIdentityOfTopic(
                            Utility.getUserIdFromSession(), identity, item.getTopicCreateUserId())))
        .collect(Collectors.toList());
  }

  private List<TopicListDetail> getTopicByForumId(
      ForumInfo forumInfo,
      Identity identity,
      int offset,
      int limit,
      Order sort,
      List<String> topicState) {
    List<TopicInfo> topicInfoList =
        getTopicOfForumWithSortAndLimit(
            forumInfo.getForumId(), offset, limit, sort, topicState, null);
    return topicInfoList
        .stream()
        .map(
            item ->
                transferTopicInfoToTopicListDetail(item)
                    .belongForum(
                        new IdNameDto().id(forumInfo.getForumId()).name(forumInfo.getForumName()))
                    .identity(
                        getUserIdentityOfTopic(
                            Utility.getUserIdFromSession(), identity, item.getTopicCreateUserId())))
        .collect(Collectors.toList());
  }

  public List<TopicInfo> getTopicOfForumWithSortAndLimit(
      int forumId, int offset, int limit, Order sort, List<String> topicState, String topicType) {
    return topicDao.getTopicOfForumWithSortAndLimit(
        forumId,
        offset,
        limit,
        getSortField(sort.getProperty()).toString(),
        sort.getDirection().toString(),
        topicState,
        topicType);
  }

  public int countTopicOfForum(int forumId, List<String> topicState, String topicType) {
    return topicDao.countTopicOfForum(forumId, topicState, topicType);
  }

  public TopicInfo getTopicInfoById(int topicId) {
    TopicInfo topicInfo = topicDao.getTopicById(topicId);
    if (topicInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_NOT_EXIST);
    }
    return topicInfo;
  }

  public TopicInfo getTopicHomePage(int topicId) {
    TopicInfo topicInfo = topicDao.getTopicHomePage(topicId, AcceptLanguage.getLanguageForDb());
    if (topicInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_NOT_EXIST);
    }
    if (PUBLIC == ForumType.fromValue(getForumTypeById(topicId))) {
      topicInfo.setTopicText(richTextHandlerService.changeToCacheUrl(topicInfo.getTopicText()));
    }
    return topicInfo;
  }

  public int addViewCount(int topicId) {
    return topicDao.addViewCountOfTopic(topicId);
  }

  public int createTopic(TopicCreationData topicData, boolean fromIssueTrack) {
    defaultTopicData(topicData);
    validateAppField(topicData.getAppField(), topicData.getAttachment());
    ForumInfo forumInfo = forumService.getForumInfoById(topicData.getForumId());
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!isSupportTopicType(forumInfo.getSupportTopicType(), topicData.getType())) {
      throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_NOT_SUPPORT);
    }
    if (!checkCurrentUserCanCreate(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_CREATE_TOPIC);
    }
    long attachmentTotalSize = getAttachmentTotalSize(topicData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    TopicInfo topicInfo = convertToTopicInfo(topicData, fromIssueTrack);
    if (!addTopic(topicInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_TOPIC_FAILED);
    }
    int topicId = topicInfo.getTopicId();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());
    setTopicInfoWithText(
        topicInfo,
        ForumType.fromValue(forumInfo.getForumType()),
        forumAdminRoleList,
        forumMemberRoleList,
        "");
    if (!addTextOfTopic(topicInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_TOPIC_FAILED);
    }
    addTagOfTopic(topicId, topicData.getTag());
    addTopicAppField(
        topicId,
        topicData
            .getAppField()
            .stream()
            .map(item -> item.getValue().toString())
            .collect(Collectors.toList()));
    topicData
        .getAttachment()
        .forEach(
            item -> {
              addAttachmentOfTopic(topicId, item.getId());
              addTopicAttachmentAppField(
                  item.getId(),
                  item.getAppField()
                      .stream()
                      .map(appField -> appField.getValue().toString())
                      .collect(Collectors.toList()));
              eventPublishService.publishFileUploadingEvent(
                  item.getId(),
                  item.getAuthor(),
                  forumInfo.getCommunityId(),
                  forumInfo.getForumId(),
                  ForumType.fromValue(forumInfo.getForumType()),
                  false,
                  item.getVideoLanguage());
            });
    eventPublishService.publishTopicChangingEvent(
        topicInfo.getTopicId(),
        forumInfo.getForumId(),
        forumInfo.getCommunityId(),
        topicInfo.getTopicCreateUserId(),
        topicInfo.getTopicCreateTime());
    sendNotification(
        topicInfo,
        forumInfo,
        topicInfo.getTopicTitle(),
        topicInfo.getTopicText(),
        topicData.getNotificationType(),
        topicData.getRecipient(),
        topicData.getOrgMembers(),
        topicData.getBgbus());
      if (topicDao.getConclusionAlertByTopicType(topicInfo.getTopicType())) {
      conclusionAlertService.alertNewTopic(
          new ConclusionAlertTopicInfo()
              .setTopicId(topicInfo.getTopicId())
              .setTopicTitle(topicInfo.getTopicTitle())
              .setForumId(forumInfo.getForumId())
              .setForumName(forumInfo.getForumName())
              .setDuration(NumberUtils.INTEGER_ZERO)
              .setTopicType(TopicType.fromValue(topicInfo.getTopicType()))
              .setTopicText(topicInfo.getTopicText()));
    }
    String userId = Utility.getUserIdFromSession();
    publishActivityLogEventFromTopic(
        userId,
        forumInfo.getForumType(),
        topicId,
        Operation.CREATE,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    topicData
        .getAttachment()
        .stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                publishActivityLogEventFromTopic(
                    userId,
                    forumInfo.getForumType(),
                    topicId,
                    Operation.UPLOAD,
                    Constants.INFO_PROJECT_NAME,
                    Constants.CONTENT_EMPTY,
                    item.getId()));
    if (drcSyncConfig.getCommunityId().contains(forumInfo.getCommunityId()) &&
        forumInfo.isPublicForum()) {
      eventPublishService.publishDrcSyncEvent(
              drcSyncConfig.getDatabase(),
              DrcSyncType.CREATE,
              topicInfo.getTopicId(),
              topicInfo.getTopicTitle(),
              topicInfo.getTopicText(),
              forumInfo.getCommunityId(),
              forumInfo.getForumId());
    }
    return topicId;
  }

  public int createIssueTrackingTopic(TopicCreationData topicData, boolean fromIssueTrack) {
    // Set issue track forum id
    topicData.setForumId(Integer.parseInt(issueTrackConfig.getForumId()));
    // Add members into the community
    communityService.addMemberIntoCommunity(
        topicData.getRecipient(), communityService.getCommunityIdByForumId(topicData.getForumId()));
    // Add members into the forum
    forumService.addMemberIntoForum(topicData.getRecipient(), topicData.getForumId());
    List<User> adminList =
        forumService.getAdminListOfForum(Integer.parseInt(issueTrackConfig.getForumId()));
    // Add issue track admin id
    topicData.setRecipient(
        adminList.stream().map(user -> user.getId()).collect(Collectors.toList()));
    return createTopic(topicData, fromIssueTrack);
  }

  private boolean checkCurrentUserCanCreate(ForumInfo forumInfo) {
    return checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, Operation.CREATE);
  }

  private boolean addTopic(TopicInfo topicInfo) {
    if (checkDuplicateTopic(topicInfo.getForumId(), StringUtils.EMPTY, topicInfo.getTopicTitle())) {
      throw new DuplicationException(I18nConstants.MSG_DUPLICATE_TITLE);
    }
    return topicDao.addInfo(topicInfo) != 0;
  }

  private boolean checkDuplicateTopic(int forumId, String originalTitle, String title) {
    return topicDao.checkDuplicateTopicOfForum(forumId, originalTitle, title) != 0;
  }

  private TopicInfo setTopicInfoWithText(
      TopicInfo topicInfo,
      ForumType forumType,
      List<String> forumAdminRoleList,
      List<String> forumMemberRoleList,
      String originalText) {
    String topicText =
        richTextHandlerService.replaceCopyedImageToBase64(topicInfo.getTopicText(), originalText);
    String html =
        richTextHandlerService
            .replaceRichTextImageSrcWithImageDataHiveUrl(
                forumType,
                fileService.getRoleMap(Utility.getUserIdFromSession(), new ArrayList<>()),
                fileService.getPrivilege(
                    topicInfo.getTopicCreateUserId(), forumAdminRoleList, forumMemberRoleList),
                topicText)
            .getText();
    topicInfo.setTopicText(html);
    return topicInfo;
  }

  private boolean addTextOfTopic(TopicInfo topicInfo) {
    return topicDao.addText(topicInfo) != 0;
  }

  private void addTagOfTopic(int topicId, List<String> tag) {
    if (!CollectionUtils.isEmpty(tag)) {
      topicDao.addTagOfTopic(topicId, tag);
    }
  }

  private void addTopicAppField(int topicId, List<String> appFieldIdList) {
    Optional.ofNullable(appFieldIdList)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(list -> topicDao.insertTopicAppField(topicId, list));
  }

  private void addAttachmentOfTopic(int topicId, String attachmentId) {
    BaseSection attachment =
        fileService.readDdf(attachmentId, FileService.DDF_BASE_FIELD).getBaseSection();
    if (attachment != null) {
      topicDao.addAttachmentOfTopic(
          topicId,
          attachment.getUuid(),
          attachment.getName(),
          attachment.getFileExt(),
          attachment.getDisplayTime().toEpochMilli());
    }
  }

  private void addTopicAttachmentAppField(String attachmentId, List<String> appFieldIdList) {
    Optional.ofNullable(appFieldIdList)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(list -> topicDao.insertTopicAttachmentAppField(attachmentId, list));
  }

  private void updateTopicAttachmentAppField(String attachmentId, List<String> appFieldIdList) {
    List<String> originalIdList =
        getAttachmentAppField(attachmentId, AcceptLanguage.getLanguageForDb())
            .stream()
            .map(item -> item.getId().toString())
            .collect(Collectors.toList());
    if (!originalIdList.equals(appFieldIdList)) {
      topicDao.deleteTopicAttachmentAppField(attachmentId);
      addTopicAttachmentAppField(attachmentId, appFieldIdList);
    }
  }

  private void sendNotification(
      TopicInfo topicInfo,
      ForumInfo forumInfo,
      String title,
      String text,
      NotificationType notificationType,
      List<String> recipient,
      List<SimpleGroupWithUsers> orgMembers,
      List<SimpleGroupWithUsers> bgbus) {
    recipient = forumService.validateForumMembers(forumInfo, recipient);
    addNotificationOfTopic(
        topicInfo.getForumId(), topicInfo.getTopicId(), notificationType, recipient);
    List<String> orgRecipient =
        getAndUpdateOrgRecipient(forumInfo.getForumType(), topicInfo.getTopicId(), orgMembers);
    List<String> bgbuRecipient =
        getAndUpdateBgbuRecipient(forumInfo.getForumType(), topicInfo.getTopicId(), bgbus);
    if (notificationType != null) {
      List<String> users =
          Stream.of(recipient, orgRecipient, bgbuRecipient)
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList());
      EmailWithChineseAndEnglishContext context =
          getNotificationContext(
              topicInfo.getTopicId(), forumInfo, title, text, notificationType, users);
      eventPublishService.publishEmailSendingEvent(context);
      eventPublishService.publishNotificationSendingEvent(
          getNotification(topicInfo, forumInfo, title, notificationType, users));
    }
  }

  private List<String> getAndUpdateOrgRecipient(
      String forumType, int topicId, List<SimpleGroupWithUsers> orgMembers) {
    topicDao.deleteOrgMemberNotificationOfTopic(topicId);
    if (!forumService.isPublicForum(forumType)) {
      return Collections.emptyList();
    }

    List<String> orgRecipient = groupRecipientHandleService.getOrgRecipient(orgMembers);
    Optional.ofNullable(orgMembers)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(
            list ->
                list.forEach(
                    item -> {
                      final String orgId = item.getValue();
                      List<String> members =
                          Optional.ofNullable(item.getUsers())
                              .orElseGet(ArrayList::new)
                              .parallelStream()
                              .map(User::getId)
                              .filter(StringUtils::isNoneBlank)
                              .collect(Collectors.toList());
                      if (CollectionUtils.isEmpty(members)) {
                        topicDao.upsertOrgMemberNotificationOfTopic(
                            topicId, orgId, StringUtils.EMPTY);
                      } else {
                        topicDao.upsertOrgMemberNotificationOfTopic(
                            topicId,
                            orgId,
                            members
                                .parallelStream()
                                .collect(Collectors.joining(Constants.COMMA_DELIMITER)));
                      }
                    }));
    return orgRecipient;
  }

  private List<String> getAndUpdateBgbuRecipient(
      String forumType, int topicId, List<SimpleGroupWithUsers> bgbus) {
    topicDao.deleteBgbuNotificationOfTopic(topicId);
    if (!forumService.isPublicForum(forumType)) {
      return Collections.emptyList();
    }

    List<String> bgbuRecipient = groupRecipientHandleService.getOrgRecipient(bgbus);
    Optional.ofNullable(bgbus)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(
            list ->
                list.forEach(
                    item -> {
                      final String orgId = item.getValue();
                      List<String> members =
                          Optional.ofNullable(item.getUsers())
                              .orElseGet(ArrayList::new)
                              .parallelStream()
                              .map(User::getId)
                              .filter(StringUtils::isNoneBlank)
                              .collect(Collectors.toList());
                      if (CollectionUtils.isEmpty(members)) {
                        topicDao.upsertBgbuNotificationOfTopic(topicId, orgId, StringUtils.EMPTY);
                      } else {
                        topicDao.upsertBgbuNotificationOfTopic(
                            topicId,
                            orgId,
                            members
                                .parallelStream()
                                .collect(Collectors.joining(Constants.COMMA_DELIMITER)));
                      }
                    }));
    return bgbuRecipient;
  }

  protected List<SimpleGroupWithUsers> getOrgMembers(int topicId) {
    List<OrgIdWithUsers> orgMemberList = topicDao.getOrgMemberNotificationOfTopic(topicId);
    return convertToSimpleGroupWithUsers(orgMemberList);
  }

  protected List<SimpleGroupWithUsers> getBgbus(int topicId) {
    List<OrgIdWithUsers> getBgbus = topicDao.getBgbuNotificationOfTopic(topicId);
    return convertToSimpleGroupWithUsers(getBgbus);
  }

  private List<SimpleGroupWithUsers> convertToSimpleGroupWithUsers(List<OrgIdWithUsers> orgList) {
    return Optional.ofNullable(orgList)
        .orElseGet(ArrayList::new)
        .stream()
        .map(this::convertToSimpleGroupWithUsers)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private SimpleGroupWithUsers convertToSimpleGroupWithUsers(OrgIdWithUsers org) {
    OrgGroup orgGroup = userGroupAdapter.getOrgGroup(org.getOrgId());
    if (Objects.isNull(orgGroup)) {
      return null;
    }

    SimpleGroupWithUsers result = new SimpleGroupWithUsers();
    result.setOrgGroupType(orgGroup.getType().name());
    result.setLabel(orgGroupExtensionService.getOrgName(orgGroup));
    List<User> users =
        userService.getUserByIds(
            Arrays.stream(org.getUsers().split(Constants.COMMA_DELIMITER))
                .filter(Objects::nonNull)
                .filter(uid -> !uid.isEmpty())
                .distinct()
                .collect(Collectors.toList()));
    result.setUsers(users);
    result.setValue(org.getOrgId());
    return result;
  }

  private void addNotificationOfTopic(
      int forumId, int topicId, NotificationType notificationType, List<String> recipient) {
    if (NotificationType.ALL.equals(notificationType)) {
      /*
      List<String> userIdList =
          forumService
              .getMemberOfForum(forumId, -1, -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
       */
      topicDao.addNotificationOfTopic(
          topicId,
          notificationType.toString(),
          //userIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)) -Caused by: java.sql.SQLDataException: (conn=14550200) Data too long for column 'recipient'
          notificationType.toString());
    } else if (NotificationType.CUSTOM.equals(notificationType)) {
      topicDao.addNotificationOfTopic(
          topicId,
          notificationType.toString(),
          recipient.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)));
    } else {
      topicDao.addNotificationOfTopic(topicId, "", "");
    }
  }

  private EmailWithChineseAndEnglishContext getNotificationContext(
      int topicId,
      ForumInfo forumInfo,
      String title,
      String text,
      NotificationType notificationType,
      List<String> recipient) {
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link = String.format(EmailConstants.TOPIC_URI_FORMAT, host, language, topicId);
    String mobileLink =
        String.format(
            EmailConstants.TOPIC_URI_FORMAT, yamlConfig.getMobileDownloadUrl(), language, topicId);
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.TOPICNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationType)) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .forumInfo(forumInfo)
        .sender(userName)
        .desc(String.format(EmailConstants.TOPIC_NOTIFICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.TOPIC_NOTIFICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + title)
        .content(Utility.getTextFromHtml(text))
        .to(
            userService.getEmailByUserId(
                getRecipientList(forumInfo.getForumId(), notificationType, recipient)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_TOPIC, title),
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName())));
  }

  private List<String> getRecipientList(
      int forumId, NotificationType notificationType, List<String> recipient) {
    List<String> userIdList;
    if (NotificationType.ALL.equals(notificationType)) {
      userIdList = Arrays.asList("");
      /*
      userIdList =
          forumService
              .getMemberOfForum(forumId, -1, -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
       */
    } else {
      userIdList = recipient;
    }
    return userIdList;
  }

  public List<String> getAttachmentIdOfTopic(int topicId) {
    return topicDao.getAttachmentIdOfTopic(topicId);
  }

  public Identity getUserIdentityOfTopic(String userId, Identity identity, String authorId) {
    if (Identity.OWNER.equals(identity)) {
      return identity;
    } else if (Identity.MEMBER.equals(identity)) {
      return Utility.checkUserIsAuthor(authorId, userId) ? Identity.AUTHOR : identity;
    } else {
      return identity;
    }
  }

  public int updateTopic(int topicId, TopicUpdatedData topicData) {
    defaultTopicData(topicData);
    validateAppField(topicData.getAppField(), topicData.getAttachment());
    TopicInfo topicInfo = getTopicInfoById(topicId);
    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    if (topicInfo.getTopicModifiedTime() != topicData.getModifiedTime().longValue()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }
    if (!topicInfo.getTopicTitle().equals(topicData.getTitle())
        && checkDuplicateTopic(
            topicInfo.getForumId(), topicInfo.getTopicTitle(), topicData.getTitle())) {
      throw new DuplicationException(I18nConstants.MSG_DUPLICATE_TITLE);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!isSupportTopicType(forumInfo.getSupportTopicType(), topicData.getType())) {
      throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_NOT_SUPPORT);
    }
    long attachmentTotalSize = getAttachmentTotalSize(topicData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    String userId = Utility.getUserIdFromSession();
    if (!checkCurrentUserCanUpdate(topicInfo.getTopicCreateUserId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_UPDATE_TOPIC);
    }
    String originalText = topicInfo.getTopicText();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());

    setUpdatedTopicInfo(
        topicInfo,
        topicData,
        ForumType.fromValue(forumInfo.getForumType()),
        forumAdminRoleList,
        forumMemberRoleList);
    if (drcSyncConfig.getCommunityId().contains(forumInfo.getCommunityId()) &&
            forumInfo.isPublicForum()) {
      eventPublishService.publishDrcSyncEvent(
              drcSyncConfig.getDatabase(),
              DrcSyncType.UPDATE,
              topicInfo.getTopicId(),
              topicInfo.getTopicTitle(),
              topicInfo.getTopicText(),
              forumInfo.getCommunityId(),
              forumInfo.getForumId());
    }
    if (updateTopicInfo(topicInfo)) {
      updateTag(topicId, topicData.getTag());
      updateTopicAppField(
          topicId,
          topicData
              .getAppField()
              .stream()
              .map(item -> item.getValue().toString())
              .collect(Collectors.toList()));
      updateAttachment(topicId, topicData.getAttachment(), forumInfo.getForumType());
      topicData
          .getAttachment()
          .forEach(
              item ->
                  eventPublishService.publishFileUploadingEvent(
                      item.getId(),
                      item.getAuthor(),
                      forumInfo.getCommunityId(),
                      forumInfo.getForumId(),
                      ForumType.fromValue(forumInfo.getForumType()),
                      true,
                      item.getVideoLanguage()));
      eventPublishService.publishTopicChangingEvent(
          topicInfo.getTopicId(),
          forumInfo.getForumId(),
          forumInfo.getCommunityId(),
          topicInfo.getTopicModifiedUserId(),
          topicInfo.getTopicModifiedTime());
      richTextHandlerService.deleteRemovedImageInRichText(originalText, topicInfo.getTopicText(), topicId);
      sendNotification(
          topicInfo,
          forumInfo,
          topicInfo.getTopicTitle(),
          topicInfo.getTopicText(),
          topicData.getNotificationType(),
          topicData.getRecipient(),
          topicData.getOrgMembers(),
          topicData.getBgbus());
      publishActivityLogEventFromTopic(
          userId,
          forumInfo.getForumType(),
          topicId,
          Operation.UPDATE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);
      eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(topicId);
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_NOT_FOUND;
    }
  }

  private boolean checkCurrentUserCanUpdate(
      String authorId, ForumInfo forumInfo) {
    return checkCurrentUserOperation(authorId, forumInfo, Operation.UPDATE);
  }

  private boolean checkCurrentUserOperation(
      String authorId, ForumInfo forumInfo, Operation operation) {
    if (checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, operation)) {
      return true;
    } else if (ForumType.PUBLIC.equals(ForumType.fromValue(forumInfo.getForumType()))
        && CommunityType.ACTIVITY.equals(
            CommunityType.fromValue(
                communityService
                    .getCommunityInfoById(forumInfo.getCommunityId())
                    .getCommunityType()))) {
      return Utility.checkUserIsAuthor(authorId, Utility.getUserIdFromSession());
    } else {
      List<User> userInForumMember = forumService.getMemberOfForumWithFilters(
              false, forumInfo.getForumId(), -1, -1, EMPTY,
              Collections.singletonList(Utility.getUserIdFromSession()),
              null, EMPTY);
      if (userInForumMember.isEmpty()) {
        return false;
      }
      return Utility.checkUserIsAuthor(authorId, Utility.getUserIdFromSession());
    }
  }

  private TopicInfo setUpdatedTopicInfo(
      TopicInfo topicInfo,
      TopicUpdatedData topicData,
      ForumType forumType,
      List<String> forumAdminList,
      List<String> forumMemberList) {
    final String originalText = topicInfo.getTopicText();
    topicInfo.setTopicTitle(topicData.getTitle());
    topicInfo.setTopicType(topicData.getType().toString());
    topicInfo.setTopicModifiedUserId(Utility.getUserIdFromSession());
    topicInfo.setTopicModifiedTime(new Date().getTime());
    topicInfo.setTopicText(richTextHandlerService.removeCacheParameter(topicData.getText()));
    return setTopicInfoWithText(
        topicInfo, forumType, forumAdminList, forumMemberList, originalText);
  }

  private boolean updateTopicInfo(TopicInfo topicInfo) {
    boolean updateInfo = topicDao.updateInfo(topicInfo) != 0;
    boolean updateText = topicDao.updateText(topicInfo) != 0;
    return updateInfo && updateText;
  }

  private void updateTag(int topicId, List<String> newTag) {
    List<String> originalTag =
        getTagOfTopic(topicId).stream().map(Tag::getLabel).collect(Collectors.toList());
    if (!originalTag.equals(newTag)) {
      topicDao.deleteTagOfTopic(topicId);
      addTagOfTopic(topicId, newTag);
    }
  }

  private void updateTopicAppField(int topicId, List<String> appFieldIdList) {
    List<String> originalIdList =
        getTopicAppField(topicId)
            .stream()
            .map(item -> item.getId().toString())
            .collect(Collectors.toList());
    if (!originalIdList.equals(appFieldIdList)) {
      topicDao.deleteTopicAppField(topicId);
      addTopicAppField(topicId, appFieldIdList);
    }
  }

  private void updateAttachment(
      int topicId, List<AttachmentWithAuthor> newAttachment, String forumType) {
    List<String> originalAttachment = getAttachmentIdOfTopic(topicId);
    List<String> newAttachmentIdList =
        newAttachment.stream().map(AttachmentWithAuthor::getId).collect(Collectors.toList());

    String userId = Utility.getUserIdFromSession();
    Long time = Instant.now().toEpochMilli();
    originalAttachment
        .stream()
        .filter(item -> !newAttachmentIdList.contains(item))
        .forEach(
            item -> {
              topicDao.deleteAttachmentOfTopic(topicId, item, userId, time);
              topicDao.deleteTopicAttachmentAppField(item);
              fileService.delete(item, DdfType.FILE.toString(), topicId);
              publishActivityLogEventFromTopic(
                  userId,
                  forumType,
                  topicId,
                  Operation.DELETE,
                  Constants.INFO_PROJECT_NAME,
                  Constants.CONTENT_EMPTY,
                  item);
            });

    newAttachment
        .stream()
        .filter(item -> !originalAttachment.contains(item.getId()))
        .forEach(
            item -> {
              addAttachmentOfTopic(topicId, item.getId());
              addTopicAttachmentAppField(
                  item.getId(),
                  item.getAppField()
                      .stream()
                      .map(appField -> appField.getValue().toString())
                      .collect(Collectors.toList()));
              publishActivityLogEventFromTopic(
                  userId,
                  forumType,
                  topicId,
                  Operation.UPLOAD,
                  Constants.INFO_PROJECT_NAME,
                  Constants.CONTENT_EMPTY,
                  item.getId());
            });

    newAttachment
        .stream()
        .filter(item -> originalAttachment.contains(item.getId()))
        .forEach(
            item ->
                updateTopicAttachmentAppField(
                    item.getId(),
                    item.getAppField()
                        .stream()
                        .map(appField -> appField.getValue().toString())
                        .collect(Collectors.toList())));
  }

  public boolean updateTopicState(int topicId, TopicState state) {
    TopicInfo topicInfo = new TopicInfo();
    topicInfo.setTopicId(topicId);
    topicInfo.setTopicState(state.toString());
    return topicDao.updateState(topicInfo) != 0;
  }

  public List<Tag> getTagOfTopic(int topicId) {
    return topicDao.getTagOfTopic(topicId);
  }

  public List<IdNameEntity> getTopicAppField(int topicId) {
    return getTopicAppField(topicId, AcceptLanguage.getLanguageForDb());
  }

  public List<IdNameEntity> getTopicAppField(int topicId, String lang) {
    return topicDao.getTopicAppField(topicId, lang);
  }

  public void deleteTopic(int topicId, boolean deleteForum) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation()) && !deleteForum) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!checkCurrentUserCanDelete(topicInfo.getTopicCreateUserId(), forumInfo)
        && !deleteForum) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_DELETE_TOPIC);
    }
    long now = new Date().getTime();
    String userId = Utility.getUserIdFromSession();
    eventPublishService.publishTopicDeletingEvent(
        forumInfo.getCommunityId(), topicInfo.getForumId(), topicId, userId, now);
    getAttachmentIdOfTopic(topicId)
        .forEach(
            item -> {
              publishActivityLogEventFromTopic(
                  userId,
                  forumInfo.getForumType(),
                  topicId,
                  Operation.DELETE,
                  Constants.INFO_PROJECT_NAME,
                  Constants.CONTENT_EMPTY,
                  item);
              fileService.delete(item, DdfType.FILE.toString(), topicId);
              topicDao.deleteAttachmentOfTopic(topicId, item, userId, now);
            });
    richTextHandlerService.deleteRemovedImageInRichText(topicInfo.getTopicText(), "", topicId);
    setDeleteTopicInfo(topicInfo, userId, now);
    topicDao.delete(topicInfo);
    if (!topicInfo.getTopicDdfId().isEmpty()) {
      fileService.delete(topicInfo.getTopicDdfId(), DdfType.TOPIC.toString(), topicId);
    }
    if (topicInfo.getTopicToppingOrder() != DEFAULT_TOPPING_ORDER) {
      topicDao.unToppingTopic(topicId);
    }
    if (drcSyncConfig.getCommunityId().contains(forumInfo.getCommunityId()) &&
            forumInfo.isPublicForum()) {
      eventPublishService.publishDrcSyncEvent(
              drcSyncConfig.getDatabase(),
              DrcSyncType.DELETE,
              topicInfo.getTopicId(),
              topicInfo.getTopicTitle(),
              topicInfo.getTopicText(),
              forumInfo.getCommunityId(),
              forumInfo.getForumId());
    }
    publishActivityLogEventFromTopic(
        userId,
        forumInfo.getForumType(),
        topicId,
        Operation.DELETE,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(topicId);
  }

  private boolean checkCurrentUserCanDelete(String authorId, ForumInfo forumInfo) {
    return checkCurrentUserOperation(authorId, forumInfo, Operation.DELETE);
  }

  private void setDeleteTopicInfo(TopicInfo topicInfo, String userId, long time) {
    topicInfo.setTopicDeleteUserId(userId);
    topicInfo.setTopicDeleteTime(time);
    topicInfo.setTopicStatus(TopicStatus.DELETE.toString());
    topicInfo.setTopicLastModifiedUserId(userId);
    topicInfo.setTopicLastModifiedTime(time);
  }

  public boolean updateLastModifiedOfTopic(int topicId, String userId, long time) {
    return topicDao.updateLastModifiedOfTopic(topicId, userId, time) != 0;
  }

  public List<LatestTopic> getLatestTopicOfAllCommunity(int offset, int limit) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    return topicDao.getLatestTopicOfAllCommunity(
        userService.isSysAdmin(), Utility.getCurrentUserIdWithGroupId(), offset, limit, dlInfo.isDL, dlInfo.allowForumId);
  }

  public AttachmentSearchResult getOwnAttachmentOfCommunity(
      int communityId, int offset, int limit, Order sort, String fileExt) {
    boolean isSysAdmin = userService.isSysAdmin();
    String userId = Utility.getCurrentUserIdWithGroupId();
    List<String> attachmentList =
        fileService.getOwnAttachmentOfCommunity(
            communityId,
            isSysAdmin,
            userId,
            offset,
            limit,
            getSortFieldOfAttachment(sort.getProperty()),
            sort.getDirection().toString(),
            fileExt);
    List<AttachmentListDetail> sortedAttachmentList =
        attachmentList
            .stream()
            .map(
                item ->
                    transferAttachmentIntoAttachmentListDetail(
                        fileService.getAttachmentDetail(item)))
            .collect(Collectors.toList());
    if (!attachmentList.isEmpty()) {
      Map<String, String> dataUrlMap =
          fileService
              .getFileBaseSection(attachmentList)
              .entrySet()
              .parallelStream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      item -> StringUtils.defaultString(item.getValue().getDataUrl())));
      sortedAttachmentList =
          sortedAttachmentList
              .parallelStream()
              .map(item -> item.refUrl(dataUrlMap.getOrDefault(item.getId(), "")))
              .collect(Collectors.toList());
    }
    int numFound =
        fileService.countOwnAttachmentOfCommunity(communityId, isSysAdmin, userId, fileExt);
    return new AttachmentSearchResult().result(sortedAttachmentList).numFound(numFound);
  }

  private String getSortFieldOfAttachment(String sortField) {
    if (SortField.UPDATETIME.toString().equals(sortField)) {
      return AttachmentFieldName.USER_CREATED_TIME.toString();
    } else if (SortField.NAME.toString().equals(sortField)) {
      return AttachmentFieldName.FILE_NAME.toString();
    } else if (SortField.TYPE.toString().equals(sortField)) {
      return ORDER_NULL_LAST + AttachmentFieldName.EXT_ORDER.toString();
    } else {
      return AttachmentFieldName.USER_CREATED_TIME.toString();
    }
  }

  private AttachmentListDetail transferAttachmentIntoAttachmentListDetail(
      AttachmentDetail attachment) {
    FileIcon icon =
        attachment.getFileIcon().isEmpty()
            ? FileIcon.OTHER
            : FileIcon.fromValue(attachment.getFileIcon());
    return new AttachmentListDetail()
        .id(attachment.getId())
        .name(attachment.getFileName())
        .fileExt(attachment.getFileExt())
        .author(fileService.getAttachmentAuthor(attachment.getId()))
        .modifiedTime(attachment.getModifiedTime())
        .belongForum(new IdNameDto().id(attachment.getForumId()).name(attachment.getForumName()))
        .belongTopic(new IdNameDto().id(attachment.getTopicId()).name(attachment.getTopicTitle()))
        .icon(icon)
        .refUrl(attachment.getRefUrl());
  }

  public void publishActivityLogEventFromTopic(
      String userId,
      String forumType,
      int topicId,
      Operation operation,
      String origin,
      String content,
      String attachmentId) {
    PermissionObject permissionObject =
        !ForumType.PRIVATE.equals(ForumType.fromValue(forumType))
            ? PermissionObject.PUBLICFORUMTOPIC
            : PermissionObject.PRIVATEFORUMTOPIC;

    eventPublishService.publishActivityLogEvent(
        Utility.setActivityLogData(
            userId,
            operation.toString(),
            permissionObject.toString(),
            topicId,
            origin,
            content,
            attachmentId));

    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            ActivityLogUtil.getAppName(origin),
            Constants.ACTIVITY_APP_VERSION,
            userId,
            ActivityLogUtil.getOperationEnumOfActivityLog(operation),
            ActivityLogUtil.getObjectType(ObjectType.TOPICID, attachmentId),
            ActivityLogUtil.getObjectId(topicId, attachmentId),
            ActivityLogUtil.getAnnotation(permissionObject, content),
            LogStatus.SUCCESS,
            LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
            ObjectType.FORUMID,
            null));
  }

  public int createTopicOfPqm(String account, TopicPqmData topicPqmData) {
    defaultRecipient(topicPqmData);
    ForumInfo forumInfo = forumService.getForumInfoById(topicPqmData.getForumId());
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    UserSession user = userService.getUserInfo(account);
    String userId = user.getCommonUUID();
    List<String> groupIds = user.getGroup();
    if (!checkCurrentUserCanCreateWithGroupInfo(forumInfo, userId, groupIds)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_CREATE_TOPIC);
    }

    TopicPqmBean topicPqmBean = getBeanOfTopicOfPqm(topicPqmData.getText());
    String title = getTitleOfTopicOfPqm(topicPqmBean);
    TopicInfo topicInfo =
        convertToTopicInfo(topicPqmData, userId, TopicType.SYSTEM.toString(), title);
    if (!addTopic(topicInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_TOPIC_FAILED);
    }

    int topicId = topicInfo.getTopicId();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());
    setTopicInfoWithText(
        topicInfo,
        ForumType.fromValue(forumInfo.getForumType()),
        forumAdminRoleList,
        forumMemberRoleList,
        "");
    if (!addTextOfTopic(topicInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_TOPIC_FAILED);
    }

    addTagOfTopic(topicId, topicPqmData.getTag());
    eventPublishService.publishTopicChangingEvent(
        topicInfo.getTopicId(),
        forumInfo.getForumId(),
        forumInfo.getCommunityId(),
        topicInfo.getTopicCreateUserId(),
        topicInfo.getTopicCreateTime());

    String textOfHtml = getTopicOfPqmForHtmlFormat(topicPqmBean);
    sendNotification(
        topicInfo,
        forumInfo,
        topicInfo.getTopicTitle(),
        textOfHtml,
        topicPqmData.getNotificationType(),
        topicPqmData.getRecipient(),
        topicPqmData.getOrgMembers(),
        topicPqmData.getBgbus());
    publishActivityLogEventFromTopic(
        userId,
        forumInfo.getForumType(),
        topicId,
        Operation.CREATE,
        Constants.INFO_NAME_PQM,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    return topicId;
  }

  public TopicPqmBean getBeanOfTopicOfPqm(String text) {
    TopicPqmBean topicPqmBean = new TopicPqmBean();
    try {
      topicPqmBean = mapper.readValue(text, TopicPqmBean.class);
    } catch (IOException e) {
      log.info(e);
    }
    return topicPqmBean;
  }

  public String getTitleOfTopicOfPqm(TopicPqmBean bean) {
    return Constants.TITLE_TOPIC_PQM_NAME
        + " "
        + bean.getWorkstation()
        + " "
        + bean.getAbnormalcyDescription();
  }

  private String getTopicOfPqmForHtmlFormat(TopicPqmBean bean) {
    StringBuilder sb = new StringBuilder();
    sb.append(EmailConstants.PQM_FACTORY_AREA + ":" + bean.getFactoryArea() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_PRODUCING_ZONE + ":" + bean.getProducingZone() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_TRIGGERED_DATE + ":" + bean.getTriggeredDate() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_PRODUCTION_LINE + ":" + bean.getProductionLine() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_WORK_STATION + ":" + bean.getWorkstation() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_EQUIPMENT_MODEL + ":" + bean.getEquipmentModel() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_CURRENT_STATE + ":" + bean.getCurrentState() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_MODEL + ":" + bean.getModel() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_DIMENSION + ":" + bean.getDimension() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_DATA + ":" + bean.getData() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_ERROR_CODE + ":" + bean.getErrorCode() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_CLASSIFICATION + ":" + bean.getClassification() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_ABNORMALCY_DESCRIPTION
            + ":"
            + bean.getAbnormalcyDescription()
            + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_ABNORMALCY_COUNT + ":" + bean.getAbnormalcyCount() + Constants.HTML_BR);
    sb.append(
        EmailConstants.PQM_ABNORMALCY_TIME + ":" + bean.getAbnormalcyTime() + Constants.HTML_BR);
    sb.append(EmailConstants.PQM_DESCRIPTION + ":" + bean.getDescription() + Constants.HTML_BR);
    return sb.toString();
  }

  private boolean checkCurrentUserCanCreateWithGroupInfo(
      ForumInfo forumInfo, String userId, List<String> groupIds) {
    return checkUserPermission(
        Utility.getUserIdWithGroupInfo(userId, groupIds), forumInfo, Operation.CREATE);
  }

  public void lockTopic(int topicId, String userId, long lockedTime) {
    topicDao.lockTopicAndItsReplies(topicId, userId, lockedTime);
  }

  public EmojiResult getEmojiDetailOfTopic(
      int topicId, Emoji emoji, int offset, int limit, Order sort) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!checkCurrentUserCanRead(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_PRIVATE_FORUM);
    }
    return new EmojiResult()
        .numFound(
            emojiService
                .transferEmojiMap(emojiService.getEmojiOfTopic(topicId))
                .get(emoji.toString()))
        .result(
            emojiService.getEmojiDetailOfTopic(topicId, emoji, offset, limit, sort.getDirection()));
  }

  public int setEmojiOfTopic(int topicId, Emoji emoji) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    int row = emojiService.setEmojiOfTopic(topicId, emoji);

    try {
      UserEchoAPI.set(
          Utility.getUserIdFromSession(), Echo.LIKE, ObjectType.TOPICID, String.valueOf(topicId));
    } catch (UserEchoException e) {
      log.warn(e);
    }
    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            App.COMMUNITY,
            Constants.ACTIVITY_APP_VERSION,
            Utility.getUserIdFromSession(),
            Activity.LIKE,
            ObjectType.TOPICID,
            String.valueOf(topicId),
            null,
            null,
            null,
            ObjectType.FORUMID,
            String.valueOf(topicInfo.getForumId())));

    return checkResultAndReturnHttpStatus(row > 0);
  }

  public int removeEmojiOfTopic(int topicId) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    int row = emojiService.removeEmojiOfTopic(topicId);

    try {
      UserEchoAPI.remove(
          Utility.getUserIdFromSession(),
          EchoGroup.EMOTION,
          ObjectType.TOPICID,
          String.valueOf(topicId));
    } catch (UserEchoException e) {
      log.warn(e);
    }
    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            App.COMMUNITY,
            Constants.ACTIVITY_APP_VERSION,
            Utility.getUserIdFromSession(),
            Activity.UNLIKE,
            ObjectType.TOPICID,
            String.valueOf(topicId),
            null,
            null,
            null,
            ObjectType.FORUMID,
            String.valueOf(topicInfo.getForumId())));

    return checkResultAndReturnHttpStatus(row > 0);
  }

  public TopicResultOfBookmark searchTopicListOfUser(
      String userId, int offset, int limit, Order sort) {
    List<TopicListDetailOfBookmark> topicListDetailList =
        getTopicByUserId(userId, offset, limit, sort);
    int numFound = countTopicOfUser(userId);
    return new TopicResultOfBookmark().result(topicListDetailList).numFound(numFound);
  }

  public List<TopicListDetailOfBookmark> getTopicByUserId(
      String userId, int offset, int limit, Order sort) {
    List<TopicInformationOfBookmark> topicInformationOfBookmarks =
        topicDao.getTopicOfUserWithSortAndLimit(
            userId,
            offset,
            limit,
            getSortField(sort.getProperty()).toString(),
            sort.getDirection().toString());
    return topicInformationOfBookmarks
        .stream()
        .map(
            item ->
                new TopicListDetailOfBookmark()
                    .id(item.getTopicId())
                    .title(item.getTopicTitle())
                    .text(item.getTopicText())
                    .type(TopicType.fromValue(item.getTopicType()))
                    .state(TopicState.fromValue(item.getTopicState()))
                    .status(TopicStatus.fromValue(item.getTopicStatus()))
                    .createUser(userService.getUserById(item.getTopicCreateUserId()))
                    .lastModifiedTime(item.getTopicLastModifiedTime())
                    .lastModifiedUser(userService.getUserById(item.getTopicLastModifiedUserId()))
                    .belongCommunity(
                        new IdNameDto().id(item.getCommunityId()).name(item.getCommunityName()))
                    .belongForum(new IdNameDto().id(item.getForumId())))
        .collect(Collectors.toList());
  }

  public int countTopicOfUser(String userId) {
    return topicDao.countTopicOfUser(userId);
  }

  public boolean checkUserBookmark(String userId, int topicId, ForumInfo forumInfo) {
    return bookmarkService.checkUserBookmark(
        userId, getPermissionObjectOfTopic(forumInfo), topicId);
  }

  public int sealOrUnsealTopic(int topicId, boolean seal) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!checkCurrentUserCanUpdate(topicInfo.getTopicCreateUserId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_SEAL_TOPIC);
    }
    eventPublishService.publishTopicChangingEvent(
        topicId,
        forumInfo.getForumId(),
        forumInfo.getCommunityId(),
        Utility.getUserIdFromSession(),
        topicInfo.getTopicLastModifiedTime());
    int returnState =
        checkResultAndReturnHttpStatus(seal ? sealTopic(topicId) : unsealTopic(topicId));
    if (HttpStatus.SC_OK == returnState) {
      eventPublishService.publishActivityLogMsgEvent(
          ActivityLogUtil.convertToActivityLogMsg(
              App.COMMUNITY,
              Constants.ACTIVITY_APP_VERSION,
              Utility.getUserIdFromSession(),
              seal ? Activity.LOCK : Activity.UNLOCK,
              ObjectType.TOPICID,
              String.valueOf(topicId),
              null,
              LogStatus.SUCCESS,
              LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
              ObjectType.FORUMID,
              String.valueOf(forumInfo.getForumId())));
    }
    return returnState;
  }

  private int checkResultAndReturnHttpStatus(boolean result) {
    if (result) {
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }

  public boolean sealTopic(int topicId) {
    return topicDao.setTopicSituation(topicId, TopicSituation.SEALED.toString()) != 0;
  }

  public boolean unsealTopic(int topicId) {
    return topicDao.setTopicSituation(topicId, TopicSituation.NORMAL.toString()) != 0;
  }

  private Notification getNotification(
      TopicInfo topicInfo,
      ForumInfo forumInfo,
      String title,
      NotificationType notificationType,
      List<String> recipient) {
    return new Notification()
        .userId(
            getRecipientList(forumInfo.getForumId(), notificationType, recipient)
                .stream()
                .collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.TOPICNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationType)) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .title(title)
        .senderId(Utility.getUserIdFromSession())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .topicId(topicInfo.getTopicId())
        .topicTitle(topicInfo.getTopicTitle())
        .topicType(TopicType.fromValue(topicInfo.getTopicType()))
        .time(new Date().getTime());
  }

  public List<ParticipatedTopic> getParticipatedTopicOfUser(String userId, int offset, int limit) {
    List<ParticipatedTopic> topicList = topicDao.getParticipatedTopicOfUser(userId, offset, limit);
    String host = yamlConfig.getHost();
    return topicList
        .stream()
        .map(item -> setParticipatedTopic(item, host))
        .collect(Collectors.toList());
  }

  private ParticipatedTopic setParticipatedTopic(ParticipatedTopic participatedTopic, String host) {
    String content = Jsoup.parse(participatedTopic.getContent()).text();
    return participatedTopic
        .content(StringUtils.substring(content, 0, CONTENT_LENGTH))
        .refUrl(
            String.format(
                NotificationConstants.TOPIC_URI_FORMAT,
                host,
                AcceptLanguage.get(),
                participatedTopic.getId()));
  }

  public String updatePinOfTopic(int topicId, PinEnum pin) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!checkCurrentUserCanPin(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_NOT_AUTHORIZED);
    }
    if (PinEnum.PIN.equals(pin)) {
      int toppingNumber = topicDao.countToppingTopicOfForum(forumInfo.getForumId());
      if (toppingNumber >= MAX_TOPPING_LIMIT) {
        throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_SET_IMPORTANT_MAX_LIMITED);
      }
      if (topicInfo.getTopicToppingOrder() != DEFAULT_TOPPING_ORDER) {
        throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_SET_IMPORTNAT_ALREADY);
      }
      if (topicDao.toppingTopic(topicId) != 1) {
        throw new CommunityException(I18nConstants.MSG_COMMON_ACTION_FAILED);
      } else {
        return I18nConstants.MSG_TOPIC_SET_IMPORTANT_SUCCESS;
      }
    } else {
      if (topicInfo.getTopicToppingOrder() == DEFAULT_TOPPING_ORDER) {
        throw new IllegalArgumentException(I18nConstants.MSG_TOPIC_SET_GENERAL_ALREADY);
      }
      topicDao.unToppingTopic(topicId);
      return I18nConstants.MSG_TOPIC_SET_GENERAL_SUCCESS;
    }
  }

  private boolean checkCurrentUserCanPin(ForumInfo forumInfo) {
    return checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, Operation.PIN);
  }

  public void updateToppingOrderOfTopic(int topicId, int toppingOrder) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    ForumInfo forumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    if (!checkCurrentUserCanPin(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_NOT_AUTHORIZED);
    }
    if (topicInfo.getTopicToppingOrder() != toppingOrder) {
      if (toppingOrder > MAX_TOPPING_LIMIT
          || toppingOrder <= DEFAULT_TOPPING_ORDER
          || toppingOrder > topicDao.countToppingTopicOfForum(forumInfo.getForumId())) {
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
      }
      if (topicDao.swapToppingOrderOfTopic(topicId, toppingOrder) != 1) {
        throw new CommunityException(I18nConstants.MSG_COMMON_ACTION_FAILED);
      }
    }
  }

  public TopicSearchResult searchHotTopicList(int communityId, int offset, int limit) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    List<TopicInfo> hotTopicList =
        topicDao.getHotPublicTopicOfCommunity(communityId, offset, limit, dlInfo.isDL, dlInfo.allowForumId,
                yamlConfig.getHotLastingMin() * MINUTE_TO_SECOND_MULTIPLY);
    Map<Integer, ForumInfo> forumInfoMap = new HashMap<>();
    hotTopicList
        .stream()
        .map(TopicInfo::getForumId)
        .distinct()
        .forEach(item -> forumInfoMap.put(item, forumService.getForumInfoById(item)));
    Map<Integer, Identity> forumIdentityMap = new HashMap<>();
    hotTopicList
        .stream()
        .map(TopicInfo::getForumId)
        .distinct()
        .forEach(
            item ->
                forumIdentityMap.put(item, getUserIdentityOfForum(forumInfoMap.get(item), false)));
    List<TopicListDetail> hotTopicDetailList =
        hotTopicList
            .stream()
            .map(
                item ->
                    transferTopicInfoToTopicListDetail(item)
                        .belongForum(
                            new IdNameDto()
                                .id(item.getForumId())
                                .name(forumInfoMap.get(item.getForumId()).getForumName()))
                        .identity(
                            getUserIdentityOfTopic(
                                Utility.getUserIdFromSession(),
                                forumIdentityMap.get(item.getForumId()),
                                item.getTopicCreateUserId())))
            .collect(Collectors.toList());
    return new TopicSearchResult().result(hotTopicDetailList).toppingResult(new ArrayList<>());
  }

  public void moveTopicToOtherForum(int topicId, ForumIdWithModifiedTime forum) {
    TopicInfo topicInfo = getTopicInfoById(topicId);
    ForumInfo originalForumInfo = forumService.getForumInfoById(topicInfo.getForumId());
    ForumInfo newForumInfo = forumService.getForumInfoById(forum.getForumId());

    if (ForumStatus.LOCKED.toString().equals(newForumInfo.getForumStatus())
        && ForumStatus.LOCKED.toString().equals(originalForumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (!checkCurrentUserCanCreate(newForumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_CREATE_TOPIC);
    }

    if (TopicStatus.LOCKED.toString().equals(topicInfo.getTopicStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    if (topicInfo.getTopicModifiedTime() != forum.getModifiedTime().longValue()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }
    if (checkDuplicateTopic(
        newForumInfo.getForumId(), StringUtils.EMPTY, topicInfo.getTopicTitle())) {
      throw new DuplicationException(I18nConstants.MSG_DUPLICATE_TITLE);
    }

    if (!checkCurrentUserCanMove(
        topicInfo.getTopicCreateUserId(), originalForumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_MOVE_TOPIC);
    }

    if (topicInfo.getTopicToppingOrder() != DEFAULT_TOPPING_ORDER) {
      topicDao.unToppingTopic(topicId);
    }
    topicDao.updateForumIdForTopicAndRepliesById(
        topicId, topicInfo.getForumId(), newForumInfo.getForumId());
    String userId = Utility.getUserIdFromSession();
    long time = new Date().getTime();
    eventPublishService.publishTopicMovingEvent(
        originalForumInfo.getForumId(), originalForumInfo.getCommunityId(), userId, time);
    eventPublishService.publishTopicChangingEvent(
        topicInfo.getTopicId(),
        newForumInfo.getForumId(),
        newForumInfo.getCommunityId(),
        userId,
        time);
    // drc robot sync logic，更新 topic 的 forum 從公開到非公開要刪除，反之新增
    // topic 只能在同一個 community 下移動
    // forumId 不會為負，-100 當作初始值，如果 publishDrcSyncEvent 收到負的forumId表示有錯誤
    if (drcSyncConfig.getCommunityId().contains(newForumInfo.getCommunityId())) {
      DrcSyncType syncType = null;
      int communityId = newForumInfo.getCommunityId();
      int forumId = ABNORMAL_FORUM_ID;

      if (originalForumInfo.getForumType().equals(Constants.PUBLIC)
              && newForumInfo.getForumType().equals(Constants.PRIVATE)) {
        syncType = DrcSyncType.DELETE;
        forumId = originalForumInfo.getForumId();
      } else if (originalForumInfo.getForumType().equals(Constants.PRIVATE)
              && newForumInfo.getForumType().equals(Constants.PUBLIC)) {
        syncType = DrcSyncType.CREATE;
        forumId = newForumInfo.getForumId();
      }

      if (syncType != null && forumId != -100) {
        eventPublishService.publishDrcSyncEvent(
                drcSyncConfig.getDatabase(),
                syncType,
                topicInfo.getTopicId(),
                topicInfo.getTopicTitle(),
                topicInfo.getTopicText(),
                communityId,
                forumId
        );
      }
    }
  }

  private boolean checkCurrentUserCanMove(
      String authorId, ForumInfo forumInfo) {
    return checkCurrentUserOperation(authorId, forumInfo, Operation.MOVE);
  }

  public List<TopicInfo> getAllByForumIdAdnStatus(int forumId, String topicStatus) {
    return topicDao.getAllByForumIdAndStatus(forumId, topicStatus);
  }

  public void reopenTopic(int topicId, String userId, long reopenedTime, String status) {
    topicDao.reopenTopicAndItsReplies(topicId, userId, reopenedTime, status);
  }

  private long getAttachmentTotalSize(List<AttachmentWithAuthor> attachmentList) {
    if (null == attachmentList) {
      return 0;
    }
    return fileService.getAttachmentTotalSize(
        attachmentList
            .parallelStream()
            .map(AttachmentWithAuthor::getId)
            .collect(Collectors.toList()),
        true);
  }

  public CommunityResultList getTopicListOfCommunity(
      int communityId,
      int offset,
      int limit,
      Order sort,
      String state,
      String type,
      Integer forumId) {
    if (!Optional.ofNullable(forumId)
        .orElseGet(() -> NumberUtils.INTEGER_ZERO)
        .equals(NumberUtils.INTEGER_ZERO)) {
      return getTopicListOfForum(forumId, offset, limit, sort, state, type, false);
    }
    List<String> stateList = getTopicStateList(state);
    boolean isSysAdmin = userService.isSysAdmin();
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    List<TopicInfo> topicInfoList =
        topicDao.getTopicOfCommunityWithSortAndLimit(
            communityId,
            offset,
            limit,
            getSortField(sort.getProperty()).toString(),
            sort.getDirection().toString(),
            isSysAdmin,
            Utility.getCurrentUserIdWithGroupId(),
            stateList,
            type,
            dlInfo.isDL,
            dlInfo.allowForumId);
    List<CommunityResultDetail> topicDataList =
        transferTopicInfoToCommunityResultDetail(topicInfoList, true);
    int numFound = countUserCanReadTopicOfCommunity(communityId, stateList, type, isSysAdmin);
    return new CommunityResultList().result(topicDataList).numFound(numFound);
  }

  private List<CommunityResultDetail> transferTopicInfoToCommunityResultDetail(
      List<TopicInfo> topicInfoList, boolean read) {
    if (topicInfoList.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Integer, ForumInfo> forumInfoMap =
        topicInfoList
            .parallelStream()
            .map(TopicInfo::getForumId)
            .distinct()
            .map(forumService::getForumInfoById)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(ForumInfo::getForumId, Function.identity()));
    Map<Integer, Identity> forumIdentityMap = new HashMap<>();
    forumInfoMap
        .values()
        .forEach(
            forumInfo ->
                forumIdentityMap.put(
                    forumInfo.getForumId(), getUserIdentityOfForum(forumInfo, true)));
    Set<String> userIdList = new HashSet<>();
    topicInfoList.forEach(
        topicInfo -> {
          userIdList.add(topicInfo.getTopicCreateUserId());
          userIdList.add(topicInfo.getTopicLastModifiedUserId());
        });
    Map<String, IdName> userIdNameMap =
        userService
            .getUserByIds(
                userIdList
                    .parallelStream()
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList()))
            .stream()
            .collect(
                LinkedHashMap::new,
                (map, item) ->
                    map.put(item.getId(), new IdName().id(item.getId()).name(item.getName())),
                Map::putAll);
    String userId = Utility.getUserIdFromSession();
    return topicInfoList
        .parallelStream()
        .map(
            topicInfo -> {
              ForumInfo forumInfo = forumInfoMap.get(topicInfo.getForumId());
              Identity forumIdentity = forumIdentityMap.get(topicInfo.getForumId());
              return new CommunityResultDetail()
                  .type(SearchType.TOPIC)
                  .read(read)
                  .data(
                      new SearchTopicData()
                          .id(topicInfo.getTopicId())
                          .name(topicInfo.getTopicTitle())
                          .status(TopicStatus.fromValue(topicInfo.getTopicStatus()))
                          .type(TopicType.fromValue(topicInfo.getTopicType()))
                          .state(TopicState.fromValue(topicInfo.getTopicState()))
                          .situation(TopicSituation.fromValue(topicInfo.getTopicSituation()))
                          .showState(topicInfo.isShowState())
                          .createUser(
                              Optional.ofNullable(
                                      userIdNameMap.get(topicInfo.getTopicCreateUserId()))
                                  .orElseGet(IdName::new))
                          .lastModifiedUser(
                              Optional.ofNullable(
                                      userIdNameMap.get(topicInfo.getTopicLastModifiedUserId()))
                                  .orElseGet(IdName::new))
                          .lastModifiedTime(topicInfo.getTopicLastModifiedTime())
                          .createTime(topicInfo.getTopicCreateTime())
                          .source(
                              Collections.singletonList(
                                  Optional.ofNullable(forumInfo)
                                      .map(
                                          forum ->
                                              new Source()
                                                  .id(forum.getForumId())
                                                  .type(SearchType.FORUM)
                                                  .name(forum.getForumName()))
                                      .orElseGet(Source::new)))
                          .identity(
                              getUserIdentityOfTopic(
                                  userId, forumIdentity, topicInfo.getTopicCreateUserId()))
                          .toppingOrder(topicInfo.getTopicToppingOrder()));
            })
        .collect(Collectors.toList());
  }

  public CommunityResultList getHotTopicListOfCommunity(int communityId, int offset, int limit) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    List<TopicInfo> topicInfoList =
        topicDao.getHotPublicTopicOfCommunity(communityId, offset, limit, dlInfo.isDL, dlInfo.allowForumId,
                yamlConfig.getHotLastingMin() * MINUTE_TO_SECOND_MULTIPLY);
    List<CommunityResultDetail> topicDataList =
        transferTopicInfoToCommunityResultDetail(topicInfoList, true);
    return new CommunityResultList().result(topicDataList).numFound(topicDataList.size());
  }

  public CommunityResultList getTopicListOfForum(
      int forumId,
      int offset,
      int limit,
      Order sort,
      String state,
      String type,
      boolean withTopping) {
    ForumInfo forumInfo = forumService.getForumInfoById(forumId);
    boolean read = checkCurrentUserCanRead(forumInfo);
    int toppingTopicNumber = 0;
    if (withTopping) {
      toppingTopicNumber = topicDao.getToppingTopicByForumId(forumId, -1, -1).size();
    }
    int page = (offset / limit);
    int mod = (offset % limit);
    limit -= toppingTopicNumber;
    offset = mod + page * limit;
    List<String> stateList = getTopicStateList(state);
    List<TopicInfo> topicInfoList =
        getTopicOfForumWithSortAndLimit(forumId, offset, limit, sort, stateList, type);
    List<CommunityResultDetail> topicDataList =
        transferTopicInfoToCommunityResultDetail(topicInfoList, read);
    int numFound = countTopicOfForum(forumId, stateList, type);
    return new CommunityResultList().result(topicDataList).numFound(numFound);
  }

  public CommunityResultList getToppingTopicListOfForum(int forumId, int offset, int limit) {
    List<TopicInfo> topicInfoList = topicDao.getToppingTopicByForumId(forumId, offset, limit);
    List<CommunityResultDetail> topicDataList =
        transferTopicInfoToCommunityResultDetail(topicInfoList, true);
    return new CommunityResultList().result(topicDataList).numFound(topicDataList.size());
  }

  public CommunityResultList getAttachmentListOfCommunity(
      int communityId, int offset, int limit, List<String> fileExt, Order sort) {
    boolean isSysAdmin = userService.isSysAdmin();
    String userId = Utility.getCurrentUserIdWithGroupId();
    String fileExtWithCommaSeperated =
        Optional.ofNullable(fileExt)
            .orElseGet(ArrayList::new)
            .stream()
            .collect(Collectors.joining(Constants.COMMA_DELIMITER));
    int numFound =
        fileService.countOwnAttachmentOfCommunity(
            communityId, isSysAdmin, userId, fileExtWithCommaSeperated);
    List<CommunityResultDetail> attachmentDataList;
    if (SortField.UPDATETIME.toString().equals(sort.getProperty())) {
      attachmentDataList =
          getAttachmentListFromDataHive(
              communityId, offset, limit, fileExt, sort.getDirection().toString());
    } else {
      attachmentDataList =
          getAttachmentListFromCommunity(
              communityId, isSysAdmin, userId, offset, limit, fileExtWithCommaSeperated, sort);
    }
    return new CommunityResultList().result(attachmentDataList).numFound(numFound);
  }

  public List<TopicTypeEntity> getTopicTypeByCommunityId(int communityId) {
    return topicDao.getTopicTypeByCommunityId(communityId, AcceptLanguage.getLanguageForDb());
  }

  public List<TopicTypeEntity> getTopicTypeByCommunityIdAndForumId(int communityId, int forumId) {
    return topicDao.getTopicTypeByCommunityIdAndForumId(
        communityId, forumId, AcceptLanguage.getLanguageForDb());
  }

  public List<ConclusionStateEntity> getConclusionStateByTopicType(List<Integer> topicTypeIdList) {
    return topicDao.getConclusionState(topicTypeIdList, AcceptLanguage.getLanguageForDb());
  }

  public List<Attachment> getTopicAttachmentList(int topicId, boolean withThumbnail) {
    List<String> attachmentIdList = getAttachmentIdOfTopic(topicId);
    List<Attachment> result = fileService.getAttachmentList(attachmentIdList, withThumbnail);
    Optional.ofNullable(result)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(
            list -> {
              Map<String, List<AttachmentAppFieldEntity>> appFieldMap =
                  topicDao
                      .getTopicAllAttachmentAppField(
                          attachmentIdList, AcceptLanguage.getLanguageForDb())
                      .stream()
                      .collect(Collectors.groupingBy(AttachmentAppFieldEntity::getAttachmentId));
              result.forEach(
                  item ->
                      item.setAppField(
                          appFieldMap
                              .getOrDefault(item.getId(), Collections.emptyList())
                              .stream()
                              .map(
                                  appField ->
                                      new LabelValueDto()
                                          .value(appField.getAppFieldId())
                                          .label(appField.getAppFieldName()))
                              .collect(Collectors.toList())));
            });
    return result;
  }

  public List<IdNameEntity> getAttachmentAppField(String ddfId, String lang) {
    return topicDao.getTopicAttachmentAppField(ddfId, lang);
  }

  public void validateTopicPrivilege(TopicOperation operation, int topicId) {
    topicPrivilegeService.validatePrivilege(operation, topicId);
  }

  public String getForumTypeById(int topicId) {
    return topicDao.getForumTypeByTopicId(topicId);
  }

  private List<CommunityResultDetail> getAttachmentListFromDataHive(
      int communityId, int offset, int limit, List<String> fileExt, String direction) {
    List<String> attachmentIdListOfCommunity = fileService.getAttachmentOfCommunity(communityId);
    if (CollectionUtils.isEmpty(attachmentIdListOfCommunity)) {
      return Collections.emptyList();
    }
    QueryTree<QueryTerm> query = getAttachmentQueryTree(attachmentIdListOfCommunity, fileExt);
    List<Sorting> order =
        Arrays.asList(
            new Sorting(SortableField.USER_MODIFY_DATE, SortOrder.valueOf(direction)),
            new Sorting(SortableField.NAME, SortOrder.ASC));
    Map<String, BaseSection> attachmentBaseSectionMap =
        fileService.getFileBaseSectionByCondition(
            query, order, offset, limit, EnumSet.of(PrivilegeType.READ_PROTECTED));
    Map<String, AttachmentDetail> attachmentDetailMap =
        attachmentBaseSectionMap
            .keySet()
            .stream()
            .map(fileService::getAttachmentDetail)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(AttachmentDetail::getId, Function.identity()));
    return attachmentBaseSectionMap
        .keySet()
        .stream()
        .map(
            attachmentId ->
                getCommunityResultDetailFromBaseSection(
                    attachmentDetailMap.get(attachmentId),
                    attachmentBaseSectionMap.get(attachmentId)))
        .collect(Collectors.toList());
  }

  private QueryTree<QueryTerm> getAttachmentQueryTree(
      List<String> attachmentIdList, List<String> fileExt) {
    QueryTree<QueryTerm> query =
        QueryTerm.builder()
            .setField(QueryTerm.Field.UUID)
            .setValues(attachmentIdList.toArray())
            .build()
            .and(
                QueryTerm.builder()
                    .setField(QueryTerm.Field.STATUS)
                    .setValues(DDFStatus.OPEN)
                    .build())
            .and(
                QueryTerm.builder()
                    .setField(QueryTerm.Field.DOC_CAT)
                    .setValues(
                        Arrays.asList(
                                DdfDocCat.TOPIC_IMAGE.toString(),
                                DdfDocCat.TOPIC_ATTACHMENT.toString())
                            .toArray())
                    .build());
    if (!CollectionUtils.isEmpty(fileExt)) {
      query =
          query.and(
              QueryTerm.builder()
                  .setField(QueryTerm.Field.ICON)
                  .setValues(fileExt.stream().map(String::toUpperCase).toArray())
                  .build());
    }
    return query;
  }

  private List<CommunityResultDetail> getAttachmentListFromCommunity(
      int communityId,
      boolean isSysAdmin,
      String userId,
      int offset,
      int limit,
      String fileExtWithCommaSeperated,
      Order sort) {
    List<String> attachmentList =
        fileService.getOwnAttachmentOfCommunity(
            communityId,
            isSysAdmin,
            userId,
            offset,
            limit,
            getSortFieldOfAttachment(sort.getProperty()),
            sort.getDirection().toString(),
            fileExtWithCommaSeperated);
    return Optional.of(attachmentList)
        .filter(list -> !list.isEmpty())
        .map(this::transferAttachmentToCommunityResultDetail)
        .orElseGet(Collections::emptyList);
  }

  private List<CommunityResultDetail> transferAttachmentToCommunityResultDetail(
      List<String> attachmentIdList) {
    Map<String, AttachmentDetail> attachmentDetailMap =
        attachmentIdList
            .stream()
            .map(fileService::getAttachmentDetail)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(AttachmentDetail::getId, Function.identity()));
    Map<String, BaseSection> attachmentBaseSectionMap =
        fileService.getFileBaseSection(attachmentIdList);
    return attachmentIdList
        .stream()
        .filter(attachmentDetailMap::containsKey)
        .map(
            attachmentId -> {
              AttachmentDetail attachmentDetail = attachmentDetailMap.get(attachmentId);
              BaseSection base = attachmentBaseSectionMap.get(attachmentId);
              return getCommunityResultDetailFromBaseSection(attachmentDetail, base);
            })
        .collect(Collectors.toList());
  }

  private CommunityResultDetail getCommunityResultDetailFromBaseSection(
      AttachmentDetail attachmentDetail, BaseSection base) {
    attachmentDetail = Optional.ofNullable(attachmentDetail).orElseGet(AttachmentDetail::new);
    base = Optional.ofNullable(base).orElseGet(BaseSection::new);
    Long createTime = getMilliSecondFromInstant(base.getUserCreatedDate(), 0L);
    return new CommunityResultDetail()
        .type(SearchType.ATTACHMENT)
        .read(true)
        .data(
            new SearchAttachmentData()
                .id(StringUtils.defaultString(base.getUuid()))
                .name(StringUtils.defaultString(base.getName()))
                .refUrl(StringUtils.defaultString(base.getDataUrl()))
                .fileExt(StringUtils.defaultString(base.getFileExt()))
                .createTime(createTime)
                .createUser(getUserFromRole(base.getPeople(), DdfRole.APPLASSIGNEDCREATOR))
                .lastModifiedTime(getMilliSecondFromInstant(base.getUserModifiedDate(), createTime))
                .lastModifiedUser(getUserFromRole(base.getPeople(), DdfRole.APPLASSIGNEDMODIFIER))
                .source(
                    Collections.singletonList(
                        new Source()
                            .id(attachmentDetail.getTopicId())
                            .type(SearchType.TOPIC)
                            .name(attachmentDetail.getTopicTitle()))));
  }

  private Long getMilliSecondFromInstant(Instant instant, Long defaultValue) {
    return Optional.ofNullable(instant).map(Instant::toEpochMilli).orElseGet(() -> defaultValue);
  }

  private IdName getUserFromRole(Map<String, List<UserGroupEntity>> people, DdfRole role) {
    return Optional.ofNullable(people)
        .map(map -> map.getOrDefault(role.toString(), Collections.emptyList()))
        .filter(list -> !list.isEmpty())
        .map(
            list ->
                list.parallelStream()
                    .filter(Objects::nonNull)
                    .map(user -> new IdName().id(user.getUuid()).name(user.getName()))
                    .findFirst()
                    .orElseGet(IdName::new))
        .orElseGet(IdName::new);
  }

  private TopicInfo convertToTopicInfo(TopicCreationData topicData, boolean fromIssueTrack) {
    final long now = new Date().getTime();
    String userId = "";
    if (topicData.getForumId().equals(Integer.parseInt(issueTrackConfig.getForumId()))) {
      userId = issueTrackConfig.getAdminId();
    } else {
      userId = Utility.getUserIdFromSession();
    }
    List<String> appFieldList =
        Optional.ofNullable(topicData.getAppField())
            .map(
                list ->
                    list.stream()
                        .map(item -> item.getValue().toString())
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toList()))
            .filter(list -> !CollectionUtils.isEmpty(list))
            .orElseThrow(() -> new IllegalArgumentException(Constants.ERR_INVALID_PARAM));
    return new TopicInfo()
        .setForumId(topicData.getForumId())
        .setTopicTitle(topicData.getTitle())
        .setTopicType(topicData.getType().toString())
        .setTopicCreateTime(now)
        .setTopicCreateUserId(userId)
        .setTopicModifiedTime(now)
        .setTopicModifiedUserId(userId)
        .setTopicLastModifiedTime(now)
        .setTopicLastModifiedUserId(userId)
        .setTopicState(TopicState.UNCONCLUDED.toString())
        .setTopicStatus(TopicStatus.OPEN.toString())
        .setTopicSituation(TopicSituation.NORMAL.toString())
        .setTopicViewCount(0)
        .setTopicText(topicData.getText())
        .setAppFieldList(appFieldList);
  }

  private TopicInfo convertToTopicInfo(
      TopicPqmData topicPqmData, String userId, String type, String title) {
    long now = new Date().getTime();
    return new TopicInfo()
        .setForumId(topicPqmData.getForumId())
        .setTopicTitle(title)
        .setTopicType(type)
        .setTopicCreateTime(now)
        .setTopicCreateUserId(userId)
        .setTopicModifiedTime(now)
        .setTopicModifiedUserId(userId)
        .setTopicLastModifiedTime(now)
        .setTopicLastModifiedUserId(userId)
        .setTopicState(TopicState.UNCONCLUDED.toString())
        .setTopicStatus(TopicStatus.OPEN.toString())
        .setTopicSituation(TopicSituation.NORMAL.toString())
        .setTopicViewCount(0)
        .setTopicText(topicPqmData.getText());
  }

  private boolean isSupportTopicType(List<TopicTypeEntity> supportTopicType, TopicType targetType) {
    return supportTopicType
        .parallelStream()
        .anyMatch(item -> TopicType.fromValue(item.getTopicType()) == targetType);
  }

  private void validateAppField(
      List<LabelValueDto> topicAppFieldList, List<AttachmentWithAuthor> attachmentList) {
    if (CollectionUtils.isEmpty(topicAppFieldList)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    if (attachmentList
        .parallelStream()
        .map(AttachmentWithAuthor::getAppField)
        .anyMatch(CollectionUtils::isEmpty)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  private void defaultTopicData(TopicCreationData topic) {
    topic.text(defaultString(topic.getText())).recipient(defaultList(topic.getRecipient()));
  }

  private void defaultTopicData(TopicUpdatedData topic) {
    topic.text(defaultString(topic.getText())).recipient(defaultList(topic.getRecipient()));
  }

  private void defaultRecipient(TopicPqmData topic) {
    topic.text(defaultString(topic.getText())).recipient(defaultList(topic.getRecipient()));
  }

  private List<String> defaultList(List<String> list) {
    return ofNullable(list).orElseGet(ArrayList::new);
  }
}
