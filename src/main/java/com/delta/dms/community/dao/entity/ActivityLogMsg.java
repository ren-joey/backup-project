package com.delta.dms.community.dao.entity;

import java.util.Map;
import com.delta.datahive.activitylog.args.Activity;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.datahive.activitylog.args.SourceOS;

public class ActivityLogMsg {

  private App app;
  private String appVersion = "";
  private String userId = "";
  private Activity activity;
  private ObjectType objectType;
  private String objectId;
  private Map<String, String> annotation;
  private LogStatus logStatus;
  private LogTimeUnit logTimeUnit;
  private ObjectType parentObjectType;
  private String parentObjectId;
  private SourceOS sourceOs;

  public void setApp(App app) {
    this.app = app;
  }

  public App getApp() {
    return app;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }

  public void setActivity(Activity activity) {
    this.activity = activity;
  }

  public Activity getActivity() {
    return activity;
  }

  public void setObjectType(ObjectType objectType) {
    this.objectType = objectType;
  }

  public ObjectType getObjectType() {
    return objectType;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setAnnotation(Map<String, String> annotation) {
    this.annotation = annotation;
  }

  public Map<String, String> getAnnotation() {
    return annotation;
  }

  public void setLogStatus(LogStatus logStatus) {
    this.logStatus = logStatus;
  }

  public LogStatus getLogStatus() {
    return logStatus;
  }

  public void setLogTimeUnit(LogTimeUnit logTimeUnit) {
    this.logTimeUnit = logTimeUnit;
  }

  public LogTimeUnit getLogTimeUnit() {
    return logTimeUnit;
  }

  public void setParentObjectType(ObjectType parentObjectType) {
    this.parentObjectType = parentObjectType;
  }

  public ObjectType getParentObjectType() {
    return parentObjectType;
  }

  public void setParentObjectId(String parentObjectId) {
    this.parentObjectId = parentObjectId;
  }

  public String getParentObjectId() {
    return parentObjectId;
  }

  public SourceOS getSourceOs() {
    return sourceOs;
  }

  public void setSourceOs(SourceOS sourceOs) {
    this.sourceOs = sourceOs;
  }

  @Override
  public String toString() {
    return "ActivityLogMsg [app="
        + app
        + ", appVersion="
        + appVersion
        + ", userId="
        + userId
        + ", activity="
        + activity
        + ", objectType="
        + objectType
        + ", objectId="
        + objectId
        + ", annotation="
        + annotation
        + ", logStatus="
        + logStatus
        + ", logTimeUnit="
        + logTimeUnit
        + ", parentObjectType="
        + parentObjectType
        + ", parentObjectId="
        + parentObjectId
        + ", sourceOs="
        + sourceOs
        + "]";
  }
}
