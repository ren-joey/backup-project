package com.delta.dms.community.service;

import static java.util.Optional.ofNullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.enums.Role;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.dao.entity.TopicTypeEntity;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.swagger.model.Attachment;
import com.delta.dms.community.swagger.model.CommunityCategory;
import com.delta.dms.community.swagger.model.CommunityHomePage;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.CommunityType;
import com.delta.dms.community.swagger.model.FilterSource;
import com.delta.dms.community.swagger.model.ForumHomePage;
import com.delta.dms.community.swagger.model.ForumStatus;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.Identity;
import com.delta.dms.community.swagger.model.KeyLabelDto;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.SupportTopicType;
import com.delta.dms.community.swagger.model.TopicHomePage;
import com.delta.dms.community.swagger.model.TopicSituation;
import com.delta.dms.community.swagger.model.TopicState;
import com.delta.dms.community.swagger.model.TopicStatus;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;

@Service
@Transactional
public class HomePageService {

  private CommunityService communityService;
  private ForumService forumService;
  private TopicService topicService;
  private UserService userService;
  private EmojiService emojiService;
  private EventPublishService eventPublishService;
  private AuthService authService;
  private YamlConfig yamlConfig;

  @Autowired
  public HomePageService(
      CommunityService communityService,
      ForumService forumService,
      TopicService topicService,
      UserService userService,
      EmojiService emojiService,
      EventPublishService eventPublishService,
      AuthService authService,
      YamlConfig yamlConfig) {
    this.communityService = communityService;
    this.forumService = forumService;
    this.topicService = topicService;
    this.userService = userService;
    this.emojiService = emojiService;
    this.eventPublishService = eventPublishService;
    this.authService = authService;
    this.yamlConfig = yamlConfig;
  }

  public CommunityHomePage getCommunityHomePage(int communityId) {
	//dl人員檢查，如果是dl且community id不在白名單裡，就回403
	if (authService.checkDLCommunityAuth(Integer.toString(communityId))==false)
		throw new HttpClientErrorException(HttpStatus.FORBIDDEN); 
    CommunityInfo communityInfo = getCommunityInfoById(communityId);
    int numberOfForum =
        forumService.countForumOfCommunity(
            communityId, Arrays.stream(ForumType.values()).collect(Collectors.toList()));
    List<String> dependentGroupList =
        communityService.getCommunityDependentGroupList(communityId);

    int numOfAdmin = communityService.gatherAllCommunityMemberCount(communityId,
            null, null, Role.COMMUNITY_ADMIN.getId());
    int numOfMember = communityService.gatherAllCommunityMemberCount(communityId,
            null, null, Role.COMMUNITY_MEMBER.getId());
/*
    List<User> adminList =
        communityService.getAdminListOfCommunity(
                communityId,null, null,
                Constants.COMMUNITY_LIST_MEMBER_DISPLAY_NUM);
    List<User> memberList =
        communityService.getAllMemberOfCommunityById(
                communityId,null, null, Role.COMMUNITY_MEMBER.getId(),
                Constants.COMMUNITY_LIST_MEMBER_DISPLAY_NUM);
 */
    List<User> currentUser =
        communityService.getAllMemberOfCommunityById(
                communityId,null,
                Collections.singletonList(Utility.getUserIdFromSession()),
                Role.COMMUNITY_MEMBER.getId(), -1);

    int numberOfTopic = topicService.countTopicOfCommunity(communityId);
    eventPublishService.publishActivityLogEvent(
        Utility.setActivityLogData(
            Utility.getUserIdFromSession(),
            Operation.READ.toString(),
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
            Activity.READ,
            ObjectType.COMMUNITYID,
            String.valueOf(communityId),
            ActivityLogUtil.getAnnotation(PermissionObject.COMMUNITY, Constants.CONTENT_EMPTY),
            LogStatus.SUCCESS,
            LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
            null,
            null));

    return new CommunityHomePage()
        .id(communityInfo.getCommunityId())
        .name(communityInfo.getCommunityName())
        .desc(communityInfo.getCommunityDesc())
        .type(CommunityType.fromValue(communityInfo.getCommunityType()))
        .category(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .status(CommunityStatus.fromValue(communityInfo.getCommunityStatus()))
        //.admins(adminList)
        //.members(memberList)
        .numberOfMember(numOfMember)
        .numberOfAdmin(numOfAdmin)
        .numberOfForum(numberOfForum)
        .numberOfTopic(numberOfTopic)
        .identity(getCurrentUserIdentityOfCommunity(communityInfo))
        .communityModifiedTime(communityInfo.getCommunityModifiedTime())
        .communityCreateTime(communityInfo.getCommunityCreateTime())
        .groupId(communityInfo.getCommunityGroupId())
        .dependentGroupCount(dependentGroupList.size())
        .notificationType(communityService.getCommunityNotificationType(communityId))
        .filter(getCommunityFilterOptionList(communityId))
        .dashboard(
            communityInfo.isDashboard()
                && !currentUser.isEmpty());
  }

  private CommunityInfo getCommunityInfoById(int communityId) {
    CommunityInfo communityInfo = communityService.getCommunityInfoById(communityId);
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
    return communityInfo;
  }

  private Identity getCurrentUserIdentityOfCommunity(CommunityInfo communityInfo) {
    return communityService.getUserIdentityOfCommunity(
        Utility.getUserIdFromSession(),
        communityInfo.getCommunityId(),
        communityInfo.getCommunityGroupId());
  }

  public ForumHomePage getForumHomePage(int forumId) {
	//dl人員檢查，如果是dl且forum id不在白名單裡，就回403
	if (authService.checkDLForumAuth(Integer.toString(forumId))==false)
		throw new HttpClientErrorException(HttpStatus.FORBIDDEN); 
    ForumInfo forumInfo = forumService.getForumInfoById(forumId);
    CommunityInfo communityInfo = getCommunityInfoById(forumInfo.getCommunityId());
    /*eventPublishService.publishSyncForumUserIdInHomePageEvent(
        forumInfo.getCommunityId(), forumId, communityInfo.getCommunityGroupId()); */
    int numOfAdmin = forumService.getAdminCountOfForum(forumInfo.getForumId());
    int numOfMember = forumService.getMemberCountOfForum(forumInfo.getForumId());
    List<User> adminList = forumService.getAdminListOfForum(forumInfo.getForumId(), 0, Constants.FORUM_LIST_MEMBER_DISPLAY_NUM);
    List<User> memberList = forumService.getMemberOfForum(forumId, 0, Constants.FORUM_LIST_MEMBER_DISPLAY_NUM);

    forumService.publishActivityLogEventFromForum(
        Utility.getUserIdFromSession(),
        forumInfo,
        Operation.READ,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    return new ForumHomePage()
        .id(forumInfo.getForumId())
        .name(forumInfo.getForumName())
        .desc(forumInfo.getForumDesc())
        .type(ForumType.fromValue(forumInfo.getForumType()))
        .status(ForumStatus.fromValue(forumInfo.getForumStatus()))
        .numberOfAdmin(numOfAdmin)
        .numberOfMember(numOfMember)
        .admins(adminList)
        .members(memberList)
        .communityId(communityInfo.getCommunityId())
        .communityName(communityInfo.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(communityInfo.getCommunityCategory()))
        .identity(
            forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false))
        .tag(forumService.getTagOfForum(forumInfo.getForumId()))
        .forumModifiedTime(forumInfo.getForumModifiedTime())
        .toppingOrder(forumInfo.getForumToppingOrder())
        .supportTopicType(
            forumInfo
                .getSupportTopicType()
                .stream()
                .map(this::convertToSupportTopicType)
                .collect(Collectors.toList()))
        .conclusionAlert(forumInfo.isConclusionAlert())
        .filter(getForumFilterOptionList(forumInfo.getCommunityId(), forumId));
  }

  public TopicHomePage getTopicHomePage(int topicId, boolean withAttachmentDetail) {
    TopicHomePage topicHomePage = convertToTopicHomePage(topicService.getTopicHomePage(topicId));
	//dl人員檢查，如果是dl且forum id不在白名單裡，就回403
	if (authService.checkDLForumAuth(Integer.toString(topicHomePage.getForumId()))==false)
		throw new HttpClientErrorException(HttpStatus.FORBIDDEN); 
    List<UserSession> userList =
        userService.getUserById(
            Arrays.asList(topicHomePage.getAuthor().getCommonUUID()), new ArrayList<>());
    if (!userList.isEmpty()) {
      topicHomePage.setAuthor(userList.get(0));
    }
    ForumInfo forumInfo = forumService.getForumInfoById(topicHomePage.getForumId());
    /*
    List<User> adminList = forumService.getAdminListOfForum(
            topicHomePage.getForumId(), 0, Constants.TOPIC_LIST_MEMBER_DISPLAY_NUM);
    List<User> memberList = forumService.getMemberOfForum(
            topicHomePage.getForumId(), 0, Constants.TOPIC_LIST_MEMBER_DISPLAY_NUM);
    */
    Identity identity =
        forumService.getUserIdentityOfForum(Utility.getUserIdFromSession(), forumInfo, false);
    //topicHomePage.setAdmins(adminList);
    //topicHomePage.setMembers(memberList);
    topicHomePage.setIdentity(
        getCurrentUserIdentityOfTopic(identity, topicHomePage.getAuthor().getCommonUUID()));
    topicHomePage.setEmoji(getEmojiOfTopic(topicId));
    topicHomePage.setUserEmoji(getUserEmojiOfTopic(topicId, Utility.getUserIdFromSession()));
    topicHomePage.setTag(topicService.getTagOfTopic(topicId));
    topicHomePage.setAppField(
        topicService
            .getTopicAppField(topicId)
            .stream()
            .map(item -> new LabelValueDto().value(item.getId()).label(item.getName()))
            .collect(Collectors.toList()));
    topicHomePage.setAttachment(getAttachmentOfTopic(topicId, withAttachmentDetail));
    topicHomePage.setNotificationType(
        topicHomePage.getNotificationType() == null ? "" : topicHomePage.getNotificationType());
    topicHomePage.setRecipient(userService.splitRecipient(topicHomePage.getRecipient()));
    topicHomePage.setOrgMembers(
        forumService.isPublicForum(forumInfo.getForumType())
            ? topicService.getOrgMembers(topicId)
            : Collections.emptyList());
    topicHomePage.setBgbus(
        forumService.isPublicForum(forumInfo.getForumType())
            ? topicService.getBgbus(topicId)
            : Collections.emptyList());
    topicService.addViewCount(topicId);
    topicHomePage.setUserBookmark(
        topicService.checkUserBookmark(Utility.getUserIdFromSession(), topicId, forumInfo));
    CommunityInfo communityInfo = getCommunityInfoById(topicHomePage.getCommunityId());
    topicHomePage.setCommunityName(communityInfo.getCommunityName());
    topicHomePage.setForumType(ForumType.fromValue(forumInfo.getForumType()));
    topicService.publishActivityLogEventFromTopic(
        Utility.getUserIdFromSession(),
        forumInfo.getForumType(),
        topicId,
        Operation.READ,
        Constants.INFO_PROJECT_NAME,
        Constants.CONTENT_EMPTY,
        Constants.ATTACHMENTID_EMPTY);
    return topicHomePage;
  }

  public Identity getCurrentUserIdentityOfTopic(Identity identity, String authorId) {
    return topicService.getUserIdentityOfTopic(Utility.getUserIdFromSession(), identity, authorId);
  }

  private Map<String, Integer> getEmojiOfTopic(int topicId) {
    List<Map<String, Object>> emojiMap = emojiService.getEmojiOfTopic(topicId);
    return emojiService.transferEmojiMap(emojiMap);
  }

  private String getUserEmojiOfTopic(int topicId, String userId) {
    String userEmoji = emojiService.getEmojiOfTopicByUser(topicId, userId);
    return userEmoji == null ? "" : userEmoji;
  }

  private List<Attachment> getAttachmentOfTopic(int topicId, boolean withThumbnail) {
    return topicService.getTopicAttachmentList(topicId, withThumbnail);
  }

  private Map<String, List<KeyLabelDto>> getCommunityFilterOptionList(int communityId) {
    Map<String, List<KeyLabelDto>> result = new HashMap<>();
    List<TopicTypeEntity> topicTypeList = topicService.getTopicTypeByCommunityId(communityId);
    result.put(
        FilterSource.FORUM.toString(),
        forumService
            .getPrivilegedForumOfCommunity(communityId)
            .stream()
            .map(
                item -> new KeyLabelDto().key(Integer.toString(item.getId())).label(item.getName()))
            .collect(Collectors.toList()));
    result.put(FilterSource.TOPICTYPE.toString(), convertToKeyLabelDtoList(topicTypeList));
    result.put(FilterSource.CONCLUSIONSTATE.toString(), getConclusionStateFilter(topicTypeList));
    return result;
  }

  private Map<String, List<KeyLabelDto>> getForumFilterOptionList(int communityId, int forumId) {
    Map<String, List<KeyLabelDto>> result = new HashMap<>();
    List<TopicTypeEntity> topicTypeList =
        topicService.getTopicTypeByCommunityIdAndForumId(communityId, forumId);
    result.put(FilterSource.TOPICTYPE.toString(), convertToKeyLabelDtoList(topicTypeList));
    result.put(FilterSource.CONCLUSIONSTATE.toString(), getConclusionStateFilter(topicTypeList));
    return result;
  }

  private List<KeyLabelDto> getConclusionStateFilter(List<TopicTypeEntity> topicTypeList) {
    return topicService
        .getConclusionStateByTopicType(
            topicTypeList
                .parallelStream()
                .map(TopicTypeEntity::getTopicTypeId)
                .collect(Collectors.toList()))
        .stream()
        .map(item -> new KeyLabelDto().key(item.getTopicConclusionState()).label(item.getName()))
        .collect(Collectors.toList());
  }

  private List<KeyLabelDto> convertToKeyLabelDtoList(List<TopicTypeEntity> topicTypeList) {
    return topicTypeList
        .stream()
        .map(item -> new KeyLabelDto().key(item.getTopicType()).label(item.getName()))
        .collect(Collectors.toList());
  }

  private TopicHomePage convertToTopicHomePage(TopicInfo topicInfo) {
    TopicInfo data = Optional.ofNullable(topicInfo).orElseThrow(NoSuchElementException::new);
    return new TopicHomePage()
        .id(data.getTopicId())
        .forumId(data.getForumId())
        .forumName(data.getForumName())
        .communityId(data.getCommunityId())
        .communityName(data.getCommunityName())
        .communityCategory(CommunityCategory.fromValue(data.getCommunityCategory()))
        .communityType(CommunityType.fromValue(data.getCommunityType()))
        .title(data.getTopicTitle())
        .status(TopicStatus.fromValue(data.getTopicStatus()))
        .state(TopicState.fromValue(data.getTopicState()))
        .situation(TopicSituation.fromValue(data.getTopicSituation()))
        .type(TopicType.fromValue(data.getTopicType()))
        .author(
            new UserSession()
                .commonUUID(data.getTopicCreateUserId())
                .commonName(data.getTopicCreateUserName()))
        .createTime(data.getTopicCreateTime())
        .viewCount(data.getTopicViewCount())
        .text(data.getTopicText())
        .numberOfReply(data.getNumberOfReply())
        .notificationType(data.getNotificationType())
        .recipient(data.getRecipient())
        .modifiedTime(data.getTopicModifiedTime())
        .toppingOrder(data.getTopicToppingOrder())
        .forumSupportTopicType(
            data.getForumSupportTopicType()
                .stream()
                .map(this::convertToSupportTopicType)
                .collect(Collectors.toList()));
  }

  private SupportTopicType convertToSupportTopicType(TopicTypeEntity topicType) {
    return new SupportTopicType()
        .topicType(TopicType.fromValue(topicType.getTopicType()))
        .editable(topicType.isEditable())
        .defaultAppField(
            ofNullable(topicType.getAppFieldDefaultId())
                .filter(StringUtils::isNotEmpty)
                .map(
                    id ->
                        Collections.singletonList(
                            new LabelValueDto()
                                .value(topicType.getAppFieldDefaultId())
                                .label(topicType.getAppFieldDefaultName())))
                .orElseGet(Collections::emptyList))
        .archiveConclusionAttachment(topicType.isArchiveConclusionAttachment());
  }
}
