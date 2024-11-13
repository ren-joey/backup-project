package com.delta.dms.community.dao.entity;

public class CommunityData {
  private int communityId;
  private Integer batchId = 0;
  private String communityType = "";
  private String communityStatus = "";

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public Integer getBatchId() {
    return batchId;
  }

  public void setBatchId(Integer batchId) {
    this.batchId = batchId;
  }

  public String getCommunityType() {
    return communityType;
  }

  public void setCommunityType(String communityType) {
    this.communityType = communityType;
  }

  public String getCommunityStatus() {
    return communityStatus;
  }

  public void setCommunityStatus(String communityStatus) {
    this.communityStatus = communityStatus;
  }

  @Override
  public String toString() {
    return "CommunityData [communityId="
        + communityId
        + ", batchId="
        + batchId
        + ", communityType="
        + communityType
        + ", communityStatus="
        + communityStatus
        + "]";
  }
}
