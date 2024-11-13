package com.delta.dms.community.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.NotificationDao;
import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.swagger.model.EmailType;
import com.delta.dms.community.swagger.model.Notification;
import com.delta.dms.community.swagger.model.NotificationResultList;
import com.delta.dms.community.swagger.model.PublishMessage;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.NotificationConstants;
import com.delta.dms.community.utils.Utility;

@Service
public class NotificationService {

  private NotificationDao notificationDao;
  private CommunityService communityService;
  private UserService userService;
  private YamlConfig yamlConfig;

  @Autowired
  public NotificationService(
      NotificationDao notificationDao,
      CommunityService communityService,
      UserService userService,
      YamlConfig yamlConfig) {
    this.notificationDao = notificationDao;
    this.communityService = communityService;
    this.userService = userService;
    this.yamlConfig = yamlConfig;
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public NotificationResultList getNotificationResultListOfUser(int offset, int limit) {
    List<Notification> notificationList =
        getNotificationOfUser(Utility.getUserIdFromSession(), offset, limit);
    List<PublishMessage> publishMessageList =
        notificationList
            .stream()
            .map(item -> transferNotificationToPublishMessage(item, AcceptLanguage.get()))
            .collect(Collectors.toList());
    Map<String, String> iconMap = new HashMap<>();
    publishMessageList
        .stream()
        .map(PublishMessage::getIcon)
        .filter(item -> !item.isEmpty())
        .forEach(item -> collectIconMap(iconMap, item));
    publishMessageList.forEach(
        item ->
            item.icon(
                iconMap
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(item.getIcon()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(""))));
    return new NotificationResultList().result(publishMessageList).icon(iconMap);
  }

  private List<Notification> getNotificationOfUser(String userId, int offset, int limit) {
    return notificationDao.getNotificationOfUser(userId, offset, limit);
  }

  private void collectIconMap(Map<String, String> iconMap, String icon) {
    if (!iconMap.containsValue(icon)) {
      iconMap.put(String.valueOf(iconMap.size()), icon);
    }
  }

  @Transactional
  public void storeNotification(Notification notification) {
    notificationDao.storeNotification(notification);
  }

  @Transactional
  public void storeUnreviewedNotification(EmailType type, List<Integer> idList, int applicationId) {
    if (!idList.isEmpty() && isApplicationTypeNotification(type)) {
      notificationDao.storeUnreviewedNotification(idList, applicationId);
    }
  }

  public int getApplicationIdByTypeAndTime(EmailType type, String applicantId, long time) {
    return notificationDao.getApplicationIdByTypeAndTime(type.toString(), applicantId, time);
  }

  public boolean isApplicationTypeNotification(EmailType type) {
    return EmailType.COMMUNITYCREATIONAPPLICATION.equals(type)
        || EmailType.COMMUNITYDELETIONAPPLICATION.equals(type)
        || EmailType.COMMUNITYJOINAPPLICATION.equals(type)
        || EmailType.FORUMJOINAPPLICATION.equals(type);
  }

  @Transactional
  public void readNotification(int notificationId) {
    Notification notification = getNotification(notificationId);
    if (!notification.getUserId().equals(Utility.getUserIdFromSession())) {
      throw new AuthenticationException(I18nConstants.MSG_NOTIFY_NOT_AUTHORIZED);
    }
    notificationDao.readNotification(notificationId);
  }

  private Notification getNotification(int notificationId) {
    Notification notification = notificationDao.getNotificationById(notificationId);
    if (notification == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_NOTIFY_NOT_EXIST);
    }
    return notification;
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
  public PublishMessage transferNotificationToPublishMessage(
      Notification notification, String lang) {
    String host = yamlConfig.getHost();
    if (Jwt.get().isEmpty()) {
      Jwt.set(userService.getSysAdminToken());
    }
    UserSession user = getUserInfo(notification.getSenderId());
    switch (notification.getType()) {
      case COMMUNITYCREATIONAPPLICATION:
        return getPublishMessage(
            notification,
            user.getCommonImage(),
            "",
            getDescOfCommunityCreationApplication(
                lang, user.getCommonName(), notification.getCommunityName()),
            String.format(NotificationConstants.COMMUNITY_APPLICATION_URI_FORMAT, host, lang),
            0,
            String.valueOf(notification.getCommunityId()));
      case COMMUNITYCREATIONAPPROVAL:
        return getPublishMessage(
            notification,
            "",
            notification.getCommunityCategory().toString(),
            getDescOfCommunityCreationApplicationApproval(lang, notification.getCommunityName()),
            String.format(
                NotificationConstants.COMMUNITY_HOME_URI_FORMAT,
                host,
                lang,
                notification.getCommunityId()),
            notification.getCommunityId(),
            "");
      case COMMUNITYCREATIONREJECTION:
        return getPublishMessage(
            notification,
            "",
            notification.getCommunityCategory().toString(),
            getDescOfCommunityCreationApplicationRejection(lang, notification.getCommunityName()),
            "",
            0,
            "");
      case COMMUNITYDELETION:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfCommunityDeletion(lang, notification.getCommunityName(), user.getCommonName()),
            "",
            notification.getCommunityId(),
            "");
      case COMMUNITYDELETIONAPPLICATION:
        return getPublishMessage(
            notification,
            user.getCommonImage(),
            "",
            getDescOfCommunityDeletionApplication(
                lang, notification.getCommunityName(), user.getCommonName()),
            String.format(NotificationConstants.COMMUNITY_APPLICATION_URI_FORMAT, host, lang),
            notification.getCommunityId(),
            String.valueOf(notification.getCommunityId()));
      case COMMUNITYDELETIONAPPROVAL:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfCommunityDeletionApplicationApproval(lang, notification.getCommunityName()),
            "",
            notification.getCommunityId(),
            "");
      case COMMUNITYDELETIONREJECTION:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfCommunityDeletionApplicationRejection(lang, notification.getCommunityName()),
            "",
            notification.getCommunityId(),
            "");
      case COMMUNITYJOINAPPLICATION:
        return getPublishMessage(
            notification,
            user.getCommonImage(),
            "",
            getDescOfJoinApplication(lang, user.getCommonName(), notification.getCommunityName()),
            "",
            notification.getCommunityId(),
            notification.getSenderId());
      case COMMUNITYJOINAPPROVAL:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfJoinApplicationApproval(lang, notification.getCommunityName()),
            String.format(
                NotificationConstants.COMMUNITY_HOME_URI_FORMAT,
                host,
                lang,
                notification.getCommunityId()),
            notification.getCommunityId(),
            "");
      case COMMUNITYJOINREJECTION:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfJoinApplicationRejection(lang, notification.getCommunityName()),
            "",
            notification.getCommunityId(),
            "");
      case COMMUNITYNOTIFICATION:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfNotification(lang, user.getCommonName(), notification.getCommunityName()),
            String.format(
                NotificationConstants.COMMUNITY_HOME_URI_FORMAT,
                host,
                lang,
                notification.getCommunityId()),
            notification.getCommunityId(),
            "");
      case CONCLUSIONMADE:
        return getPublishMessage(
            notification,
            "",
            notification.getTopicType().toString(),
            getDescOfConclusionMade(
                lang,
                user.getCommonName(),
                notification.getForumName(),
                notification.getTopicTitle()),
            String.format(
                NotificationConstants.TOPIC_URI_FORMAT, host, lang, notification.getTopicId()),
            notification.getTopicId(),
            "");
      case FORUMDELETION:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfForumDeletion(lang, notification.getForumName(), user.getCommonName()),
            "",
            notification.getForumId(),
            "");
      case FORUMJOINAPPLICATION:
        return getPublishMessage(
            notification,
            user.getCommonImage(),
            "",
            getDescOfJoinApplication(lang, user.getCommonName(), notification.getForumName()),
            "",
            notification.getForumId(),
            notification.getSenderId());
      case FORUMJOINAPPROVAL:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfJoinApplicationApproval(lang, notification.getForumName()),
            String.format(
                NotificationConstants.FORUM_HOME_URI_FORMAT, host, lang, notification.getForumId()),
            notification.getForumId(),
            "");
      case FORUMJOINREJECTION:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfJoinApplicationRejection(lang, notification.getForumName()),
            "",
            notification.getForumId(),
            "");
      case FORUMNOTIFICATION:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfNotification(lang, user.getCommonName(), notification.getForumName()),
            String.format(
                NotificationConstants.FORUM_HOME_URI_FORMAT, host, lang, notification.getForumId()),
            notification.getForumId(),
            "");
      case JOINCOMMUNITY:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfJoinCommunity(notification, lang),
            String.format(
                NotificationConstants.COMMUNITY_HOME_URI_FORMAT,
                host,
                lang,
                notification.getCommunityId()),
            notification.getCommunityId(),
            "");
      case JOINFORUM:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfJoinForum(notification, lang),
            String.format(
                NotificationConstants.FORUM_HOME_URI_FORMAT, host, lang, notification.getForumId()),
            notification.getForumId(),
            "");
      case REMOVEFROMCOMMUNITY:
        return getPublishMessage(
            notification,
            getIconOfCommunity(notification.getCommunityId()),
            notification.getCommunityCategory().toString(),
            getDescOfRemoveCommunity(notification, lang),
            "",
            notification.getCommunityId(),
            "");
      case REMOVEFROMFORUM:
        return getPublishMessage(
            notification,
            "",
            notification.getForumType().toString(),
            getDescOfRemoveForum(notification, lang),
            "",
            notification.getForumId(),
            "");
      case TOPICNOTIFICATION:
        return getPublishMessage(
            notification,
            "",
            notification.getTopicType().toString(),
            getDescOfTopicNotification(
                lang,
                user.getCommonName(),
                notification.getForumName(),
                notification.getTopicTitle()),
            String.format(
                NotificationConstants.TOPIC_URI_FORMAT, host, lang, notification.getTopicId()),
            notification.getTopicId(),
            "");
      default:
        return new PublishMessage();
    }
  }

  private String getIconOfCommunity(int communityId) {
    return communityService.getCommunityImgAvatarById(communityId);
  }

  private UserSession getUserInfo(String id) {
    List<UserSession> user = new ArrayList<>();
    if (!id.isEmpty()) {
      user = userService.getUserById(Arrays.asList(id), new ArrayList<>());
    }
    return user.isEmpty() ? new UserSession() : user.get(0);
  }

  private String getDescOfCommunityCreationApplication(
      String lang, String userName, String communityName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_TW_FORMAT,
            userName,
            communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_CN_FORMAT,
            userName,
            communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_EN_FORMAT,
            userName,
            communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_TW_FORMAT,
            userName,
            communityName);
    }
  }

  private String getDescOfCommunityCreationApplicationApproval(String lang, String communityName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_APPROVAL_TW_FORMAT, communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_APPROVAL_CN_FORMAT, communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_APPROVAL_EN_FORMAT, communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_APPROVAL_TW_FORMAT, communityName);
    }
  }

  private String getDescOfCommunityCreationApplicationRejection(String lang, String communityName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_REJECTION_TW_FORMAT,
            communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_REJECTION_CN_FORMAT,
            communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_REJECTION_EN_FORMAT,
            communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_CREATION_APPLICATION_REJECTION_TW_FORMAT,
            communityName);
    }
  }

  private String getDescOfCommunityDeletion(String lang, String communityName, String userName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_TW_FORMAT, communityName, userName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_CN_FORMAT, communityName, userName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_EN_FORMAT, communityName, userName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_TW_FORMAT, communityName, userName);
    }
  }

  private String getDescOfCommunityDeletionApplication(
      String lang, String communityName, String userName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_TW_FORMAT,
            userName,
            communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_CN_FORMAT,
            userName,
            communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_EN_FORMAT,
            userName,
            communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_TW_FORMAT,
            userName,
            communityName);
    }
  }

  private String getDescOfCommunityDeletionApplicationApproval(String lang, String communityName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_APPROVAL_TW_FORMAT, communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_APPROVAL_CN_FORMAT, communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_APPROVAL_EN_FORMAT, communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_APPROVAL_TW_FORMAT, communityName);
    }
  }

  private String getDescOfCommunityDeletionApplicationRejection(String lang, String communityName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_REJECTION_TW_FORMAT,
            communityName);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_REJECTION_CN_FORMAT,
            communityName);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_REJECTION_EN_FORMAT,
            communityName);
      default:
        return String.format(
            NotificationConstants.COMMUNITY_DELETION_APPLICATION_REJECTION_TW_FORMAT,
            communityName);
    }
  }

  private String getDescOfJoinApplication(String lang, String userName, String applyFor) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(NotificationConstants.JOIN_APPLICATION_TW_FORMAT, userName, applyFor);
      case NotificationConstants.LANG_CN:
        return String.format(NotificationConstants.JOIN_APPLICATION_CN_FORMAT, userName, applyFor);
      case NotificationConstants.LANG_EN:
        return String.format(NotificationConstants.JOIN_APPLICATION_EN_FORMAT, userName, applyFor);
      default:
        return String.format(NotificationConstants.JOIN_APPLICATION_TW_FORMAT, userName, applyFor);
    }
  }

  private String getDescOfJoinApplicationApproval(String lang, String applyFor) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(NotificationConstants.JOIN_APPLICATION_APPROVAL_TW_FORMAT, applyFor);
      case NotificationConstants.LANG_CN:
        return String.format(NotificationConstants.JOIN_APPLICATION_APPROVAL_CN_FORMAT, applyFor);
      case NotificationConstants.LANG_EN:
        return String.format(NotificationConstants.JOIN_APPLICATION_APPROVAL_EN_FORMAT, applyFor);
      default:
        return String.format(NotificationConstants.JOIN_APPLICATION_APPROVAL_TW_FORMAT, applyFor);
    }
  }

  private String getDescOfJoinApplicationRejection(String lang, String applyFor) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(NotificationConstants.JOIN_APPLICATION_REJECTION_TW_FORMAT, applyFor);
      case NotificationConstants.LANG_CN:
        return String.format(NotificationConstants.JOIN_APPLICATION_REJECTION_CN_FORMAT, applyFor);
      case NotificationConstants.LANG_EN:
        return String.format(NotificationConstants.JOIN_APPLICATION_REJECTION_EN_FORMAT, applyFor);
      default:
        return String.format(NotificationConstants.JOIN_APPLICATION_REJECTION_TW_FORMAT, applyFor);
    }
  }

  private String getDescOfNotification(String lang, String userName, String from) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(NotificationConstants.NOTIFICATION_TW_FORMAT, userName, from);
      case NotificationConstants.LANG_CN:
        return String.format(NotificationConstants.NOTIFICATION_CN_FORMAT, userName, from);
      case NotificationConstants.LANG_EN:
        return String.format(NotificationConstants.NOTIFICATION_EN_FORMAT, userName, from);
      default:
        return String.format(NotificationConstants.NOTIFICATION_TW_FORMAT, userName, from);
    }
  }

  private String getDescOfConclusionMade(
      String lang, String userName, String forumName, String topicTitle) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.TOPIC_CONCLUSION_MADE_TW_FORMAT, userName, forumName, topicTitle);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.TOPIC_CONCLUSION_MADE_CN_FORMAT, userName, forumName, topicTitle);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.TOPIC_CONCLUSION_MADE_EN_FORMAT, userName, topicTitle, forumName);
      default:
        return String.format(
            NotificationConstants.TOPIC_CONCLUSION_MADE_TW_FORMAT, userName, forumName, topicTitle);
    }
  }

  private String getDescOfForumDeletion(String lang, String forumName, String userName) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(NotificationConstants.FORUM_DELETION_TW_FORMAT, forumName, userName);
      case NotificationConstants.LANG_CN:
        return String.format(NotificationConstants.FORUM_DELETION_CN_FORMAT, forumName, userName);
      case NotificationConstants.LANG_EN:
        return String.format(NotificationConstants.FORUM_DELETION_EN_FORMAT, forumName, userName);
      default:
        return String.format(NotificationConstants.FORUM_DELETION_TW_FORMAT, forumName, userName);
    }
  }

  private String getDescOfJoinCommunity(Notification notification, String lang) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_JOIN_TW_FORMAT, notification.getCommunityName());
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_JOIN_CN_FORMAT, notification.getCommunityName());
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_JOIN_EN_FORMAT, notification.getCommunityName());
      default:
        return String.format(
            NotificationConstants.COMMUNITY_JOIN_TW_FORMAT, notification.getCommunityName());
    }
  }

  private String getDescOfJoinForum(Notification notification, String lang) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.FORUM_JOIN_TW_FORMAT, notification.getForumName());
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.FORUM_JOIN_TW_FORMAT, notification.getForumName());
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.FORUM_JOIN_EN_FORMAT, notification.getForumName());
      default:
        return String.format(
            NotificationConstants.FORUM_JOIN_TW_FORMAT, notification.getForumName());
    }
  }

  private String getDescOfRemoveCommunity(Notification notification, String lang) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.COMMUNITY_REMOVE_TW_FORMAT, notification.getCommunityName());
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.COMMUNITY_REMOVE_CN_FORMAT, notification.getCommunityName());
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.COMMUNITY_REMOVE_EN_FORMAT, notification.getCommunityName());
      default:
        return String.format(
            NotificationConstants.COMMUNITY_REMOVE_TW_FORMAT, notification.getCommunityName());
    }
  }

  private String getDescOfRemoveForum(Notification notification, String lang) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.FORUM_REMOVE_TW_FORMAT, notification.getForumName());
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.FORUM_REMOVE_CN_FORMAT, notification.getForumName());
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.FORUM_REMOVE_EN_FORMAT, notification.getForumName());
      default:
        return String.format(
            NotificationConstants.FORUM_REMOVE_TW_FORMAT, notification.getForumName());
    }
  }

  private String getDescOfTopicNotification(
      String lang, String userName, String forumName, String topicTitle) {
    switch (lang) {
      case NotificationConstants.LANG_TW:
        return String.format(
            NotificationConstants.TOPIC_NOTIFICATION_TW_FORMAT, userName, forumName, topicTitle);
      case NotificationConstants.LANG_CN:
        return String.format(
            NotificationConstants.TOPIC_NOTIFICATION_CN_FORMAT, userName, forumName, topicTitle);
      case NotificationConstants.LANG_EN:
        return String.format(
            NotificationConstants.TOPIC_NOTIFICATION_EN_FORMAT, userName, topicTitle, forumName);
      default:
        return String.format(
            NotificationConstants.TOPIC_NOTIFICATION_TW_FORMAT, userName, forumName, topicTitle);
    }
  }

  public PublishMessage getPublishMessage(int notificationId) {
    Notification notification = getNotification(notificationId);
    if (!notification.getUserId().equals(Utility.getUserIdFromSession())) {
      throw new AuthenticationException(I18nConstants.MSG_NOTIFY_NOT_AUTHORIZED);
    }
    return transferNotificationToPublishMessage(notification, AcceptLanguage.get());
  }

  private PublishMessage getPublishMessage(
      Notification notification,
      String icon,
      String iconType,
      String desc,
      String url,
      int targetId,
      String reviewedId) {
    return new PublishMessage()
        .id(notification.getId())
        .type(notification.getType())
        .status(notification.getStatus())
        .state(notification.getState())
        .title(notification.getTitle())
        .icon(icon)
        .iconType(iconType)
        .content(notification.getContent())
        .time(notification.getTime())
        .refUrl(url)
        .desc(desc)
        .targetId(targetId)
        .reviewedId(reviewedId);
  }

  public void renewAccessTime() {
    notificationDao.renewAccessTime(Utility.getUserIdFromSession(), Instant.now().toEpochMilli());
  }

  public int countNotification(String userId) {
    return notificationDao.countNotification(userId);
  }
}
