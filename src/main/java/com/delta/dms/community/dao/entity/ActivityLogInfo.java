package com.delta.dms.community.dao.entity;

public class ActivityLogInfo {

  private Integer id = 0;
  private String userId = "";
  private String operation = "";
  private String object = "";
  private Integer objectPk = 0;
  private Long operationTime = (long) 0;
  private String origin = "community";
  private String content = "";
  private String attachmentId = "";

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  public Integer getObjectPk() {
    return objectPk;
  }

  public void setObjectPk(Integer objectPk) {
    this.objectPk = objectPk;
  }

  public Long getOperationTime() {
    return operationTime;
  }

  public void setOperationTime(Long operationTime) {
    this.operationTime = operationTime;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public void setAttachmentId(String attachmentId) {
    this.attachmentId = attachmentId;
  }

  @Override
  public String toString() {
    return "ActivityLogInfo [id="
        + id
        + ", userId="
        + userId
        + ", operation="
        + operation
        + ", object="
        + object
        + ", objectPk="
        + objectPk
        + ", operationTime="
        + operationTime
        + ", origin="
        + origin
        + ", content="
        + content
        + ", attachmentId="
        + attachmentId
        + "]";
  }
}
