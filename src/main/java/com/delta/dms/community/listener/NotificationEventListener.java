package com.delta.dms.community.listener;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.service.*;
import com.delta.dms.community.swagger.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.event.NotificationSendingEvent;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.NotificationConstants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Transactional
public class NotificationEventListener {
  private final CommunityService communityService;
  private final ForumService forumService;
  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper = new ObjectMapper();
  static final String MQTT_TOPIC_FORMAT = "%s/%s/community/%s/notify";

  private NotificationService notificationService;
  private MqttService mqttService;
  private ApnsService apnsService;
  private DeviceService deviceService;
  private YamlConfig yamlConfig;

  @Autowired
  public NotificationEventListener(
      CommunityService communityService,
      ForumService forumService,
      NotificationService notificationService,
      MqttService mqttService,
      ApnsService apnsService,
      DeviceService deviceService,
      YamlConfig yamlConfig) {
    this.communityService = communityService;
    this.forumService = forumService;
    this.notificationService = notificationService;
    this.mqttService = mqttService;
    this.apnsService = apnsService;
    this.deviceService = deviceService;
    this.yamlConfig = yamlConfig;
  }

  @Async
  @TransactionalEventListener
  public void handleNotificationSendingEvent(NotificationSendingEvent event) {
    log.debug("Sending the notification");
    Notification notification = event.getNotification().status(NotificationStatus.UNREAD);
    PublishMessage messageTw =
        notificationService.transferNotificationToPublishMessage(
            notification, NotificationConstants.LANG_TW);
    PublishMessage messageCn =
        notificationService.transferNotificationToPublishMessage(
            notification, NotificationConstants.LANG_CN);
    PublishMessage messageEn =
        notificationService.transferNotificationToPublishMessage(
            notification, NotificationConstants.LANG_EN);

    List<String> userIdList = getUserId(event);
    userIdList
        .stream()
        .filter(item -> !item.isEmpty())
        .forEach(
            item -> {
              Notification userNotification =
                  new Notification()
                      .userId(item)
                      .type(notification.getType())
                      .title(notification.getTitle())
                      .status(notification.getStatus())
                      .state(notification.getState())
                      .content(notification.getContent())
                      .time(notification.getTime())
                      .priority(notification.getPriority())
                      .communityId(notification.getCommunityId())
                      .communityName(notification.getCommunityName())
                      .communityCategory(notification.getCommunityCategory())
                      .forumId(notification.getForumId())
                      .forumName(notification.getForumName())
                      .forumType(notification.getForumType())
                      .topicId(notification.getTopicId())
                      .topicTitle(notification.getTopicTitle())
                      .topicType(notification.getTopicType())
                      .senderId(notification.getSenderId());
              notificationService.storeNotification(userNotification);
              messageTw.setId(userNotification.getId());
              messageCn.setId(userNotification.getId());
              messageEn.setId(userNotification.getId());
              if (notificationService.isApplicationTypeNotification(notification.getType())) {
                int applicationId =
                    notificationService.getApplicationIdByTypeAndTime(
                        notification.getType(), notification.getSenderId(), notification.getTime());
                notificationService.storeUnreviewedNotification(
                    notification.getType(), Arrays.asList(userNotification.getId()), applicationId);
              }
              int notificationCount = notificationService.countNotification(item);
              messageTw.setNotificationCount(notificationCount);
              messageCn.setNotificationCount(notificationCount);
              messageEn.setNotificationCount(notificationCount);
              publishMessageToMqtt(messageTw, messageCn, messageEn, yamlConfig.getEnvIdentity(), item);
              publishMessageToApns(messageTw, messageCn, messageEn, item);
            });
  }

  private List<String> getUserId(NotificationSendingEvent event) {
      List<String> userIdList = Arrays.asList(event.getNotification().getUserId().split(Constants.COMMA_DELIMITER));
      if(event.getNotification().getType().equals(EmailType.COMMUNITYJOINAPPLICATION)) {
          userIdList = communityService.getAdminListOfCommunity(event.getNotification().getCommunityId(),
                          null, null, -1)
                  .stream()
                  .map(User::getId)
                  .collect(Collectors.toList());
      }
      else if(event.getNotification().getType().equals(EmailType.JOINCOMMUNITY)) {
          if(event.getNotification().getUserId().equals(EmailMemberType.ALLCOMMUNITYMEMBER.toString())) {
              userIdList = communityService.getAllMemberOfCommunityById(event.getNotification().getCommunityId(),
                              null, null, -1, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
          }
      }
      else if(event.getNotification().getType().equals(EmailType.JOINFORUM)) {
          if(event.getNotification().getUserId().equals(EmailMemberType.ALLPRIVATEFORUMMEMBER.toString())) {
              forumService.getAllPrivateForumMemberList(
                      event.getNotification().getForumId(), event.getNotification().getForumCreatedById());
          } else if(event.getNotification().getUserId().equals(EmailMemberType.ALLPUBLICFORUMMEMBER.toString())) {
              forumService.getAllPublicForumMemberList(
                      event.getNotification().getForumId(), event.getNotification().getForumCreatedById());
          }
      }
      else if(event.getNotification().getType().equals(EmailType.COMMUNITYNOTIFICATION)) {
          if(event.getNotification().getExtraMemberType().equals(EmailMemberType.ALLCOMMUNITYMEMBER)) {
              List<String> allMemberIdList = communityService.getAdminListOfCommunity(event.getNotification().getCommunityId(),
                              null, null, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
              userIdList = Stream.of(userIdList, allMemberIdList)
                      .flatMap(Collection::stream)
                      .distinct()
                      .collect(Collectors.toList());
          }
      } else if(event.getNotification().getType().equals(EmailType.COMMUNITYDELETION)) {
          if (event.getNotification().getExtraMemberType().equals(EmailMemberType.ALLCOMMUNITYMEMBER)) {
              List<String> allMemberIdList = communityService.getAdminListOfCommunity(event.getNotification().getCommunityId(),
                              null, null, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
          }
      } else if(event.getNotification().getType().equals(EmailType.FORUMNOTIFICATION)) {
          if(event.getNotification().getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
              List<String> allMemberIdList = forumService.getMemberOfForum(event.getNotification().getForumId(),
                              -1, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
              userIdList = Stream.of(userIdList, allMemberIdList)
                      .flatMap(Collection::stream)
                      .distinct()
                      .collect(Collectors.toList());
          }
      } else if(event.getNotification().getType().equals(EmailType.TOPICNOTIFICATION)) {
          if(event.getNotification().getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
              userIdList = forumService.getMemberOfForum(event.getNotification().getForumId(),
                              -1, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
          }
      } else if(event.getNotification().getType().equals(EmailType.FORUMDELETION)) {
          if(event.getNotification().getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
              userIdList = forumService.getMemberOfForum(event.getNotification().getForumId(),
                              -1, -1)
                      .stream()
                      .map(User::getId)
                      .collect(Collectors.toList());
          }
      }
      return userIdList;
  }

  private void publishMessageToMqtt(
      PublishMessage tw, PublishMessage cn, PublishMessage en, String env, String userId) {
    try {
      mqttService.publishMessage(
          String.format(MQTT_TOPIC_FORMAT, env, NotificationConstants.LANG_TW, userId),
          mapper.writeValueAsString(tw));
      mqttService.publishMessage(
          String.format(MQTT_TOPIC_FORMAT, env, NotificationConstants.LANG_CN, userId),
          mapper.writeValueAsString(cn));
      mqttService.publishMessage(
          String.format(MQTT_TOPIC_FORMAT, env, NotificationConstants.LANG_EN, userId),
          mapper.writeValueAsString(en));
    } catch (JsonProcessingException e) {
      log.error(e);
    }
  }

  private void publishMessageToApns(
      PublishMessage tw, PublishMessage cn, PublishMessage en, String userId) {
    Set<String> deviceListTw = new HashSet<>();
    Set<String> deviceListCn = new HashSet<>();
    Set<String> deviceListEn = new HashSet<>();
    List<DeviceInfo> deviceTokens = deviceService.getDeviceTokenListOfUser(userId);
    deviceTokens
        .stream()
        .filter(token -> NotificationConstants.LANG_TW.equals(token.getLanguage()))
        .map(DeviceInfo::getDeviceToken)
        .forEach(deviceListTw::add);
    deviceTokens
        .stream()
        .filter(token -> NotificationConstants.LANG_CN.equals(token.getLanguage()))
        .map(DeviceInfo::getDeviceToken)
        .forEach(deviceListCn::add);
    deviceTokens
        .stream()
        .filter(token -> NotificationConstants.LANG_EN.equals(token.getLanguage()))
        .map(DeviceInfo::getDeviceToken)
        .forEach(deviceListEn::add);
    apnsService.publishMessage(tw, deviceListTw);
    apnsService.publishMessage(cn, deviceListCn);
    apnsService.publishMessage(en, deviceListEn);
  }
}
