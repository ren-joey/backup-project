package com.delta.dms.community.service;

import static com.delta.dms.community.swagger.model.Emoji.LIKE;
import static com.delta.dms.community.swagger.model.ForumType.PUBLIC;
import static com.delta.dms.community.swagger.model.TopicType.GENERAL;
import static com.delta.dms.community.utils.Constants.*;
import static com.delta.dms.community.utils.DataConstants.COMMON_DATE_FORMAT;
import static com.delta.dms.community.utils.DateUtility.EMPTY_DATE_TIME_VALUE;
import static com.delta.dms.community.utils.DateUtility.convertToDateTime;
import static com.delta.dms.community.utils.I18nConstants.MSG_CONCLUSION_EMPTY;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.dao.entity.*;
import com.delta.dms.community.enums.ExcelReplyReportHeaderRaw;
import com.delta.dms.community.enums.I18nEnum;
import com.delta.dms.community.swagger.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.FileArchiveQueueDao;
import com.delta.dms.community.dao.MedalDao;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.enums.FileArchiveType;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.exception.UpdateConflictException;
import com.delta.dms.community.exception.UpdateFailedException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.SourceOsParam;
import com.delta.dms.community.service.conclusion.BaseConclusion;
import com.delta.dms.community.service.conclusion.ConclusionFactory;
import com.delta.dms.community.service.conclusion.GeneralConclusion;
import com.delta.dms.community.service.eerp.EerpService;
import com.delta.dms.community.service.privilege.reply.ReplyPrivilegeService;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class ReplyService {

  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String NOTIFICATION_TYPE = "type";
  private static final String RECIPIENT = "recipient";
  private static final String DELETE_TIME_FORMAT = "yyyy/MM/dd HH:mm";
  private static final String DELETE_REPLY_TEXT_FORMAT = "此回覆已於%s被%s刪除";
  private static final int INITIAL_VALUE = 0;
  private static final int MAX_REPLY_FETCH_LIMIT = 9999;
  private static final Integer EMPTY_LIKE_COUNT_VALUE = -1;

  private final ForumService forumService;
  private final TopicService topicService;
  private final UserService userService;
  private final FileService fileService;
  private final RichTextHandlerService richTextHandlerService;
  private final PrivilegeService privilegeService;
  private final EmojiService emojiService;
  private final EventPublishService eventPublishService;
  private final CommunityService communityService;
  private final EerpService eerpService;
  private final OrgGroupExtensionService orgGroupExtensionService;
  private final GroupRecipientHandleService groupRecipientHandleService;
  private final ReplyPrivilegeService replyPrivilegeService;
  private final UserGroupAdapter userGroupAdapter;
  private final ReplyDao replyDao;
  private final MedalDao medalDao;
  private final FileArchiveQueueDao fileArchiveQueueDao;
  private final YamlConfig yamlConfig;
  @Autowired
  private MessageSource messageSource;

  public List<ReplyInfo> getReplyListOfTopic(
      int topicId, int offset, int limit, Direction sortOrder) {
    return replyDao.getReplyListOfTopic(topicId, offset, limit, sortOrder.toString());
  }

  public ReplyData createReply(ReplyCreationData replyData, int topicId) {
    defaultRecipient(replyData);
    validateAppField(topicId, replyData.getAttachment());
    ForumInfo forumInfo = forumService.getForumInfoById(replyData.getForumId());
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(topicId);
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    if (!checkCurrentUserCanCreate(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_CREATE_REPLY);
    }
    if(replyData.getFollowReplyId() != null) {
      getReplyInfoById(replyData.getFollowReplyId()); // throw exception if reply deleted
    }

    long attachmentTotalSize = getAttachmentTotalSize(replyData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    Optional.ofNullable(replyData.getAttachment())
        .orElse(new ArrayList<>())
        .forEach(
            item ->
                eventPublishService.publishFileUploadingEvent(
                    item.getId(),
                    item.getAuthor(),
                    forumInfo.getCommunityId(),
                    forumInfo.getForumId(),
                    ForumType.fromValue(forumInfo.getForumType()),
                    false,
                    item.getVideoLanguage()));
    int index = getReplyIndex(replyData.getForumId(), topicId, replyData.getFollowReplyId());
    ReplyInfo replyInfo = setReplyInfo(replyData, index, topicId);
    if (!addReply(replyInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_REPLY_FAILED);
    }
    int replyId = replyInfo.getReplyId();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());
    Map<String, List<UserGroupEntity>> richTextRoleMap =
        fileService.getRoleMap(Utility.getUserIdFromSession(), new ArrayList<>());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(Utility.getUserIdFromSession(), forumAdminRoleList, forumMemberRoleList);
    if (!addTextOfReply(
        replyId,
        replyData.getText(),
        ForumType.fromValue(forumInfo.getForumType()),
        richTextRoleMap,
        privilegeMap,
        "")) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_REPLY_FAILED);
    }
    Optional.ofNullable(replyData.getAttachment())
        .orElse(new ArrayList<>())
        .forEach(
            item -> {
              addAttachmentOfReply(replyId, item.getId());
              addReplyAttachmentAppField(
                  item.getId(),
                  item.getAppField()
                      .stream()
                      .map(appField -> appField.getValue().toString())
                      .collect(Collectors.toList()));
            });
    eventPublishService.publishReplyChangingEvent(
        forumInfo.getCommunityId(),
        replyData.getForumId(),
        topicId,
        Utility.getUserIdFromSession(),
        replyInfo.getReplyCreateTime());
    sendReplyNotification(
        replyId,
        forumInfo,
        replyData.getText(),
        replyData.getNotificationType(),
        replyData.getRecipient(),
        topicInfo,
        replyData.getOrgMembers(),
        replyData.getBgbus());
    publishActivityLogEventFromReply(
        Utility.getUserIdFromSession(),
        forumInfo,
        replyId,
        Operation.CREATE,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    replyData
        .getAttachment()
        .stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                publishActivityLogEventFromReply(
                    Utility.getUserIdFromSession(),
                    forumInfo,
                    replyId,
                    Operation.UPLOAD,
                    Constants.INFO_PROJECT_NAME,
                    Constants.CONTENT_EMPTY,
                    item.getId()));
    ReplyData code = new ReplyData();
    code.setIndex(index);
    code.setReplyId(replyId);
    return code;
  }

  private int getReplyIndex(Integer forumId, int topicId, Integer replyId) {
    if (Optional.ofNullable(replyId).orElse(INITIAL_VALUE) != INITIAL_VALUE) {
      return INITIAL_VALUE;
    } else {
      return replyDao.getReplyIndexById(forumId, topicId);
    }
  }

  private boolean checkCurrentUserCanCreate(ForumInfo forumInfo) {
    return checkUserPermission(Utility.getCurrentUserIdWithGroupId(), forumInfo, Operation.CREATE);
  }

  private boolean checkUserPermission(String userId, ForumInfo forumInfo, Operation operation) {
    PermissionObject permissionObject = getPermissionObjectOfReply(forumInfo);
    return privilegeService.checkUserPrivilege(
        userId,
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        permissionObject.toString(),
        operation.toString());
  }

  private PermissionObject getPermissionObjectOfReply(ForumInfo forumInfo) {
    return !ForumType.PRIVATE.equals(ForumType.fromValue(forumInfo.getForumType()))
        ? getDetailedPublicForumReplyByCommunityId(forumInfo.getCommunityId())
        : PermissionObject.PRIVATEFORUMREPLY;
  }

  private PermissionObject getDetailedPublicForumReplyByCommunityId(int communityId) {
    CommunityInfo communityInfo = communityService.getCommunityInfoById(communityId);
    return CommunityType.ACTIVITY.equals(CommunityType.fromValue(communityInfo.getCommunityType()))
        ? PermissionObject.ACTIVEREPLY
        : PermissionObject.PUBLICFORUMREPLY;
  }

  private ReplyInfo setReplyInfo(ReplyCreationData replyData, int index, int topicId) {
    final long now = new Date().getTime();
    ReplyInfo replyInfo = new ReplyInfo();
    replyInfo.setForumId(replyData.getForumId());
    replyInfo.setFollowTopicId(topicId);
    if (Optional.ofNullable(replyData.getFollowReplyId()).orElse(INITIAL_VALUE) != INITIAL_VALUE) {
      replyInfo.setFollowReplyId(replyData.getFollowReplyId());
    }
    replyInfo.setReplyIndex(index);
    replyInfo.setReplyStatus(ReplyStatus.OPEN.toString());
    String userId = Utility.getUserIdFromSession();
    replyInfo.setReplyCreateTime(now);
    replyInfo.setReplyCreateUserId(userId);
    replyInfo.setReplyModifiedTime(now);
    replyInfo.setReplyModifiedUserId(userId);
    replyInfo.setReplyRespondee(replyData.getRespondee());
    return replyInfo;
  }

  private boolean addReply(ReplyInfo replyInfo) {
    return replyDao.addInfo(replyInfo) != 0;
  }

  private boolean addTextOfReply(
      int replyId,
      String text,
      ForumType forumType,
      Map<String, List<UserGroupEntity>> richTextRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String originalText) {
    String newText = richTextHandlerService.replaceCopyedImageToBase64(text, originalText);
    String html =
        richTextHandlerService
            .replaceRichTextImageSrcWithImageDataHiveUrl(
                forumType, richTextRoleMap, privilegeMap, newText)
            .getText();
    ReplyInfo replyInfo = new ReplyInfo();
    replyInfo.setReplyId(replyId);
    replyInfo.setReplyText(html);
    return replyDao.addText(replyInfo) != 0;
  }

  private void addAttachmentOfReply(int replyId, String attachmentId) {
    BaseSection attachment =
        fileService.readDdf(attachmentId, FileService.DDF_BASE_FIELD).getBaseSection();
    if (attachment != null) {
      replyDao.addAttachmentOfReply(
          replyId,
          attachment.getUuid(),
          attachment.getName(),
          attachment.getFileExt(),
          attachment.getDisplayTime().toEpochMilli());
    }
  }

  private void addReplyAttachmentAppField(String attachmentId, List<String> appFieldIdList) {
    Optional.ofNullable(appFieldIdList)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(list -> replyDao.insertReplyAttachmentAppField(attachmentId, list));
  }

  private void updateReplyAttachmentAppField(String attachmentId, List<String> appFieldIdList) {
    List<String> originalIdList =
        getAttachmentAppField(attachmentId, AcceptLanguage.getLanguageForDb())
            .stream()
            .map(item -> item.getId().toString())
            .collect(Collectors.toList());
    if (!originalIdList.equals(appFieldIdList)) {
      replyDao.deleteReplyAttachmentAppField(attachmentId);
      addReplyAttachmentAppField(attachmentId, appFieldIdList);
    }
  }

  private void sendReplyNotification(
      int replyId,
      ForumInfo forumInfo,
      String text,
      NotificationType notificationType,
      List<String> recipient,
      TopicInfo topicInfo,
      List<SimpleGroupWithUsers> orgMembers,
      List<SimpleGroupWithUsers> bgbus) {
    recipient = forumService.validateForumMembers(forumInfo, recipient);
    addNotificationOfReply(forumInfo.getForumId(), replyId, notificationType, recipient);
    List<String> orgRecipient =
        getAndUpdateOrgRecipient(forumInfo.getForumType(), replyId, orgMembers);
    List<String> bgbuRecipient =
        getAndUpdateBgbuRecipient(forumInfo.getForumType(), replyId, bgbus);
    if (notificationType != null) {
      List<String> users =
          Stream.of(recipient, orgRecipient, bgbuRecipient)
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList());
      EmailWithChineseAndEnglishContext context =
          getReplyNotificationContext(forumInfo, text, notificationType, users, topicInfo);
      eventPublishService.publishEmailSendingEvent(context);
      eventPublishService.publishNotificationSendingEvent(
          getNotification(forumInfo, notificationType, users, topicInfo));
    }
  }

  protected List<String> getAndUpdateOrgRecipient(
      String forumType, int replyId, List<SimpleGroupWithUsers> orgMembers) {
    replyDao.deleteOrgMemberNotificationOfReply(replyId);
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
                        replyDao.upsertOrgMemberNotificationOfReply(
                            replyId, orgId, StringUtils.EMPTY);
                      } else {
                        replyDao.upsertOrgMemberNotificationOfReply(
                            replyId,
                            orgId,
                            members
                                .parallelStream()
                                .collect(Collectors.joining(Constants.COMMA_DELIMITER)));
                      }
                    }));
    return orgRecipient;
  }

  protected List<String> getAndUpdateBgbuRecipient(
      String forumType, int replyId, List<SimpleGroupWithUsers> bgbus) {
    replyDao.deleteBgbuNotificationOfReply(replyId);
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
                        replyDao.upsertBgbuNotificationOfReply(replyId, orgId, StringUtils.EMPTY);
                      } else {
                        replyDao.upsertBgbuNotificationOfReply(
                            replyId,
                            orgId,
                            members
                                .parallelStream()
                                .collect(Collectors.joining(Constants.COMMA_DELIMITER)));
                      }
                    }));
    return bgbuRecipient;
  }

  private void addNotificationOfReply(
      int forumId, int replyId, NotificationType notificationType, List<String> recipient) {
    if (NotificationType.ALL.equals(notificationType)) {
      /*
      List<String> userIdList =
          forumService
              .getMemberOfForum(forumId, -1, -1)
              .stream()
              .map(User::getId)
              .collect(Collectors.toList());
       */
      replyDao.addNotificationOfReply(
          replyId,
          notificationType.toString(),
          //userIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)) -Caused by: java.sql.SQLDataException: (conn=14550200) Data too long for column 'recipient'
          notificationType.toString()
      );
    } else if (NotificationType.CUSTOM.equals(notificationType)) {
      replyDao.addNotificationOfReply(
          replyId,
          notificationType.toString(),
          recipient.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)));
    } else {
      replyDao.addNotificationOfReply(replyId, "", "");
    }
  }

  private EmailWithChineseAndEnglishContext getReplyNotificationContext(
      ForumInfo forumInfo,
      String text,
      NotificationType notificationType,
      List<String> recipient,
      TopicInfo topicInfo) {
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.TOPIC_URI_FORMAT, host, language, topicInfo.getTopicId());
    String mobileLink =
        String.format(
            EmailConstants.TOPIC_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            topicInfo.getTopicId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.TOPICNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationType)) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .forumInfo(forumInfo)
        .sender(userName)
        .desc(String.format(EmailConstants.REPLY_NOTIFICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.REPLY_NOTIFICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + topicInfo.getTopicTitle())
        .content(Utility.getTextFromHtml(text))
        .to(
            userService.getEmailByUserId(
                getRecipientList(forumInfo.getForumId(), notificationType, recipient)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_TOPIC, topicInfo.getTopicTitle()),
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

  public ReplyInfo getConclusionByTopicId(int topicId) {
    List<ReplyInfo> replyInfoList =
        replyDao.getReplyListOfTopic(
            topicId, NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ONE, Direction.ASC.toString());
    if (!CollectionUtils.isEmpty(replyInfoList)
        && NumberUtils.INTEGER_ZERO
            == replyInfoList.get(NumberUtils.INTEGER_ZERO).getReplyIndex()) {
      return replyInfoList.get(NumberUtils.INTEGER_ZERO);
    }
    return null;
  }

  private boolean checkIsConcluded(int topicId) {
    return Objects.nonNull(getConclusionByTopicId(topicId));
  }

  public Integer createConclusion(ReplyConclusionCreationData conclusionData, int topicId)
      throws IOException {
    if (checkIsConcluded(topicId)) {
      return HttpStatus.CONFLICT.value();
    }
    defaultRecipient(conclusionData);
    validateAppField(topicId, conclusionData.getAttachment());
    ForumInfo forumInfo = forumService.getForumInfoById(conclusionData.getForumId());
    if (ForumStatus.LOCKED.toString().equals(forumInfo.getForumStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(topicId);
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    if (!checkCurrentUserCanCreateConclusion(
        topicInfo.getTopicCreateUserId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_CREATE_CONCLUSION);
    }
    long attachmentTotalSize = getAttachmentTotalSize(conclusionData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    BaseConclusion conclusion =
        new ConclusionFactory()
            .getConclusion(conclusionData.getTopicType(), conclusionData.getJsonData());
    TopicState topicState =
        sendConclusionToRemote(topicInfo, conclusion, conclusionData.isForceConclude());
    ReplyCreationData replyData = setReplyCreationData(conclusionData, conclusion.getText());
    ReplyInfo replyInfo = setReplyInfo(replyData, REPLY_CONCLUSION_INDEX, topicId);
    if (!addReply(replyInfo)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_REPLY_FAILED);
    }

    int replyId = replyInfo.getReplyId();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());

    String userId = Utility.getUserIdFromSession();
    Map<String, List<UserGroupEntity>> richTextRoleMap =
        fileService.getRoleMap(userId, new ArrayList<>());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(userId, forumAdminRoleList , forumMemberRoleList );
    if (!addConclusionTextOfReply(
        replyId,
        conclusion,
        ForumType.fromValue(forumInfo.getForumType()),
        richTextRoleMap,
        privilegeMap)) {
      throw new IllegalArgumentException(I18nConstants.MSG_CREATE_REPLY_FAILED);
    }

    archiveConclusionAttachments(topicInfo, replyId, replyData.getAttachment());
    if (replyData.getAttachment() != null) {
      replyData
          .getAttachment()
          .forEach(
              item -> {
                addAttachmentOfReply(replyId, item.getId());
                addReplyAttachmentAppField(
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
      addReplyAttachmentRecordType(
          replyData
              .getAttachment()
              .stream()
              .filter(attachment -> StringUtils.isNotEmpty(attachment.getRecordType()))
              .collect(toMap(AttachmentWithAuthor::getId, AttachmentWithAuthor::getRecordType)));
    }
    topicService.updateTopicState(topicId, topicState);
    if (Boolean.TRUE.equals(conclusionData.isSeal())) {
      topicService.sealTopic(topicId);
    } else {
      topicService.unsealTopic(topicId);
    }
    eventPublishService.publishReplyChangingEvent(
        forumInfo.getCommunityId(),
        replyData.getForumId(),
        topicId,
        Utility.getUserIdFromSession(),
        replyInfo.getReplyCreateTime());
    sendConclusionNotification(
        replyId,
        forumInfo,
        replyData.getText(),
        replyData.getNotificationType(),
        replyData.getRecipient(),
        topicInfo,
        replyData.getOrgMembers(),
        replyData.getBgbus());
    sendConcludedNotification(forumInfo, replyData.getText(), topicInfo);
    publishActivityLogEventFromConclusion(
        Utility.getUserIdFromSession(),
        replyId,
        Operation.CREATE,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    replyData
        .getAttachment()
        .stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                publishActivityLogEventFromConclusion(
                    Utility.getUserIdFromSession(),
                    replyId,
                    Operation.UPLOAD,
                    Constants.INFO_PROJECT_NAME,
                    Constants.CONTENT_EMPTY,
                    item.getId()));
    return HttpStatus.CREATED.value();
  }

  private boolean checkCurrentUserCanCreateConclusion(
      String authorId, ForumInfo forumInfo) {
    if (privilegeService.checkUserPrivilege(
        Utility.getCurrentUserIdWithGroupId(),
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        PermissionObject.CONCLUSION.toString(),
        Operation.CREATE.toString())) {
      return true;
    } else if (ForumType.PUBLIC.equals(ForumType.fromValue(forumInfo.getForumType()))
        && CommunityType.ACTIVITY.equals(
            CommunityType.fromValue(
                communityService
                    .getCommunityInfoById(forumInfo.getCommunityId())
                    .getCommunityType()))) {
      return authorId.equals(Utility.getUserIdFromSession());
    } else {
      List<User> userInForumMember = forumService.getMemberOfForumWithFilters(
              false, forumInfo.getForumId(), -1, -1, org.apache.commons.lang.StringUtils.EMPTY,
              Collections.singletonList(Utility.getUserIdFromSession()),
              null, org.apache.commons.lang.StringUtils.EMPTY);
      if (userInForumMember.isEmpty()) {
        return false;
      }
      return authorId.equals(Utility.getUserIdFromSession());
    }
  }

  private ReplyCreationData setReplyCreationData(
      ReplyConclusionCreationData conclusionData, String conclusion) {
    ReplyCreationData replyData = new ReplyCreationData();
    replyData.setForumId(conclusionData.getForumId());
    if (conclusionData.getFollowReplyId() != null) {
      replyData.setFollowReplyId(conclusionData.getFollowReplyId());
    }
    if (conclusionData.getAttachment() != null) {
      replyData.setAttachment(conclusionData.getAttachment());
    }
    if (conclusionData.getNotificationType() != null) {
      replyData.setNotificationType(conclusionData.getNotificationType());
    }
    if (conclusionData.getRecipient() != null) {
      replyData.setRecipient(conclusionData.getRecipient());
    }
    replyData.setText(conclusion);
    if (conclusionData.getOrgMembers() != null) {
      replyData.setOrgMembers(conclusionData.getOrgMembers());
    }
    if (conclusionData.getBgbus() != null) {
      replyData.setBgbus(conclusionData.getBgbus());
    }
    return replyData;
  }

  private void sendConclusionNotification(
      int replyId,
      ForumInfo forumInfo,
      String text,
      NotificationType notificationType,
      List<String> recipient,
      TopicInfo topicInfo,
      List<SimpleGroupWithUsers> orgMembers,
      List<SimpleGroupWithUsers> bgbus) {
    recipient = forumService.validateForumMembers(forumInfo, recipient);
    addNotificationOfReply(forumInfo.getForumId(), replyId, notificationType, recipient);
    List<String> orgRecipient =
        getAndUpdateOrgRecipient(forumInfo.getForumType(), replyId, orgMembers);
    List<String> bgbuRecipient =
        getAndUpdateBgbuRecipient(forumInfo.getForumType(), replyId, bgbus);
    if (notificationType != null) {
      List<String> users =
          Stream.of(recipient, new ArrayList<>(orgRecipient), new ArrayList<>(bgbuRecipient))
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList());
      EmailWithChineseAndEnglishContext context =
          getConclusionNotificationContext(forumInfo, text, notificationType, users, topicInfo);
      eventPublishService.publishEmailSendingEvent(context);
      eventPublishService.publishNotificationSendingEvent(
          getNotification(forumInfo, notificationType, users, topicInfo));
    }
  }

  private EmailWithChineseAndEnglishContext getConclusionNotificationContext(
      ForumInfo forumInfo,
      String text,
      NotificationType notificationType,
      List<String> recipient,
      TopicInfo topicInfo) {
    String host = yamlConfig.getHost();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.TOPIC_URI_FORMAT, host, language, topicInfo.getTopicId());
    String mobileLink =
        String.format(
            EmailConstants.TOPIC_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            topicInfo.getTopicId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.TOPICNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationType)) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .forumInfo(forumInfo)
        .sender(userName)
        .desc(String.format(EmailConstants.CONCLUSION_NOTIFICATION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.CONCLUSION_NOTIFICATION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + topicInfo.getTopicTitle())
        .content(Utility.getTextFromHtml(text))
        .to(
            userService.getEmailByUserId(
                getRecipientList(forumInfo.getForumId(), notificationType, recipient)))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_TOPIC, topicInfo.getTopicTitle()),
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName())));
  }

  private boolean addConclusionTextOfReply(
      int replyId,
      BaseConclusion conclusion,
      ForumType forumType,
      Map<String, List<UserGroupEntity>> richTextRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap)
      throws IOException {
    ReplyInfo replyInfo = new ReplyInfo();
    replyInfo.setReplyId(replyId);
    String conclusionText = conclusion.getJson();
    if (TopicType.GENERAL.equals(conclusion.getType())) {
      conclusionText =
          convertToGeneralConclusionText(
              conclusionText, forumType, richTextRoleMap, privilegeMap, "");
    }
    replyInfo.setReplyConclusionText(conclusionText);
    return replyDao.addText(replyInfo) != 0;
  }

  public int countReplyOfTopic(int topicId) {
    return replyDao.countReplyOfTopic(topicId);
  }

  public List<String> getAttachmentIdOfReply(int replyId) {
    return replyDao.getAttachmentIdOfReply(replyId);
  }

  public List<AttachmentInfo> getReplyAttachments(int replyId) {
    List<AttachmentInfo> attachments = replyDao.getReplyAttachments(replyId);
    if (isNotEmpty(attachments)) {
      Map<String, List<String>> appFieldMap =
          replyDao
              .getReplyAllAttachmentAppField(
                  attachments.stream().map(AttachmentInfo::getAttachmentId).collect(toList()),
                  AcceptLanguage.getLanguageForDb())
              .stream()
              .collect(
                  groupingBy(
                      AttachmentAppFieldEntity::getAttachmentId,
                      mapping(AttachmentAppFieldEntity::getAppFieldId, toList())));
      attachments.forEach(
          attachment ->
              attachment.setAppFieldList(
                  appFieldMap.getOrDefault(attachment.getAttachmentId(), new ArrayList<>())));
    }
    return attachments;
  }

  private List<Attachment> getReplyAttachmentList(int replyId, boolean withThumbnail) {
    List<String> attachmentIdList = getAttachmentIdOfReply(replyId);
    List<Attachment> result = fileService.getAttachmentList(attachmentIdList, withThumbnail);
    Optional.ofNullable(result)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(
            list -> {
              Map<String, List<AttachmentAppFieldEntity>> appFieldMap =
                  replyDao
                      .getReplyAllAttachmentAppField(
                          attachmentIdList, AcceptLanguage.getLanguageForDb())
                      .stream()
                      .collect(Collectors.groupingBy(AttachmentAppFieldEntity::getAttachmentId));
              Map<String, String> recordTypeMap =
                  replyDao
                      .getReplyAllAttachmentRecordType(attachmentIdList)
                      .stream()
                      .collect(toMap(i -> i.getId().toString(), IdNameEntity::getName));
              result.forEach(
                  item ->
                      item.appField(
                              appFieldMap
                                  .getOrDefault(item.getId(), Collections.emptyList())
                                  .stream()
                                  .map(
                                      appField ->
                                          new LabelValueDto()
                                              .value(appField.getAppFieldId())
                                              .label(appField.getAppFieldName()))
                                  .collect(Collectors.toList()))
                          .recordType(recordTypeMap.getOrDefault(item.getId(), EMPTY)));
            });
    return result;
  }

  public ReplyListDetail getReplyInfo(int replyId, boolean withAttachmentDetail) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    Map<String, UserSession> userMap =
        getUserDataMap(
            Arrays.asList(replyInfo.getReplyCreateUserId()).stream().collect(Collectors.toSet()));
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfo.getForumId());
    Identity identityOfForum =
        forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false);
    return getReplyListDetail(replyInfo, userMap, identityOfForum, withAttachmentDetail, forumInfo);
  }

  public ReplyListDetail updateReply(int replyId, ReplyUpdatedData replyData) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    defaultRecipient(replyData);
    validateAppField(replyInfo.getFollowTopicId(), replyData.getAttachment());
    if (ReplyStatus.LOCKED.toString().equals(replyInfo.getReplyStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (replyInfo.getReplyModifiedTime() != replyData.getModifiedTime().longValue()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }
    if (replyInfo.getFollowReplyId() == 0 && replyInfo.getReplyIndex() == 0) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_DELETE_CONCLUSION);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(replyInfo.getFollowTopicId());
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfo.getForumId());
    String userId = Utility.getUserIdFromSession();
    if (!checkCurrentUserCanUpdate(replyInfo.getReplyCreateUserId(), forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_UPDATE_REPLY);
    }
    long attachmentTotalSize = getAttachmentTotalSize(replyData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    String originalText = replyInfo.getReplyText();
    Map<String, List<UserGroupEntity>> richTextRoleMap =
        fileService.getRoleMap(userId, new ArrayList<>());

    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(replyInfo.getReplyCreateUserId(), forumAdminRoleList, forumMemberRoleList);
    ReplyInfo updatedReplyInfo =
        setUpdatedReplyInfo(
            replyInfo,
            replyData,
            ForumType.fromValue(forumInfo.getForumType()),
            richTextRoleMap,
            privilegeMap);
    if (updateReplyInfo(updatedReplyInfo)) {
      updateAttachment(replyId, replyData.getAttachment(), forumInfo);
      replyData
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
      richTextHandlerService.deleteRemovedImageInRichText(
          originalText, updatedReplyInfo.getReplyText(), replyInfo.getFollowTopicId());
      eventPublishService.publishReplyChangingEvent(
          forumInfo.getCommunityId(),
          updatedReplyInfo.getForumId(),
          updatedReplyInfo.getFollowTopicId(),
          updatedReplyInfo.getReplyModifiedUserId(),
          updatedReplyInfo.getReplyModifiedTime());
      sendReplyNotification(
          replyId,
          forumInfo,
          updatedReplyInfo.getReplyText(),
          replyData.getNotificationType(),
          replyData.getRecipient(),
          topicInfo,
          replyData.getOrgMembers(),
          replyData.getBgbus());
      publishActivityLogEventFromReply(
          userId,
          forumInfo,
          replyId,
          Operation.UPDATE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);
      Map<String, UserSession> userMap =
          getUserDataMap(
              Arrays.asList(updatedReplyInfo)
                  .stream()
                  .map(ReplyInfo::getReplyCreateUserId)
                  .collect(Collectors.toSet()));
      Identity identity = forumService.getUserIdentityOfForum(userId, forumInfo, false);
      eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(replyInfo.getFollowTopicId());
      return getReplyListDetail(updatedReplyInfo, userMap, identity, false, forumInfo);
    } else {
      throw new UpdateFailedException("");
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
      return authorId.equals(Utility.getUserIdFromSession());
    } else {
      List<User> userInForumMember = forumService.getMemberOfForumWithFilters(
              false, forumInfo.getForumId(), -1, -1, EMPTY,
              Collections.singletonList(Utility.getUserIdFromSession()),
              null, EMPTY);
      if (userInForumMember.isEmpty()) {
        return false;
      }
      return authorId.equals(Utility.getUserIdFromSession());
    }
  }

  public ReplyInfo getReplyInfoById(int replyId) {
    ReplyInfo replyInfo = replyDao.getReplyById(replyId);
    if (replyInfo == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_REPLY_NOT_EXIST);
    }
    return replyInfo;
  }

  private ReplyInfo setUpdatedReplyInfo(
      ReplyInfo replyInfo,
      ReplyUpdatedData replyData,
      ForumType forumType,
      Map<String, List<UserGroupEntity>> richTextRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap) {
    String newText =
        richTextHandlerService.replaceCopyedImageToBase64(
            replyData.getText(), replyInfo.getReplyText());
    String html =
        richTextHandlerService
            .replaceRichTextImageSrcWithImageDataHiveUrl(
                forumType, richTextRoleMap, privilegeMap, newText)
            .getText();
    replyInfo.setReplyText(richTextHandlerService.removeCacheParameter(html));
    replyInfo.setReplyModifiedTime(new Date().getTime());
    replyInfo.setReplyModifiedUserId(Utility.getUserIdFromSession());
    replyInfo.setReplyRespondee(replyData.getRespondee());
    return replyInfo;
  }

  private boolean updateReplyInfo(ReplyInfo replyInfo) {
    boolean updateInfo = replyDao.updateInfo(replyInfo) != 0;
    boolean updateText = updateText(replyInfo);
    return updateInfo && updateText;
  }

  public boolean updateText(ReplyInfo replyInfo) {
    return replyDao.updateText(replyInfo) != 0;
  }

  private void updateAttachment(
      int replyId, List<AttachmentWithAuthor> newAttachment, ForumInfo forumInfo) {
    List<String> originalAttachment = getAttachmentIdOfReply(replyId);
    List<String> newAttachmentIdList =
        newAttachment.stream().map(AttachmentWithAuthor::getId).collect(Collectors.toList());
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    String userId = Utility.getUserIdFromSession();
    Long time = Instant.now().toEpochMilli();
    originalAttachment
        .stream()
        .filter(item -> !newAttachmentIdList.contains(item))
        .forEach(
            item -> {
              replyDao.deleteAttachmentOfReply(replyId, item, userId, time);
              replyDao.deleteReplyAttachmentAppField(item);
              fileService.delete(item, DdfType.FILE.toString(), replyInfo.getFollowTopicId());
            });
    newAttachment
        .stream()
        .filter(item -> !originalAttachment.contains(item.getId()))
        .forEach(
            item -> {
              addAttachmentOfReply(replyId, item.getId());
              addReplyAttachmentAppField(
                  item.getId(),
                  item.getAppField()
                      .stream()
                      .map(appField -> appField.getValue().toString())
                      .collect(Collectors.toList()));
            });
    newAttachment
        .stream()
        .filter(item -> originalAttachment.contains(item.getId()))
        .forEach(
            item ->
                updateReplyAttachmentAppField(
                    item.getId(),
                    item.getAppField()
                        .stream()
                        .map(appField -> appField.getValue().toString())
                        .collect(Collectors.toList())));

    if (forumInfo != null) {
      originalAttachment
          .stream()
          .filter(item -> !newAttachmentIdList.contains(item))
          .forEach(
              item ->
                  publishActivityLogEventFromReply(
                      Utility.getUserIdFromSession(),
                      forumInfo,
                      replyId,
                      Operation.DELETE,
                      Constants.INFO_PROJECT_NAME,
                      Constants.CONTENT_EMPTY,
                      item));
      newAttachmentIdList
          .stream()
          .filter(item -> !originalAttachment.contains(item))
          .forEach(
              item ->
                  publishActivityLogEventFromReply(
                      Utility.getUserIdFromSession(),
                      forumInfo,
                      replyId,
                      Operation.UPLOAD,
                      Constants.INFO_PROJECT_NAME,
                      Constants.CONTENT_EMPTY,
                      item));
    } else {
      originalAttachment
          .stream()
          .filter(item -> !newAttachmentIdList.contains(item))
          .forEach(
              item ->
                  publishActivityLogEventFromConclusion(
                      Utility.getUserIdFromSession(),
                      replyId,
                      Operation.DELETE,
                      Constants.INFO_PROJECT_NAME,
                      Constants.CONTENT_EMPTY,
                      item));
      newAttachmentIdList
          .stream()
          .filter(item -> !originalAttachment.contains(item))
          .forEach(
              item ->
                  publishActivityLogEventFromConclusion(
                      Utility.getUserIdFromSession(),
                      replyId,
                      Operation.UPLOAD,
                      Constants.INFO_PROJECT_NAME,
                      Constants.CONTENT_EMPTY,
                      item));
    }
  }

  public ReplyListDetail updateConclusion(
      Integer replyId, ReplyConclusionUpdatedData conclusionData) throws IOException {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    defaultRecipient(conclusionData);
    validateAppField(replyInfo.getFollowTopicId(), conclusionData.getAttachment());
    if (ReplyStatus.LOCKED.toString().equals(replyInfo.getReplyStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (replyInfo.getReplyModifiedTime() != conclusionData.getModifiedTime()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(replyInfo.getFollowTopicId());
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfo.getForumId());
    String userId = Utility.getUserIdFromSession();
    if (!checkCurrentUserCanUpdateConclusion(
            replyInfo.getReplyCreateUserId(), forumInfo)
        || (eerpService.isEerpType(conclusionData.getTopicType())
            && StringUtils.equalsIgnoreCase(
                TopicState.CONCLUDED.toString(), topicInfo.getTopicState()))) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_UPDATE_CONCLUSION);
    }
    long attachmentTotalSize = getAttachmentTotalSize(conclusionData.getAttachment());
    if (!fileService.isFileTotalSizeValid(attachmentTotalSize)) {
      throw new MaxUploadSizeExceededException(attachmentTotalSize);
    }
    BaseConclusion conclusion =
        new ConclusionFactory()
            .getConclusion(conclusionData.getTopicType(), conclusionData.getJsonData());
    String originalText = replyInfo.getReplyConclusionText();
    TopicState topicState = sendConclusionToRemote(topicInfo, conclusion, false);
    if (!StringUtils.equalsIgnoreCase(topicInfo.getTopicState(), topicState.toString())) {
      topicService.updateTopicState(topicInfo.getTopicId(), topicState);
    }
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(forumInfo.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(forumInfo.getForumId());
    Map<String, List<UserGroupEntity>> richTextRoleMap =
        fileService.getRoleMap(userId, new ArrayList<>());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(replyInfo.getReplyCreateUserId(), forumAdminRoleList, forumMemberRoleList);
    ReplyInfo updatedReplyInfo =
        setUpdatedConclusionTextOfReply(
            replyInfo,
            conclusion,
            ForumType.fromValue(forumInfo.getForumType()),
            richTextRoleMap,
            privilegeMap,
            originalText);
    if (updateReplyInfo(updatedReplyInfo)) {
      updateAttachment(replyId, conclusionData.getAttachment(), null);
      conclusionData
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
      richTextHandlerService.deleteRemovedImageInRichText(
          originalText, updatedReplyInfo.getReplyConclusionText(), replyInfo.getFollowTopicId());
      if (Boolean.TRUE.equals(conclusionData.isSeal())) {
        topicService.sealTopic(replyInfo.getFollowTopicId());
      } else {
        topicService.unsealTopic(replyInfo.getFollowTopicId());
      }
      eventPublishService.publishReplyChangingEvent(
          forumInfo.getCommunityId(),
          updatedReplyInfo.getForumId(),
          updatedReplyInfo.getFollowTopicId(),
          updatedReplyInfo.getReplyModifiedUserId(),
          updatedReplyInfo.getReplyModifiedTime());
      sendConclusionNotification(
          replyId,
          forumInfo,
          conclusion.getText(),
          conclusionData.getNotificationType(),
          conclusionData.getRecipient(),
          topicInfo,
          conclusionData.getOrgMembers(),
          conclusionData.getBgbus());
      publishActivityLogEventFromConclusion(
          userId,
          replyId,
          Operation.UPDATE,
          Constants.INFO_PROJECT_NAME,
          Constants.CONTENT_EMPTY,
          Constants.ATTACHMENTID_EMPTY);
      Map<String, UserSession> userMap =
          getUserDataMap(
              Arrays.asList(updatedReplyInfo)
                  .stream()
                  .map(ReplyInfo::getReplyCreateUserId)
                  .collect(Collectors.toSet()));
      Identity identity = forumService.getUserIdentityOfForum(userId, forumInfo, false);
      eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(replyInfo.getFollowTopicId());
      return getReplyListDetail(updatedReplyInfo, userMap, identity, false, forumInfo);
    } else {
      throw new UpdateFailedException("");
    }
  }

  private boolean checkCurrentUserCanUpdateConclusion(
      String authorId, ForumInfo forumInfo) {
    if (privilegeService.checkUserPrivilege(
        Utility.getCurrentUserIdWithGroupId(),
        forumInfo.getCommunityId(),
        forumInfo.getForumId(),
        PermissionObject.CONCLUSION.toString(),
        Operation.UPDATE.toString())) {
      return true;
    } else if (ForumType.PUBLIC.equals(ForumType.fromValue(forumInfo.getForumType()))
        && CommunityType.ACTIVITY.equals(
            CommunityType.fromValue(
                communityService
                    .getCommunityInfoById(forumInfo.getCommunityId())
                    .getCommunityType()))) {
      return authorId.equals(Utility.getUserIdFromSession());
    } else {
      List<User> userInForumMember = forumService.getMemberOfForumWithFilters(
              false, forumInfo.getForumId(), -1, -1, org.apache.commons.lang.StringUtils.EMPTY,
              Collections.singletonList(Utility.getUserIdFromSession()),
              null, org.apache.commons.lang.StringUtils.EMPTY);
      if (userInForumMember.isEmpty()) {
        return false;
      }
      return authorId.equals(Utility.getUserIdFromSession());
    }
  }

  private ReplyInfo setUpdatedConclusionTextOfReply(
      ReplyInfo replyInfo,
      BaseConclusion conclusion,
      ForumType forumType,
      Map<String, List<UserGroupEntity>> richTextRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String originalText)
      throws IOException {
    replyInfo.setReplyModifiedUserId(Utility.getUserIdFromSession());
    replyInfo.setReplyModifiedTime(new Date().getTime());
    String conclusionText = richTextHandlerService.removeCacheParameter(conclusion.getJson());
    if (TopicType.GENERAL.equals(conclusion.getType())) {
      conclusionText =
          convertToGeneralConclusionText(
              conclusionText, forumType, richTextRoleMap, privilegeMap, originalText);
    }
    replyInfo.setReplyConclusionText(conclusionText);
    return replyInfo;
  }

  private String convertToGeneralConclusionText(
      String jsonData,
      ForumType forumType,
      Map<String, List<UserGroupEntity>> richTextRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String originalText)
      throws IOException {
    GeneralConclusionBean bean = mapper.readValue(jsonData, GeneralConclusionBean.class);
    String newText;
    if (!originalText.isEmpty()) {
      GeneralConclusionBean oldConclusion =
          mapper.readValue(originalText, GeneralConclusionBean.class);
      newText =
          richTextHandlerService.replaceCopyedImageToBase64(
              bean.getText(), oldConclusion.getText());
    } else {
      newText = richTextHandlerService.replaceCopyedImageToBase64(bean.getText(), "");
    }
    String html =
        richTextHandlerService
            .replaceRichTextImageSrcWithImageDataHiveUrl(
                forumType, richTextRoleMap, privilegeMap, newText)
            .getText();
    bean.setText(html);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bean);
  }

  public void deleteReply(int replyId, boolean deleteTopic) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    if (ReplyStatus.LOCKED.toString().equals(replyInfo.getReplyStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    if (replyInfo.getFollowReplyId() == 0 && replyInfo.getReplyIndex() == 0 && !deleteTopic) {
      throw new IllegalArgumentException(I18nConstants.MSG_CANNOT_DELETE_CONCLUSION);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(replyInfo.getFollowTopicId());
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation()) && !deleteTopic) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfo.getForumId());
    String userId = Utility.getUserIdFromSession();
    if (!checkCurrentUserCanDelete(replyInfo.getReplyCreateUserId(), forumInfo)
        && !deleteTopic) {
      throw new UnauthorizedException(I18nConstants.MSG_CANNOT_DELETE_REPLY);
    }
    long now = new Date().getTime();
    richTextHandlerService.deleteRemovedImageInRichText(replyInfo.getReplyText(), "", replyInfo.getFollowTopicId());
    getAttachmentIdOfReply(replyId)
        .forEach(
            item -> {
              publishActivityLogEventFromReply(
                  Utility.getUserIdFromSession(),
                  forumInfo,
                  replyId,
                  Operation.DELETE,
                  Constants.INFO_PROJECT_NAME,
                  Constants.CONTENT_EMPTY,
                  item);
              fileService.delete(item, DdfType.FILE.toString(), replyInfo.getFollowTopicId());
              replyDao.deleteAttachmentOfReply(replyId, item, userId, now);
            });
    replyDao.deleteReply(replyId, userId, now);
    publishActivityLogEventFromReply(
        userId,
        forumInfo,
        replyId,
        Operation.DELETE,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    eventPublishService.publishDDFDeleteQueueTriggerDeletingEvent(replyInfo.getFollowTopicId());
  }

  private boolean checkCurrentUserCanDelete(
      String authorId, ForumInfo forumInfo) {
    return checkCurrentUserOperation(authorId, forumInfo, Operation.DELETE);
  }

  public Map<String, String> getNotificationOfReply(int replyId) {
    return replyDao.getNotificationOfReply(replyId);
  }

  private void sendConcludedNotification(ForumInfo forumInfo, String text, TopicInfo topicInfo) {
    String author = topicInfo.getTopicCreateUserId();
    EmailWithChineseAndEnglishContext context =
        getConcludedNotificationContext(forumInfo, text, author, topicInfo);
    eventPublishService.publishEmailSendingEvent(context);
    eventPublishService.publishNotificationSendingEvent(
        getConcludedNotification(forumInfo, author, topicInfo));
  }

  private EmailWithChineseAndEnglishContext getConcludedNotificationContext(
      ForumInfo forumInfo, String text, String author, TopicInfo topicInfo) {
    String host = yamlConfig.getHost();
    String userId =
        Objects.equals(author, Utility.getUserIdFromSession())
            ? ""
            : topicInfo.getTopicCreateUserId();
    String userName = Utility.getUserFromSession().getCommonName();
    String language = AcceptLanguage.get();
    String link =
        String.format(EmailConstants.TOPIC_URI_FORMAT, host, language, topicInfo.getTopicId());
    String mobileLink =
        String.format(
            EmailConstants.TOPIC_URI_FORMAT,
            yamlConfig.getMobileDownloadUrl(),
            language,
            topicInfo.getTopicId());
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.CONCLUSIONMADE)
        .sender(userName)
        .desc(String.format(EmailConstants.CONCLUSION_CHINESE_FORMAT, userName))
        .englishDesc(String.format(EmailConstants.CONCLUSION_ENGLISH_FORMAT, userName))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + topicInfo.getTopicTitle())
        .content(Utility.getTextFromHtml(text))
        .to(
            userService.getEmailByUserId(
                getRecipientList(
                    forumInfo.getForumId(), NotificationType.CUSTOM, Arrays.asList(userId))))
        .link(link)
        .mobileLink(mobileLink)
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_TOPIC, topicInfo.getTopicTitle()),
                String.format(EmailConstants.TITLE_FORMAT_FORUM, forumInfo.getForumName())));
  }

  public void publishActivityLogEventFromReply(
      String userId,
      ForumInfo forumInfo,
      int replyId,
      Operation operation,
      String origin,
      String content,
      String attachmentId) {
    PermissionObject permissionObject = getPermissionObjectOfReply(forumInfo);

    eventPublishService.publishActivityLogEvent(
        Utility.setActivityLogData(
            userId,
            operation.toString(),
            permissionObject.toString(),
            replyId,
            origin,
            content,
            attachmentId));

    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            ActivityLogUtil.getAppName(origin),
            Constants.ACTIVITY_APP_VERSION,
            userId,
            ActivityLogUtil.getOperationEnumOfActivityLog(operation),
            ActivityLogUtil.getObjectType(ObjectType.REPLYID, attachmentId),
            ActivityLogUtil.getObjectId(replyId, attachmentId),
            ActivityLogUtil.getAnnotation(permissionObject, content),
            LogStatus.SUCCESS,
            LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
            ObjectType.TOPICID,
            null));
  }

  public void publishActivityLogEventFromConclusion(
      String userId,
      int replyId,
      Operation operation,
      String origin,
      String content,
      String attachmentId) {
    PermissionObject permissionObject = PermissionObject.CONCLUSION;

    eventPublishService.publishActivityLogEvent(
        Utility.setActivityLogData(
            userId,
            operation.toString(),
            permissionObject.toString(),
            replyId,
            origin,
            content,
            attachmentId));

    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            ActivityLogUtil.getAppName(origin),
            Constants.ACTIVITY_APP_VERSION,
            userId,
            ActivityLogUtil.getOperationEnumOfActivityLog(operation),
            ActivityLogUtil.getObjectType(ObjectType.REPLYID, attachmentId),
            ActivityLogUtil.getObjectId(replyId, attachmentId),
            ActivityLogUtil.getAnnotation(permissionObject, content),
            LogStatus.SUCCESS,
            LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
            ObjectType.TOPICID,
            null));
  }

  public ReplySearchResult searchReplyOfTopic(
      int topicId, int offset, int limit, Direction sortOrder, boolean withAttachmentDetail) {
    List<ReplyListDetail> replyListDetailList =
        searchReplyListOfTopic(topicId, offset, limit, sortOrder, withAttachmentDetail);
    int numFound = countReplyOfTopic(topicId);
    if (PUBLIC == ForumType.fromValue(topicService.getForumTypeById(topicId))) {
      replyListDetailList = changeToCacheUrl(topicId, replyListDetailList);
    }
    return new ReplySearchResult().result(replyListDetailList).numFound(numFound);
  }

  private List<ReplyListDetail> searchReplyListOfTopic(
      int topicId, int offset, int limit, Direction sortOrder, boolean withAttachmentDetail) {
    List<ReplyInfo> replyInfoList = getReplyListOfTopic(topicId, offset, limit, sortOrder);
    if (replyInfoList.isEmpty()) {
      return new ArrayList<>();
    }
    Map<String, UserSession> userMap =
        getUserDataMap(
            replyInfoList
                .stream()
                .map(ReplyInfo::getReplyCreateUserId)
                .collect(Collectors.toSet()));
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfoList.get(0).getForumId());
    Identity identityOfForum =
        forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false);
    return replyInfoList
        .stream()
        .map(
            item ->
                getReplyListDetail(item, userMap, identityOfForum, withAttachmentDetail, forumInfo))
        .collect(Collectors.toList());
  }

  private List<ReplyListDetail> changeToCacheUrl(int topicId, List<ReplyListDetail> replyList) {
    TopicInfo topic = topicService.getTopicInfoById(topicId);
    boolean isConcluded = ConclusionState.isConcluded(topic.getTopicState());
    return replyList
        .stream()
        .map(
            reply -> {
              if (isConcluded && INTEGER_ZERO.equals(reply.getIndex())) {
                if (GENERAL == TopicType.fromValue(topic.getTopicType())) {
                  try {
                    GeneralConclusion conclusion =
                        (GeneralConclusion)
                            new ConclusionFactory()
                                .getConclusion(
                                    TopicType.fromValue(topic.getTopicType()), reply.getText());
                    GeneralConclusionBean text = conclusion.getBean();
                    conclusion.setBean(
                        text.setText(richTextHandlerService.changeToCacheUrl(text.getText())));
                    reply.setText(conclusion.getJson());
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }

              } else {
                reply.text(richTextHandlerService.changeToCacheUrl(reply.getText()));
              }
              return reply;
            })
        .collect(toList());
  }

  private Map<String, UserSession> getUserDataMap(Set<String> userIdList) {
    return userService
        .getUserById(userIdList.stream().collect(Collectors.toList()), new ArrayList<>())
        .stream()
        .collect(Collectors.toMap(UserSession::getCommonUUID, Function.identity()));
  }

  private ReplyListDetail getReplyListDetail(
      ReplyInfo replyInfo,
      Map<String, UserSession> userMap,
      Identity identity,
      boolean withAttachmentDetail,
      ForumInfo forumInfo) {
    Map<String, String> notification = getNotificationOfReply(replyInfo.getReplyId());

    UserSession userSession =
        userMap.get(replyInfo.getReplyCreateUserId()) == null
            ? new UserSession()
            : userMap.get(replyInfo.getReplyCreateUserId());
    userSession
        .certificates(
            medalDao.getCertificationByIdAndType(
                replyInfo.getReplyCreateUserId(), MedalType.USER.toString()))
        .medal(replyInfo.getMedalFrame())
        .medalId(replyInfo.getMedalId())
        .title(replyInfo.getMedalTitle());

    ReplyListDetail replyListDetail =
        new ReplyListDetail()
            .id(replyInfo.getReplyId())
            .index(replyInfo.getReplyIndex())
            .author(userSession)
            .status(ReplyStatus.fromValue(replyInfo.getReplyStatus()))
            .createTime(replyInfo.getReplyCreateTime());
    if (ReplyStatus.DELETE.equals(replyListDetail.getStatus())) {
      replyListDetail.text(
          getDeletedText(replyInfo.getReplyDeleteUserId(), replyInfo.getReplyDeleteTime()));
    } else {
      User respondee =
          StringUtils.isEmpty(replyInfo.getReplyRespondee())
              ? new User()
              : userService.getUserById(replyInfo.getReplyRespondee());
      String replyText =
          replyInfo.getReplyIndex() == 0
              ? replyInfo.getReplyConclusionText()
              : replyInfo.getReplyText();
      replyListDetail
          .text(replyInfo.getFollowReplyId() == 0 ? replyText : replyInfo.getReplyText())
          .attachment(getReplyAttachmentList(replyInfo.getReplyId(), withAttachmentDetail))
          .identity(
              getUserIdentityOfReply(
                  Utility.getUserIdFromSession(), identity, replyInfo.getReplyCreateUserId()))
          .emoji(getEmojiOfReply(replyInfo.getReplyId()))
          .userEmoji(getUserEmojiOfReply(replyInfo.getReplyId(), Utility.getUserIdFromSession()))
          .modifiedTime(replyInfo.getReplyModifiedTime())
          .notificationType(notification.get(NOTIFICATION_TYPE))
          .recipient(
              userService.splitRecipient(Arrays.asList(new User().id(notification.get(RECIPIENT)))))
          .respondee(respondee)
          .kidNumFound(getKidNumFound(replyInfo.getReplyId()))
          .orgMembers(
              isPublicForum(forumInfo.getForumType())
                  ? getOrgMembers(replyInfo.getReplyId())
                  : Collections.emptyList())
          .bgbus(
              isPublicForum(forumInfo.getForumType())
                  ? getBgbus(replyInfo.getReplyId())
                  : Collections.emptyList());
    }
    return replyListDetail;
  }

  private boolean isPublicForum(String forumType) {
    return StringUtils.equalsIgnoreCase(ForumType.PUBLIC.toString(), forumType);
  }

  protected List<SimpleGroupWithUsers> getOrgMembers(int replyId) {
    List<OrgIdWithUsers> orgMemberList = replyDao.getOrgMemberNotificationOfReply(replyId);
    return convertToSimpleGroupWithUsers(orgMemberList);
  }

  protected List<SimpleGroupWithUsers> getBgbus(int replyId) {
    List<OrgIdWithUsers> bgbusList = replyDao.getBgbuNotificationOfReply(replyId);
    return convertToSimpleGroupWithUsers(bgbusList);
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

  private Identity getUserIdentityOfReply(String userId, Identity identity, String authorId) {
    if (Identity.OWNER.equals(identity)) {
      return identity;
    } else if (Identity.MEMBER.equals(identity)) {
      return Utility.checkUserIsAuthor(authorId, userId) ? Identity.AUTHOR : identity;
    } else {
      return identity;
    }
  }

  private int getKidNumFound(int replyId) {
    return replyDao.countKidNumOfReply(replyId);
  }

  private String getDeletedText(String userId, long deleteTime) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(DELETE_TIME_FORMAT, Locale.TRADITIONAL_CHINESE);
    ZonedDateTime zdt =
        ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(deleteTime), ZoneId.of(TimeZone.getDefault().getID()));
    return String.format(
        DELETE_REPLY_TEXT_FORMAT, formatter.format(zdt), userService.getUserById(userId).getName());
  }

  private Map<String, Integer> getEmojiOfReply(int replyId) {
    List<Map<String, Object>> emojiMap = emojiService.getEmojiOfReply(replyId);
    return emojiService.transferEmojiMap(emojiMap);
  }

  private String getUserEmojiOfReply(int replyId, String userId) {
    String userEmoji = emojiService.getEmojiOfReplyByUser(replyId, userId);
    return userEmoji == null ? "" : userEmoji;
  }

  public EmojiResult getEmojiDetailOfReply(
      int replyId, Emoji emoji, int offset, int limit, Order sort) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfo.getForumId());
    if (!topicService.checkCurrentUserCanRead(forumInfo)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_PRIVATE_FORUM);
    }
    return new EmojiResult()
        .numFound(getEmojiOfReply(replyId).get(emoji.toString()))
        .result(
            emojiService.getEmojiDetailOfReply(replyId, emoji, offset, limit, sort.getDirection()));
  }

  public int setEmojiOfReply(int replyId, Emoji emoji) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    if (ReplyStatus.LOCKED.toString().equals(replyInfo.getReplyStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(replyInfo.getFollowTopicId());
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    int row = emojiService.setEmojiOfReply(replyId, emoji);

    try {
      UserEchoAPI.set(
          Utility.getUserIdFromSession(), Echo.LIKE, ObjectType.REPLYID, String.valueOf(replyId));
    } catch (UserEchoException e) {
      log.warn(e);
    }
    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            App.COMMUNITY,
            Constants.ACTIVITY_APP_VERSION,
            Utility.getUserIdFromSession(),
            Activity.LIKE,
            ObjectType.REPLYID,
            String.valueOf(replyId),
            null,
            null,
            null,
            ObjectType.TOPICID,
            String.valueOf(replyInfo.getFollowTopicId())));

    if (row > 0) {
      return HttpStatus.OK.value();
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
  }

  public int removeEmojiOfReply(int replyId) {
    ReplyInfo replyInfo = getReplyInfoById(replyId);
    if (ReplyStatus.LOCKED.toString().equals(replyInfo.getReplyStatus())) {
      throw new UnauthorizedException(I18nConstants.MSG_COMMUNITY_LOCKED);
    }
    TopicInfo topicInfo = topicService.getTopicInfoById(replyInfo.getFollowTopicId());
    if (TopicSituation.SEALED.toString().equals(topicInfo.getTopicSituation())) {
      throw new UnauthorizedException(I18nConstants.MSG_TOPIC_SEALED);
    }
    int row = emojiService.removeEmojiOfReply(replyId);

    try {
      UserEchoAPI.remove(
          Utility.getUserIdFromSession(),
          EchoGroup.EMOTION,
          ObjectType.REPLYID,
          String.valueOf(replyId));
    } catch (UserEchoException e) {
      log.warn(e);
    }
    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            App.COMMUNITY,
            Constants.ACTIVITY_APP_VERSION,
            Utility.getUserIdFromSession(),
            Activity.UNLIKE,
            ObjectType.REPLYID,
            String.valueOf(replyId),
            null,
            null,
            null,
            ObjectType.TOPICID,
            String.valueOf(replyInfo.getFollowTopicId())));

    if (row > 0) {
      return HttpStatus.OK.value();
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
  }

  public ReplySearchResult searchReplyOfReply(
      int replyId, int offset, int limit, Direction sortOrder, boolean withAttachmentDetail) {
    List<ReplyListDetail> replyListDetailList =
        searchReplyListOfReply(replyId, offset, limit, sortOrder, withAttachmentDetail);
    int numFound = countReplyOfReply(replyId);
    return new ReplySearchResult().result(replyListDetailList).numFound(numFound);
  }

  public List<ReplyListDetail> searchReplyListOfReply(
      int replyId, int offset, int limit, Direction sortOrder, boolean withAttachmentDetail) {
    List<ReplyInfo> replyInfoList = getReplyListOfReply(replyId, offset, limit, sortOrder);
    if (replyInfoList.isEmpty()) {
      return new ArrayList<>();
    }
    Map<String, UserSession> userMap =
        getUserDataMap(
            replyInfoList
                .stream()
                .map(ReplyInfo::getReplyCreateUserId)
                .collect(Collectors.toSet()));
    ForumInfo forumInfo = forumService.getForumInfoById(replyInfoList.get(0).getForumId());
    Identity identityOfForum =
        forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false);
    return replyInfoList
        .stream()
        .map(
            item ->
                getReplyListDetail(item, userMap, identityOfForum, withAttachmentDetail, forumInfo))
        .collect(Collectors.toList());
  }

  public List<ReplyInfo> getReplyListOfReply(
      int replyId, int offset, int limit, Direction sortOrder) {
    return replyDao.getReplyListOfReply(replyId, offset, limit, sortOrder.toString());
  }

  public int countReplyOfReply(int replyId) {
    return replyDao.countReplyOfReply(replyId);
  }

  public List<IdNameEntity> getAttachmentAppField(String ddfId, String lang) {
    return replyDao.getReplyAttachmentAppField(ddfId, lang);
  }

  public void validateReplyPrivilege(ReplyOperation operation, int replyId) {
    getReplyInfoById(replyId); // throw exception if reply deleted
    replyPrivilegeService.validatePrivilege(operation, replyId);
  }

  private void putTopicReplyReportRow(List<Map<String, String>> excelRowMapList, Integer replyFloor,
                                      Integer replySubFloor, String commentAuthor, Integer likeCount,
                                      String employeeEmail, String department, String office,
                                      String commentText, Long createTime, Long modifiedTime) {
    Map<String, String> rowMap = new HashMap<>();
    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(COMMON_DATE_FORMAT);
    String likeCountString = "";
    if (likeCount >= 0) {
      likeCountString = Integer.toString(likeCount);
    }
    rowMap.put(ExcelReplyReportHeaderRaw.REPLY_FLOOR.toString(), Integer.toString(replyFloor));
    rowMap.put(ExcelReplyReportHeaderRaw.REPLY_SUB_FLOOR.toString(), Integer.toString(replySubFloor));
    rowMap.put(ExcelReplyReportHeaderRaw.COMMENT_AUTHOR.toString(), commentAuthor);
    rowMap.put(ExcelReplyReportHeaderRaw.LIKE_COUNT.toString(), likeCountString);
    rowMap.put(ExcelReplyReportHeaderRaw.EMPLOYEE_EMAIL.toString(), employeeEmail);
    rowMap.put(ExcelReplyReportHeaderRaw.DEPARTMENT.toString(), department);
    rowMap.put(ExcelReplyReportHeaderRaw.OFFICE.toString(), office);
    rowMap.put(ExcelReplyReportHeaderRaw.COMMENT_TEXT.toString(), Jsoup.parse(commentText).wholeText());
    rowMap.put(ExcelReplyReportHeaderRaw.CREATE_TIME.toString(), convertToDateTime(dateFormat, createTime));
    rowMap.put(ExcelReplyReportHeaderRaw.MODIFIED_TIME.toString(), convertToDateTime(dateFormat, modifiedTime));
    excelRowMapList.add(rowMap);
  }


  public void putAllTopicReplyReportRow(TopicInfo topicInfo,
                                        List<Map<String, String>> excelRowMapList)
  throws Exception{
    int topicId = topicInfo.getTopicId();
    // TODO: 當留言數量太多時可能超過這邊Hard-coded value的數字，不過目前系統應該是暫無此問題
    List<ReplyListDetail> replyDetails = this.searchReplyListOfTopic(
            topicId, 0, MAX_REPLY_FETCH_LIMIT,
            Sort.Direction.DESC, false
    );
    // 留言的SQL結構雖然可以儲存無限層結構的留言，但是在實作上只有存兩層，不過這邊還是以能無限層的方式處理，擴充性比較好
    LinkedList<Pair<ReplyListDetail, ReplyFloor>> replyDetailStack = replyDetails.stream()
            .map(r -> Pair.of(r, ReplyFloor.of(r.getIndex(), 0)))
            .collect(Collectors.toCollection(LinkedList::new));
    while (!replyDetailStack.isEmpty()) {
      Pair<ReplyListDetail, ReplyFloor> tempPair = replyDetailStack.pop();;
      ReplyListDetail replyListDetail = tempPair.getFirst();
      ReplyFloor floor = tempPair.getSecond();
      // 回覆的內容會有比較多情況需要處理，作為結論的回覆，其儲存是json，這邊要處理，並且只有回覆有附件就要後綴[附件]、結論要前綴[結論]
      // 那些前綴和後綴都要套用i18n
      String replyContent = replyListDetail.getText();
      // 結論的樓層為0，子回覆的floor為非0值，但原本儲存的index為0
      if (floor.getFloor() == REPLY_CONCLUSION_INDEX) {
        replyContent = new ConclusionFactory()
                .getConclusion(TopicType.fromValue(topicInfo.getTopicType()), replyContent).getExcelText(messageSource)
                .replaceAll(HTML_BR, LINE_BREAKS);
        replyContent = String.format(REPLY_WITH_PREFIX_FORMAT,
                messageSource.getMessage(String.valueOf(I18nEnum.EXCEL_REPLY_REPORT_CONCLUSION),
                        null, LocaleContextHolder.getLocale()),
                replyContent);
      }
      // 處理rich text中的表格
      if (REPLAY_RICH_TEXT_TABLE_MATCH_REGEX_PATTERN.matcher(replyListDetail.getText()).find()) {
        replyContent = String.format(REPLY_WITH_POSTFIX_FORMAT, replyContent, messageSource.getMessage(
                String.valueOf(I18nEnum.EXCEL_REPLY_REPORT_TABLE), null, LocaleContextHolder.getLocale()
        ));
      }
      // 附件像是圖片或影片可以在網頁那邊編輯時，用richtext的方式插入，所以也要判定那種情況，輸出[附件]
      if ((replyListDetail.getAttachment() != null && !replyListDetail.getAttachment().isEmpty()) ||
              REPLAY_RICH_TEXT_ATTACHMENT_PATTERN.matcher(replyListDetail.getText()).find()) {
        replyContent = String.format(REPLY_WITH_POSTFIX_FORMAT, replyContent, messageSource.getMessage(
                String.valueOf(I18nEnum.EXCEL_REPLY_REPORT_ATTACHMENT), null, LocaleContextHolder.getLocale()
        ));
      }
      // 結論無法被刪除，所以不用管結論可能是被刪除的狀態。並且結論的index一定為0，其他回覆在第幾層，index就是多少
      if (!replyListDetail.getStatus().equals(ReplyStatus.DELETE)) {
        putTopicReplyReportRow(excelRowMapList, floor.getFloor(), floor.getSubFloor(),
                replyListDetail.getAuthor().getCommonName(), replyListDetail.getEmoji().get(LIKE.toString()),
                replyListDetail.getAuthor().getProfileMail(), replyListDetail.getAuthor().getProfileDeptName(),
                replyListDetail.getAuthor().getProfileOffice(), replyContent,
                replyListDetail.getCreateTime(), replyListDetail.getModifiedTime());
      }
      else {
        // 刪除的留言在匯出時只顯示層數、作者與內容欄位
        putTopicReplyReportRow(excelRowMapList, floor.getFloor(), floor.getSubFloor(),
                "", EMPTY_LIKE_COUNT_VALUE, "", "", "",
                replyContent, EMPTY_DATE_TIME_VALUE, EMPTY_DATE_TIME_VALUE);
      }
      if (replyListDetail.getKidNumFound() > 0) {
        int curSubFloor = 1;
        // TODO: 當留言數量太多時可能超過這邊Hard-coded value的數字，不過目前系統應該是暫無此問題
        List<ReplyListDetail> replies =  this.searchReplyListOfReply(
                replyListDetail.getId(), 0, MAX_REPLY_FETCH_LIMIT,
                Sort.Direction.ASC, false
        );
        for (ReplyListDetail r: replies) {
          replyDetailStack.push(Pair.of(r, ReplyFloor.of(replyListDetail.getIndex(), curSubFloor++)));
        }
      }
    }
  }

  private Notification getNotification(
      ForumInfo forumInfo,
      NotificationType notificationType,
      List<String> recipient,
      TopicInfo topicInfo) {
    return new Notification()
        .userId(
            getRecipientList(forumInfo.getForumId(), notificationType, recipient)
                .stream()
                .collect(Collectors.joining(Constants.COMMA_DELIMITER)))
        .type(EmailType.TOPICNOTIFICATION)
        .extraMemberType((NotificationType.ALL.equals(notificationType)) ? EmailMemberType.ALLFORUMMEMBER : EmailMemberType.NONE)
        .title(topicInfo.getTopicTitle())
        .senderId(Utility.getUserIdFromSession())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .topicId(topicInfo.getTopicId())
        .topicTitle(topicInfo.getTopicTitle())
        .topicType(TopicType.fromValue(topicInfo.getTopicType()))
        .time(new Date().getTime());
  }

  private Notification getConcludedNotification(
      ForumInfo forumInfo, String author, TopicInfo topicInfo) {
    String userId =
        Objects.equals(author, Utility.getUserIdFromSession())
            ? ""
            : topicInfo.getTopicCreateUserId();
    return new Notification()
        .userId(userId)
        .type(EmailType.CONCLUSIONMADE)
        .title(topicInfo.getTopicTitle())
        .senderId(Utility.getUserIdFromSession())
        .forumId(forumInfo.getForumId())
        .forumName(forumInfo.getForumName())
        .topicId(topicInfo.getTopicId())
        .topicTitle(topicInfo.getTopicTitle())
        .topicType(TopicType.fromValue(topicInfo.getTopicType()))
        .time(new Date().getTime());
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

  private TopicState sendConclusionToRemote(
      TopicInfo topicInfo, BaseConclusion conclusion, boolean ignoreError) throws IOException {
    try {
      if (TopicType.SYSTEM.toString().equals(topicInfo.getTopicType())) {
        eerpService.sendConclusionToPqm(topicInfo, conclusion.getJson());
        return TopicState.CONCLUDED;
      }
      if (eerpService.isEerpmType(TopicType.fromValue(topicInfo.getTopicType()))) {
        if (eerpService.isEerpConcluded(conclusion.getRawJson())) {
          eerpService.sendConclusionToEerpm(topicInfo, conclusion.getJson());
          return TopicState.CONCLUDED;
        }
        return TopicState.BRIEFCONCLUDED;
      }
      if (TopicType.EERPPGENERAL.toString().equals(topicInfo.getTopicType())) {
        eerpService.sendConclusionToEerpp(topicInfo);
        return TopicState.CONCLUDED;
      }
      return TopicState.CONCLUDED;
    } catch (Exception e) {
      log.error(e);
      if (ignoreError) {
        return TopicState.CONCLUDED;
      }
      throw e;
    }
  }

  private void addReplyAttachmentRecordType(Map<String, String> attachmentMap) {
    Optional.ofNullable(attachmentMap)
        .filter(MapUtils::isNotEmpty)
        .ifPresent(list -> replyDao.insertReplyAttachmentRecordType(attachmentMap));
  }

  private void archiveConclusionAttachments(
      TopicInfo topic, int conclusionId, List<AttachmentWithAuthor> attachments) {
    if (!StringUtils.equals(TopicType.EERPMHIGHLEVEL.toString(), topic.getTopicType())) {
      return;
    }
    if (isEmpty(attachments)) {
      throw new IllegalArgumentException(MSG_CONCLUSION_EMPTY);
    }

    fileArchiveQueueDao.upsertQueue(FileArchiveType.EERPMHIGHLEVEL, Integer.toString(conclusionId));
  }

  private void validateAppField(int topicId, List<AttachmentWithAuthor> attachmentList) {
    if (SourceOsParam.isMobile()
        && CollectionUtils.isEmpty(topicService.getTopicAppField(topicId))) {
      return;
    }
    if (attachmentList
        .parallelStream()
        .map(AttachmentWithAuthor::getAppField)
        .anyMatch(CollectionUtils::isEmpty)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  private void defaultRecipient(ReplyCreationData reply) {
    reply.setRecipient(defaultList(reply.getRecipient()));
  }

  private void defaultRecipient(ReplyUpdatedData reply) {
    reply.setRecipient(defaultList(reply.getRecipient()));
  }

  private void defaultRecipient(ReplyConclusionCreationData reply) {
    reply.setRecipient(defaultList(reply.getRecipient()));
  }

  private void defaultRecipient(ReplyConclusionUpdatedData reply) {
    reply.setRecipient(defaultList(reply.getRecipient()));
  }

  private List<String> defaultList(List<String> list) {
    return ofNullable(list).orElseGet(ArrayList::new);
  }
}
