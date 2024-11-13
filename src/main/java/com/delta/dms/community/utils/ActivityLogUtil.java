package com.delta.dms.community.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.dms.community.dao.entity.ActivityLogMsg;
import com.delta.dms.community.model.SourceOsParam;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;

public class ActivityLogUtil {

  private static final String HEADER_SOURCE_OS_VERSION_KEY = "Source-OS-Version";

  private ActivityLogUtil() {}

  public static App getAppName(String origin) {
    App appName = null;
    switch (origin) {
      case Constants.INFO_PROJECT_NAME:
        appName = App.COMMUNITY;
        break;
      case Constants.INFO_NAME_DMS:
        appName = App.MYDMS;
        break;
      default:
        break;
    }
    return appName;
  }

  public static ObjectType getObjectType(ObjectType objectType, String attachmentId) {
    if (Constants.ATTACHMENTID_EMPTY.equals(attachmentId)) {
      return objectType;
    } else {
      return ObjectType.DOCUUID;
    }
  }

  public static String getObjectId(Integer objectId, String attachmentId) {
    if (Constants.ATTACHMENTID_EMPTY.equals(attachmentId)) {
      return String.valueOf(objectId);
    } else {
      return attachmentId;
    }
  }

  // Architecture team's "Activity" doesn't include the operations: REVIEW, NOTIFY
  @SuppressWarnings("incomplete-switch")
  public static Activity getOperationEnumOfActivityLog(Operation operation) {
    Activity activity = null;
    switch (operation) {
      case CREATE:
        activity = Activity.CREATE;
        break;
      case READ:
        activity = Activity.READ;
        break;
      case UPDATE:
        activity = Activity.UPDATE;
        break;
      case DELETE:
        activity = Activity.DELETE;
        break;
      case DOWNLOAD:
        activity = Activity.DOWNLOAD;
        break;
      case LOGIN:
        activity = Activity.LOGIN;
        break;
      case UPLOAD:
        activity = Activity.CREATE;
        break;
      case PIN:
        activity = Activity.PIN;
        break;
      case CLICK:
        activity = Activity.CLICK;
        break;
      default:
        break;
    }
    return activity;
  }

  public static Map<String, String> getAnnotation(
      PermissionObject permissionObject, String content) {
    Map<String, String> annotation = new HashMap<>();
    annotation.put(Constants.ACTIVITY_LOG_OBJECT, permissionObject.toString());

    if (null != content && !Constants.ATTACHMENTID_EMPTY.equals(content)) {
      annotation.put(Constants.ACTIVITY_LOG_CONTENT, content);
    }
    return annotation;
  }

  public static ActivityLogMsg convertToActivityLogMsg(
      App appName,
      String appVersion,
      String userId,
      Activity activity,
      ObjectType objectType,
      String objectId,
      Map<String, String> annotation,
      LogStatus logStatus,
      LogTimeUnit logTimeUnit,
      ObjectType parentObjectType,
      String parentObjectId) {
    ActivityLogMsg activityLogMsg = new ActivityLogMsg();
    activityLogMsg.setApp(appName);
    activityLogMsg.setAppVersion(appVersion);
    activityLogMsg.setUserId(userId);
    activityLogMsg.setActivity(activity);
    activityLogMsg.setObjectType(objectType);
    activityLogMsg.setObjectId(objectId);
    activityLogMsg.setAnnotation(annotation);
    activityLogMsg.setLogStatus(logStatus);
    activityLogMsg.setLogTimeUnit(logTimeUnit);
    activityLogMsg.setParentObjectType(parentObjectType);
    activityLogMsg.setParentObjectId(parentObjectId);
    activityLogMsg.setSourceOs(SourceOsParam.get());

    String sourceOsVersion = Utility.getRequestHeaderAttribute(HEADER_SOURCE_OS_VERSION_KEY);
    if (StringUtils.isNotBlank(sourceOsVersion)) {
      Optional.ofNullable(annotation)
          .orElseGet(HashMap::new)
          .put(Constants.ACTIVITY_LOG_SOURCE_OS_VERSION, sourceOsVersion);
      activityLogMsg.setAnnotation(annotation);
    }
    return activityLogMsg;
  }
}
