package com.delta.dms.community.service;

import java.util.List;

import com.delta.dms.community.enums.DrcSyncType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.entity.ActivityLogMsg;
import com.delta.dms.community.event.ActivityLogEvent;
import com.delta.dms.community.event.ActivityLogMsgEvent;
import com.delta.dms.community.event.CommunityAvatarChangingEvent;
import com.delta.dms.community.event.CommunityChangingEvent;
import com.delta.dms.community.event.CommunityCreatingEvent;
import com.delta.dms.community.event.CommunityDeletingEvent;
import com.delta.dms.community.event.CommunityLockingEvent;
import com.delta.dms.community.event.CommunityReopeningEvent;
import com.delta.dms.community.event.EmailSendingEvent;
import com.delta.dms.community.event.FileUploadingEvent;
import com.delta.dms.community.event.ForumChangingEvent;
import com.delta.dms.community.event.ForumDeletingEvent;
import com.delta.dms.community.event.ForumLockingEvent;
import com.delta.dms.community.event.ForumReopeningEvent;
import com.delta.dms.community.event.NotificationSendingEvent;
import com.delta.dms.community.event.ReplyChangingEvent;
import com.delta.dms.community.event.SyncForumUserIdEvent;
import com.delta.dms.community.event.SyncForumUserIdInHomePageEvent;
import com.delta.dms.community.event.TopicChangingEvent;
import com.delta.dms.community.event.TopicDeletingEvent;
import com.delta.dms.community.event.DDFDeleteQueueTriggerDeletingEvent;
import com.delta.dms.community.event.TopicMovingEvent;
import com.delta.dms.community.event.DrcSyncEvent;
import com.delta.dms.community.swagger.model.ActivityLogData;
import com.delta.dms.community.swagger.model.EmailWithChineseAndEnglishContext;
import com.delta.dms.community.swagger.model.ForumData;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.Notification;
import com.delta.dms.community.swagger.model.TemplateType;

@Service
public class EventPublishService {

  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public EventPublishService(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void publishCommunityCreatingEvent(ForumData forumData) {
    CommunityCreatingEvent event = new CommunityCreatingEvent(this, forumData);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishCommunityChangingEvent(int communityId) {
    CommunityChangingEvent event = new CommunityChangingEvent(this, communityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishEmailSendingEvent(EmailWithChineseAndEnglishContext context) {
    EmailSendingEvent event = new EmailSendingEvent(this, context);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishEmailSendingEvent(
      EmailWithChineseAndEnglishContext context, TemplateType templateType) {
    EmailSendingEvent event = new EmailSendingEvent(this, context, templateType);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishForumChangingEvent(int communityId, int forumId, String userId, long time) {
    ForumChangingEvent event = new ForumChangingEvent(this, communityId, forumId, userId, time, true);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishForumChangingNoDdfUpdateEvent(int communityId, int forumId, String userId, long time) {
    ForumChangingEvent event = new ForumChangingEvent(this, communityId, forumId, userId, time, false);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishReplyChangingEvent(
      int communityId, int forumId, int topicId, String userId, long time) {
    ReplyChangingEvent event =
        new ReplyChangingEvent(this, communityId, forumId, topicId, userId, time);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishFileUploadingEvent(
      String fileId,
      List<String> author,
      int communityId,
      int forumId,
      ForumType forumType,
      boolean updateDtuMapping,
      String videoLanguage) {
    FileUploadingEvent event =
        new FileUploadingEvent(
            this, fileId, author, communityId, forumId, forumType, updateDtuMapping, videoLanguage);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishTopicChangingEvent(
      int topicId, int forumId, int communityId, String userId, long time) {
    TopicChangingEvent event =
        new TopicChangingEvent(this, topicId, forumId, communityId, userId, time);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishActivityLogEvent(ActivityLogData context) {
    ActivityLogEvent event = new ActivityLogEvent(this, context);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishActivityLogMsgEvent(ActivityLogMsg context) {
    ActivityLogMsgEvent event = new ActivityLogMsgEvent(this, context);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishForumDeletingEvent(
      int communityId, int forumId, String userId, long time, String forumDdfId) {
    ForumDeletingEvent event =
        new ForumDeletingEvent(this, communityId, forumId, userId, time, forumDdfId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishCommunityDeletingEvent(int communityId, String communityDdfId) {
    CommunityDeletingEvent event = new CommunityDeletingEvent(this, communityId, communityDdfId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishForumLockingEvent(int forumId, String userId, long lockedTime) {
    ForumLockingEvent event = new ForumLockingEvent(this, forumId, userId, lockedTime);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishCommunityLockingEvent(int communityId, String userId, long lockedTime) {
    CommunityLockingEvent event = new CommunityLockingEvent(this, communityId, userId, lockedTime);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishTopicDeletingEvent(
      int communityId, int forumId, int topicId, String userId, long time) {
    TopicDeletingEvent event =
        new TopicDeletingEvent(this, communityId, forumId, topicId, userId, time);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishDDFDeleteQueueTriggerDeletingEvent(int associatedId) {
    DDFDeleteQueueTriggerDeletingEvent event = new DDFDeleteQueueTriggerDeletingEvent(this, associatedId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishNotificationSendingEvent(Notification notification) {
    NotificationSendingEvent event = new NotificationSendingEvent(this, notification);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishTopicMovingEvent(int forumId, int communityId, String userId, long time) {
    TopicMovingEvent event = new TopicMovingEvent(this, forumId, communityId, userId, time);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishSyncForumUserIdEvent(int communityId, String groupId) {
    SyncForumUserIdEvent event = new SyncForumUserIdEvent(this, communityId, groupId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishSyncForumUserIdInHomePageEvent(int communityId, int forumId, String groupId) {
    SyncForumUserIdInHomePageEvent event =
        new SyncForumUserIdInHomePageEvent(this, communityId, forumId, groupId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishForumReopeningEvent(int forumId, String userId, long lockedTime) {
    ForumReopeningEvent event = new ForumReopeningEvent(this, forumId, userId, lockedTime);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishCommunityReopeningEvent(int communityId, String userId, long lockedTime) {
    CommunityReopeningEvent event =
        new CommunityReopeningEvent(this, communityId, userId, lockedTime);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishCommunityAvatarChangingEvent(int communityId) {
    CommunityAvatarChangingEvent event = new CommunityAvatarChangingEvent(this, communityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishDrcSyncEvent(String database, DrcSyncType action, int topicId, String topicTitle, String topicText, int communityId, int forumId) {
    DrcSyncEvent event = new DrcSyncEvent(this, database, action, topicId, topicTitle, topicText, communityId, forumId);
    applicationEventPublisher.publishEvent(event);
  }
}
