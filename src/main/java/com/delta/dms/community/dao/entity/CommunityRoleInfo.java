package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class CommunityRoleInfo {
  private String userId = "";
  private String groupId = "";
  private int communityId = 0;
  private int roleId = 0;
  private boolean isGeneratedFromApp = false;
  private String uid = "";
  private String gid = "";
  private String mainGroupId = null;
  private String mainGroupName = null;
  private boolean isFromGroupList = false;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getGroupId() {
    return groupId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public int getCommunityId() {
    return communityId;
  }

  public void setCommunityId(int communityId) {
    this.communityId = communityId;
  }

  public int getRoleId() {
    return roleId;
  }

  public void setRoleId(int roleId) {
    this.roleId = roleId;
  }

  public boolean getIsGeneratedFromApp() {
    return isGeneratedFromApp;
  }
  public void setIsGeneratedFromApp(boolean isGeneratedFromApp) {
    this.isGeneratedFromApp = isGeneratedFromApp;
  }

  public String getMainGroupId() {
    return mainGroupId;
  }
  public void setMainGroupId(String mainGroupId) {
    this.mainGroupId = mainGroupId;
  }

  public String getMainGroupName() {
    return mainGroupName;
  }

  public void setMainGroupName(String mainGroupName) {
    this.mainGroupName = mainGroupName;
  }

  public boolean getIsFromGroupList() {
    return isFromGroupList;
  }

  public void setIsFromGroupList(boolean isFromGroupList) {
    this.isFromGroupList = isFromGroupList;
  }

  @Override
  public String toString() {
    return "CommunityRoleInfo [userId="
        + userId
        + ", communityId="
        + groupId
        + ", groupId="
        + communityId
        + ", roleId="
        + roleId
        + ", isGeneratedFromApp="
        + (isGeneratedFromApp ? "true" : "false")
        + ", mainGroupId="
        + mainGroupId
        + ", mainGroupName="
        + mainGroupName
        + ", isFromGroupList="
        + (isFromGroupList ? "true" : "false")
        + "]";
  }
}
