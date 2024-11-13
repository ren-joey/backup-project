package com.delta.dms.community.listener;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;

import com.delta.dms.community.enums.Role;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Component;
import com.delta.datahive.activitylog.ActivityLog;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.DDF;
import com.delta.datahive.api.DDF.PrivilegeSection;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.datahive.types.DDFStatus;
import com.delta.dms.community.adapter.StreamingAdapter;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.ActivityLogDao;
import com.delta.dms.community.dao.AttachmentDao;
import com.delta.dms.community.dao.DdfDao;
import com.delta.dms.community.dao.entity.ActivityLogInfo;
import com.delta.dms.community.dao.entity.ActivityLogMsg;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.enums.DdfQueueStatus;
import com.delta.dms.community.event.ActivityLogEvent;
import com.delta.dms.community.event.ActivityLogMsgEvent;
import com.delta.dms.community.event.CommunityAvatarChangingEvent;
import com.delta.dms.community.event.CommunityChangingEvent;
import com.delta.dms.community.event.CommunityCreatingEvent;
import com.delta.dms.community.event.CommunityDeletingEvent;
import com.delta.dms.community.event.CommunityLockingEvent;
import com.delta.dms.community.event.CommunityReopeningEvent;
import com.delta.dms.community.event.FileUploadingEvent;
import com.delta.dms.community.event.ForumChangingEvent;
import com.delta.dms.community.event.ForumDeletingEvent;
import com.delta.dms.community.event.ForumLockingEvent;
import com.delta.dms.community.event.ForumReopeningEvent;
import com.delta.dms.community.event.ReplyChangingEvent;
import com.delta.dms.community.event.SyncForumUserIdEvent;
import com.delta.dms.community.event.SyncForumUserIdInHomePageEvent;
import com.delta.dms.community.event.TopicChangingEvent;
import com.delta.dms.community.event.TopicDeletingEvent;
import com.delta.dms.community.event.DDFDeleteQueueTriggerDeletingEvent;
import com.delta.dms.community.event.TopicMovingEvent;
import com.delta.dms.community.exception.CreationException;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.FileService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.ReplyService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.model.ActivityLogData;
import com.delta.dms.community.swagger.model.DdfRole;
import com.delta.dms.community.swagger.model.DdfType;
import com.delta.dms.community.swagger.model.ForumListDetail;
import com.delta.dms.community.swagger.model.ForumStatus;
import com.delta.dms.community.swagger.model.ForumStatusInput;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.ReplyStatus;
import com.delta.dms.community.swagger.model.ResponseData;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.swagger.model.TopicStatus;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.swagger.model.VideoMappingDto;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CustomEventListener {

  private static final String CREATE_SYSTEM_FORUM_FAILED = "Create system forum failed";
  private static final LogUtil log = LogUtil.getInstance();

  private CommunityService communityService;
  private ForumService forumService;
  private TopicService topicService;
  private FileService fileService;
  private ReplyService replyService;
  private YamlConfig yamlConfig;
  private DdfDao ddfDao;
  private ActivityLogDao activityLogDao;
  private AttachmentDao attachmentDao;
  private StreamingAdapter streamAdapter;

  @Autowired
  public CustomEventListener(
      CommunityService communityService,
      ForumService forumService,
      TopicService topicService,
      FileService fileService,
      ReplyService replyService,
      YamlConfig yamlConfig,
      DdfDao ddfDao,
      ActivityLogDao activityLogDao,
      AttachmentDao attachmentDao,
      StreamingAdapter streamAdapter) {
    this.communityService = communityService;
    this.forumService = forumService;
    this.topicService = topicService;
    this.fileService = fileService;
    this.replyService = replyService;
    this.yamlConfig = yamlConfig;
    this.ddfDao = ddfDao;
    this.activityLogDao = activityLogDao;
    this.attachmentDao = attachmentDao;
    this.streamAdapter = streamAdapter;
  }

  @EventListener
  public void handleCommunityCreatingEvent(CommunityCreatingEvent event) {
    log.debug("Create system forum");
    ResponseData createStatus = forumService.createForum(event.getForumData());
    if (HttpStatus.SC_NOT_FOUND == createStatus.getStatusCode()) {
      throw new CreationException(CREATE_SYSTEM_FORUM_FAILED);
    }
    updateAllDdfUnderCommunity(event.getForumData().getCommunityId());
  }

  @EventListener
  public void handleCommunityChangingEvent(CommunityChangingEvent event) {
    log.debug("Community changing");
    updateAllDdfUnderCommunity(event.getCommunityId());
  }

  private void updateAllDdfUnderCommunity(int communityId) {
    ddfDao.upsertDdfQueue(
        DdfType.COMMUNITY.toString(), communityId, DdfQueueStatus.WAIT.getValue(), EMPTY);
    List<Integer> forumIdList =
        forumService
            .searchForumOfCommunityByType(
                communityId,
                Arrays.stream(ForumType.values()).collect(Collectors.toList()),
                -1,
                -1,
                new Order(Direction.DESC, SortField.UPDATETIME.toString()))
            .stream()
            .map(ForumListDetail::getId)
            .collect(Collectors.toList());
    forumIdList.forEach(
        item -> {
          ddfDao.upsertDdfQueue(
              DdfType.FORUM.toString(), item, DdfQueueStatus.WAIT.getValue(), EMPTY);
          List<Integer> topicIdList =
              topicService
                  .getTopicOfForumWithSortAndLimit(
                      item,
                      -1,
                      -1,
                      new Order(Direction.DESC, SortField.UPDATETIME.toString()),
                      null,
                      null)
                  .stream()
                  .map(TopicInfo::getTopicId)
                  .collect(Collectors.toList());
          topicIdList.forEach(
              topic ->
                  ddfDao.upsertDdfQueue(
                      DdfType.TOPIC.toString(), topic, DdfQueueStatus.WAIT.getValue(), EMPTY));
        });
  }

  @EventListener
  public void handleFileUploadingEvent(FileUploadingEvent event) throws IOException {
    log.debug("Update ddf of the attachment");
    DDF ddf = setUpdateDdf(event);
    fileService.updateFile(event.getFileId(), ddf, null);
    attachmentDao.insertAttachmentKeyman(event.getAuthor(), event.getFileId());
    if (event.needUpdateDtuMapping() && StringUtils.isNotEmpty(event.getVideoLanguage())) {
      VideoMappingDto data =
          new VideoMappingDto().app(Constants.INFO_PROJECT_NAME).ddfId(event.getFileId());
      data.setVideoLanguage(event.getVideoLanguage());
      streamAdapter.upsertMapping(data);
    }
  }

  private DDF setUpdateDdf(FileUploadingEvent event) {
    String groupId =
        communityService.getCommunityInfoById(event.getCommunityId()).getCommunityGroupId();
    List<String> forumMemberRoleList = forumService.getMemberRoleListOfForum(event.getForumId());
    List<String> forumAdminRoleList = forumService.getAdminRoleListOfForum(event.getForumId());
    DDF ddf =
        new DDF()
            .setBaseSection(new BaseSection().setStatus(DDFStatus.OPEN))
            .setPrivilegeSection(
                new PrivilegeSection()
                    .setIdMap(fileService.getPrivilege(null, forumAdminRoleList, forumMemberRoleList)));
    if (!ForumType.PRIVATE.equals(event.getForumType())) {
      ddf.getPrivilegeSection().addPublic(FileService.PRIV_PUBLIC_SR);
    } else {
      ddf.getPrivilegeSection().removePublic(FileService.PRIV_ALL);
    }
    if (!groupId.isEmpty()) {
      ddf.getBaseSection().setOrg(new UserGroupEntity(groupId));
    }
    fileService
        .getRoleMap(null, event.getAuthor())
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    ddf.getBaseSection()
        .setPeople(
            DdfRole.APPLASSIGNEDMODIFIER.toString(),
            Arrays.asList(new UserGroupEntity(Utility.getUserIdFromSession())));
    return ddf;
  }

  @EventListener
  public void handleForumChangingEvent(ForumChangingEvent event) {
    log.debug("Forum changing");
    communityService.updateLastModifiedOfCommunity(
        event.getCommunityId(), event.getUserId(), event.getTime());

    if(event.getToUpdateDdf()) {
      ddfDao.upsertDdfQueue(
              DdfType.COMMUNITY.toString(),
              event.getCommunityId(),
              DdfQueueStatus.WAIT.getValue(),
              EMPTY);
      ddfDao.upsertDdfQueue(
              DdfType.FORUM.toString(), event.getForumId(), DdfQueueStatus.WAIT.getValue(), EMPTY);
      List<Integer> topicIdList =
              topicService
                      .getTopicOfForumWithSortAndLimit(
                              event.getForumId(),
                              -1,
                              -1,
                              new Order(Direction.DESC, SortField.UPDATETIME.toString()),
                              null,
                              null)
                      .stream()
                      .map(TopicInfo::getTopicId)
                      .collect(Collectors.toList());
      topicIdList.forEach(
              topic ->
                      ddfDao.upsertDdfQueue(
                              DdfType.TOPIC.toString(), topic, DdfQueueStatus.WAIT.getValue(), EMPTY));
    }
  }

  @EventListener
  public void handleTopicChangingEvent(TopicChangingEvent event) {
    log.debug("Topic changing");
    topicChanging(
        event.getCommunityId(),
        event.getForumId(),
        event.getTopicId(),
        event.getUserId(),
        event.getTime());
    topicOrReplyChanging(event.getCommunityId(), event.getForumId(), event.getTopicId());
  }

  private void topicChanging(int communityId, int forumId, int topicId, String userId, long time) {
    communityService.updateLastModifiedOfCommunity(communityId, userId, time);
    forumService.updateLastModifiedOfForum(forumId, userId, time);
    topicService.updateLastModifiedOfTopic(topicId, userId, time);
  }

  private void topicOrReplyChanging(int communityId, int forumId, int topicId) {
    ddfDao.upsertDdfQueue(DdfType.TOPIC.toString(), topicId, DdfQueueStatus.WAIT.getValue(), EMPTY);
    ddfDao.upsertDdfQueue(DdfType.FORUM.toString(), forumId, DdfQueueStatus.WAIT.getValue(), EMPTY);
    ddfDao.upsertDdfQueue(
        DdfType.COMMUNITY.toString(), communityId, DdfQueueStatus.WAIT.getValue(), EMPTY);
  }

  @EventListener
  public void handleReplyChangingEvent(ReplyChangingEvent event) {
    log.debug("Reply changing");
    communityService.updateLastModifiedOfCommunity(
        event.getCommunityId(), event.getUserId(), event.getTime());
    forumService.updateLastModifiedOfForum(event.getForumId(), event.getUserId(), event.getTime());
    topicService.updateLastModifiedOfTopic(event.getTopicId(), event.getUserId(), event.getTime());
    topicOrReplyChanging(event.getCommunityId(), event.getForumId(), event.getTopicId());
  }

  @EventListener
  public void handleActivityLogEvent(ActivityLogEvent event) {
    log.debug("Insert activityLog data into table");
    ActivityLogInfo activityLogInfo = setActivityLogInfo(event.getActivityLogData());
    activityLogDao.insertActivityLogData(activityLogInfo);
  }

  private ActivityLogInfo setActivityLogInfo(ActivityLogData activityLogData) {
    ActivityLogInfo activityLogInfo = new ActivityLogInfo();

    activityLogInfo.setUserId(activityLogData.getUserId());
    activityLogInfo.setOperation(activityLogData.getOperation());
    activityLogInfo.setObject(activityLogData.getObject());
    activityLogInfo.setObjectPk(activityLogData.getObjectPk());
    activityLogInfo.setOperationTime(activityLogData.getOperationTime());
    activityLogInfo.setOrigin(activityLogData.getOrigin());
    activityLogInfo.setContent(activityLogData.getContent());
    activityLogInfo.setAttachmentId(activityLogData.getAttachmentId());

    return activityLogInfo;
  }

  /*
   * Insert the activity log into MQ/DB of architecture team
   */
  @EventListener
  public void handleActivityLogMsgEvent(ActivityLogMsgEvent event) {
    log.debug("Insert activityLog data into new place of architecture team");
    ActivityLogMsg activityLogMsg = event.getActivityLogMsg();

    if (!ActivityLog.isConnected()) {
      this.connectActivityLog();
    }
    ActivityLog.addLog(
        activityLogMsg.getApp(),
        yamlConfig.getVersion(),
        activityLogMsg.getUserId(),
        activityLogMsg.getActivity(),
        activityLogMsg.getObjectType(),
        activityLogMsg.getObjectId(),
        activityLogMsg.getSourceOs(),
        activityLogMsg.getAnnotation(),
        activityLogMsg.getLogStatus(),
        activityLogMsg.getLogTimeUnit(),
        activityLogMsg.getParentObjectType(),
        activityLogMsg.getParentObjectId());
  }

  private void connectActivityLog() {
    log.debug("Connect to activity log mq server");
    try {
      ActivityLog.initConnection(true);
    } catch (Exception e) {
      log.error(e);
    }
  }

  @PreDestroy
  private void destroy() {
    log.debug("Disconnect activity log mq server");
    try {
      ActivityLog.closeConnection(true);
    } catch (IOException | TimeoutException e) {
      log.error(e);
    }
  }

  @EventListener
  public void handleForumDeletingEvent(ForumDeletingEvent event) {
    log.debug("Forum delete");
    communityService.updateLastModifiedOfCommunity(
        event.getCommunityId(), event.getUserId(), event.getTime());
    List<Integer> topicIdList =
        topicService
            .getTopicOfForumWithSortAndLimit(
                event.getForumId(),
                -1,
                -1,
                new Order(Direction.DESC, SortField.UPDATETIME.toString()),
                null,
                null)
            .stream()
            .map(TopicInfo::getTopicId)
            .collect(Collectors.toList());
    topicIdList.forEach(item -> topicService.deleteTopic(item, true));
    if (!event.getForumDdfId().isEmpty()) {
      fileService.delete(event.getForumDdfId(), DdfType.FORUM.toString(), event.getForumId());
    }
  }

  @EventListener
  public void handleCommunityDeletingEvent(CommunityDeletingEvent event) {
    log.debug("Community delete");
    List<Integer> forumIdList =
        forumService
            .searchForumInfoList(
                event.getCommunityId(),
                Arrays.stream(ForumType.values()).collect(Collectors.toList()),
                -1,
                -1,
                new Order(Direction.DESC, SortField.UPDATETIME.toString()))
            .stream()
            .map(ForumInfo::getForumId)
            .collect(Collectors.toList());
    forumIdList.forEach(
        item -> {
          forumService.deleteForum(item, new ForumStatusInput().status(ForumStatus.DELETE));
          forumService.deleteAllMemberJoinApplicationOfForum(item);
        });
    communityService.deleteAllMemberJoinApplicationOfCommunity(event.getCommunityId());
    if (!event.getCommunityDdfId().isEmpty()) {
      fileService.delete(event.getCommunityDdfId(), DdfType.COMMUNITY.toString(), event.getCommunityId());
    }
  }

  @EventListener
  public void handleForumLockingEvent(ForumLockingEvent event) {
    log.debug("Forum lock");
    List<Integer> topicIdList =
        topicService
            .getTopicOfForumWithSortAndLimit(
                event.getForumId(),
                -1,
                -1,
                new Order(Direction.DESC, SortField.UPDATETIME.toString()),
                null,
                null)
            .stream()
            .map(TopicInfo::getTopicId)
            .collect(Collectors.toList());
    topicIdList.forEach(
        item -> topicService.lockTopic(item, event.getUserId(), event.getLockedTime()));
  }

  @EventListener
  public void handleCommunityLockingEvent(CommunityLockingEvent event) {
    log.debug("Community lock");
    List<Integer> forumIdList =
        forumService
            .searchForumInfoList(
                event.getCommunityId(),
                Arrays.stream(ForumType.values()).collect(Collectors.toList()),
                -1,
                -1,
                new Order(Direction.DESC, SortField.UPDATETIME.toString()))
            .stream()
            .map(ForumInfo::getForumId)
            .collect(Collectors.toList());
    forumIdList.forEach(
        item -> {
          forumService.lockForum(item, event.getUserId(), event.getLockedTime());
          forumService.deleteAllMemberJoinApplicationOfForum(item);
        });
    communityService.deleteAllMemberJoinApplicationOfCommunity(event.getCommunityId());
    updateAllDdfUnderCommunity(event.getCommunityId());
  }

  @EventListener
  public void handleTopicDeletingEvent(TopicDeletingEvent event) {
    log.debug("Topic delete");
    communityService.updateLastModifiedOfCommunity(
        event.getCommunityId(), event.getUserId(), event.getTime());
    forumService.updateLastModifiedOfForum(event.getForumId(), event.getUserId(), event.getTime());
    List<Integer> replyIdList =
        replyService
            .getReplyListOfTopic(event.getTopicId(), -1, -1, Direction.DESC)
            .stream()
            .filter(item -> !item.getReplyStatus().equals(ReplyStatus.DELETE.toString()))
            .map(ReplyInfo::getReplyId)
            .collect(Collectors.toList());
    replyIdList.forEach(item -> replyService.deleteReply(item, true));
  }

  @TransactionalEventListener
  public void handleDDFDeleteQueueTriggerDeletingEvent(DDFDeleteQueueTriggerDeletingEvent event) {
    log.debug("DDF Delete Queue Trigger delete");
    int associatedId = event.getAssociatedId();
    List<String> ddfQueue = ddfDao.getDdfDeleteQueueByStatusAndAssociatedId(
            DdfQueueStatus.WAIT.getValue(), associatedId, DdfType.FILE.toString());
    ofNullable(ddfQueue)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(ids -> {
            ddfDao.updateDdfDeleteQueueStatus(ids, DdfQueueStatus.PROCESSING.getValue());
            ids.forEach(id -> {
                try {
                    log.debug("Trigger Delete " + id);
                    fileService.deleteRealFile(id);
                    ddfDao.deleteDdfDeleteQueue(id);
                } catch (NoSuchElementException e) {
                    log.warn(e);
                    ddfDao.deleteDdfDeleteQueue(id);
                } catch (Exception e) {
                    log.error(e);
                    ddfDao.upsertDdfDeleteQueue(id, DdfQueueStatus.WAIT.getValue(), e.getMessage());
                }
            });
        });
  }

  @EventListener
  public void handleTopicMovingEvent(TopicMovingEvent event) {
    log.debug("Topic Moving");
    communityService.updateLastModifiedOfCommunity(
        event.getCommunityId(), event.getUserId(), event.getTime());
    forumService.updateLastModifiedOfForum(event.getForumId(), event.getUserId(), event.getTime());
    communityAndForumChanging(event.getCommunityId(), event.getForumId());
  }

  private void communityAndForumChanging(int communityId, int forumId) {
    ddfDao.upsertDdfQueue(DdfType.FORUM.toString(), forumId, DdfQueueStatus.WAIT.getValue(), EMPTY);
    ddfDao.upsertDdfQueue(
        DdfType.COMMUNITY.toString(), communityId, DdfQueueStatus.WAIT.getValue(), EMPTY);
  }

  @EventListener
  public void syncForumUserId(SyncForumUserIdEvent event) {
    List<ForumInfo> forumInfos =
        forumService
            .getAllForumInfoByCommunityIdAndStatus(
                event.getCommunityId(), ForumStatus.OPEN.toString())
            .stream()
            .filter(item -> StringUtils.equals(ForumStatus.OPEN.toString(), item.getForumStatus()))
            .collect(Collectors.toList());
    List<User> communityAllMemberList =
        communityService.getAllMemberOfCommunityById(event.getCommunityId(), null,
                null, Role.COMMUNITY_MEMBER.getId(), -1);
    for (ForumInfo forumInfo : forumInfos) {
      List<User> adminList =
          forumService.getAdminListOfForum(forumInfo.getForumId(), -1, -1);
      List<User> memberList = forumService.getMemberOfForum(forumInfo.getForumId(), -1, -1);

      List<String> deletingMembers =
          forumService.getDeletingMemberInfo(adminList, memberList, communityAllMemberList);
      if (!deletingMembers.isEmpty()) {
        //forumService.deleteUserFromForum(forumInfo.getForumId(), deletingMembers);
      }
    }
  }

  @EventListener
  public void syncForumUserIdInHomePageEvent(SyncForumUserIdInHomePageEvent event) {
    int forumId = event.getForumId();
    List<User> communityAllMemberList =
        communityService.getAllMemberOfCommunityById(event.getCommunityId(), null, null, -1, -1);

    List<User> adminList = forumService.getAdminListOfForum(forumId, -1, -1);
    List<User> memberList = forumService.getMemberOfForum(forumId, -1, -1);

    List<String> deletingMembers =
        forumService.getDeletingMemberInfo(adminList, memberList, communityAllMemberList);
    if (!deletingMembers.isEmpty()) {
      //forumService.deleteUserFromForum(forumId, deletingMembers);
      ddfDao.upsertDdfQueue(
          DdfType.FORUM.toString(), event.getForumId(), DdfQueueStatus.WAIT.getValue(), EMPTY);
      List<Integer> topicIdList =
          topicService
              .getTopicOfForumWithSortAndLimit(
                  event.getForumId(),
                  -1,
                  -1,
                  new Order(Direction.DESC, SortField.UPDATETIME.toString()),
                  null,
                  null)
              .stream()
              .map(TopicInfo::getTopicId)
              .collect(Collectors.toList());
      topicIdList.forEach(
          topic ->
              ddfDao.upsertDdfQueue(
                  DdfType.TOPIC.toString(), topic, DdfQueueStatus.WAIT.getValue(), EMPTY));
    }
  }

  @EventListener
  public void handleCommunityReopeningEvent(CommunityReopeningEvent event) {
    log.debug("Community reopen");
    List<Integer> forumIdList =
        forumService
            .getAllForumInfoByCommunityIdAndStatus(
                event.getCommunityId(), ForumStatus.LOCKED.toString())
            .stream()
            .map(ForumInfo::getForumId)
            .collect(Collectors.toList());
    forumIdList.forEach(
        item -> forumService.reopenForum(item, event.getUserId(), event.getReopenedTime()));
    updateAllDdfUnderCommunity(event.getCommunityId());
  }

  @EventListener
  public void handleForumReopeningEvent(ForumReopeningEvent event) {
    log.debug("Forum reopen");
    List<Integer> topicIdList =
        topicService
            .getAllByForumIdAdnStatus(event.getForumId(), TopicStatus.LOCKED.toString())
            .stream()
            .map(TopicInfo::getTopicId)
            .collect(Collectors.toList());
    topicIdList.forEach(
        item ->
            topicService.reopenTopic(
                item, event.getUserId(), event.getReopenedTime(), TopicStatus.LOCKED.toString()));
  }

  @EventListener
  public void handleCommunityAvatarChangingEvent(CommunityAvatarChangingEvent event) {
    log.debug("Community avatar changing");
    ddfDao.upsertDdfQueue(
        DdfType.COMMUNITY.toString(),
        event.getCommunityId(),
        DdfQueueStatus.WAIT.getValue(),
        EMPTY);
  }
}
