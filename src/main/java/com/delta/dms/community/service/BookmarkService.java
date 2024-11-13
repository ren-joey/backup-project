package com.delta.dms.community.service;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.datahive.activitylog.UserEchoAPI;
import com.delta.datahive.activitylog.UserEchoException;
import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.Echo;
import com.delta.datahive.activitylog.args.EchoGroup;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.dms.community.dao.BookmarkDao;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;

@Service
@Transactional
public class BookmarkService {

  private BookmarkDao bookmarkDao;
  private ForumService forumService;
  private EventPublishService eventPublishService;
  private static final LogUtil log = LogUtil.getInstance();

  public BookmarkService(
      BookmarkDao bookmarkDao, ForumService forumService, EventPublishService eventPublishService) {
    this.bookmarkDao = bookmarkDao;
    this.forumService = forumService;
    this.eventPublishService = eventPublishService;
  }

  public void setBookmarkOfTopic(Integer topicId, Integer forumId) {
    bookmarkDao.setBookmark(
        Utility.getUserIdFromSession(),
        setPermissionObject(forumId).toString(),
        topicId,
        System.currentTimeMillis());

    try {
      UserEchoAPI.set(
          Utility.getUserIdFromSession(), Echo.KEEP, ObjectType.TOPICID, String.valueOf(topicId));
    } catch (UserEchoException e) {
      log.warn(e);
    }
    eventPublishService.publishActivityLogMsgEvent(
        ActivityLogUtil.convertToActivityLogMsg(
            App.COMMUNITY,
            Constants.ACTIVITY_APP_VERSION,
            Utility.getUserIdFromSession(),
            Activity.KEEP,
            ObjectType.TOPICID,
            String.valueOf(topicId),
            null,
            null,
            null,
            ObjectType.FORUMID,
            String.valueOf(forumId)));
  }

  public void removeBookmarkOfTopic(Integer topicId, Integer forumId) {
    bookmarkDao.removeBookmark(
        Utility.getUserIdFromSession(), setPermissionObject(forumId).toString(), topicId);

    try {
      UserEchoAPI.remove(
          Utility.getUserIdFromSession(),
          EchoGroup.KEEP,
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
            Activity.UNKEEP,
            ObjectType.TOPICID,
            String.valueOf(topicId),
            null,
            null,
            null,
            ObjectType.FORUMID,
            String.valueOf(forumId)));
  }

  private PermissionObject setPermissionObject(int forumId) {
    ForumInfo forumInfo = forumService.getForumInfoById(forumId);
    return !Objects.equals(ForumType.PRIVATE, ForumType.fromValue(forumInfo.getForumType()))
        ? PermissionObject.PUBLICFORUMTOPIC
        : PermissionObject.PRIVATEFORUMTOPIC;
  }

  public boolean checkUserBookmark(String userId, PermissionObject permissionObject, int id) {
    return bookmarkDao.checkUserBookmark(userId, permissionObject.toString(), id) != 0;
  }
}
