package com.delta.dms.community.dao.entity;

public class CommunitiesCreationInfo {
  private int batchId = 0;
  private String communityName = "";
  private String communityDesc = "";
  private String communityCategory = "";
  private String applicantId = "";
  private long applicationTime = 0;
  private String reviewerId = "";
  private long reviewTime = 0;
  private String status = "";
  private String communityType = "";
  private String rejectedMessage = "";
  private String cname = "";
  private String language = "";

  public int getBatchId() {
    return batchId;
  }

  public void setBatchId(int batchId) {
    this.batchId = batchId;
  }

  public String getCommunityName() {
    return communityName;
  }

  public void setCommunityName(String communityName) {
    this.communityName = communityName;
  }

  public String getCommunityDesc() {
    return communityDesc;
  }

  public void setCommunityDesc(String communityDesc) {
    this.communityDesc = communityDesc;
  }

  public String getCommunityCategory() {
    return communityCategory;
  }

  public void setCommunityCategory(String communityCategory) {
    this.communityCategory = communityCategory;
  }

  public String getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(String applicantId) {
    this.applicantId = applicantId;
  }

  public long getApplicationTime() {
    return applicationTime;
  }

  public void setApplicationTime(long applicationTime) {
    this.applicationTime = applicationTime;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
  }

  public long getReviewTime() {
    return reviewTime;
  }

  public void setReviewTime(long reviewTime) {
    this.reviewTime = reviewTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCommunityType() {
    return communityType;
  }

  public void setCommunityType(String communityType) {
    this.communityType = communityType;
  }

  public String getRejectedMessage() {
    return rejectedMessage;
  }

  public void setRejectedMessage(String rejectedMessage) {
    this.rejectedMessage = rejectedMessage;
  }

  public String getCname() {
    return cname;
  }

  public void setCname(String cname) {
    this.cname = cname;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  @Override
  public String toString() {
    return "CommunitiesCreationInfo [batchId="
        + batchId
        + ", communityName="
        + communityName
        + ", communityDesc="
        + communityDesc
        + ", communityCategory="
        + communityCategory
        + ", applicantId="
        + applicantId
        + ", applicationTime="
        + applicationTime
        + ", reviewerId="
        + reviewerId
        + ", reviewTime="
        + reviewTime
        + ", status="
        + status
        + ", communityType="
        + communityType
        + ", rejectedMessage="
        + rejectedMessage
        + ", cname="
        + cname
        + ", language="
        + language
        + "]";
  }
}
