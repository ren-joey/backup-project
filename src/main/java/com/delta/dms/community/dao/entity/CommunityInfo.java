package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class CommunityInfo {
  private int communityId;
  private String communityName = "";
  private String communityDesc = "";
  private String communityType = "";
  private String communityCategory = "";
  private String communityStatus = "";
  private String communityImgBanner = "";
  private String communityImgAvatar = "";
  private String communityCreateUserId = "";
  private long communityCreateTime = 0;
  private String communityModifiedUserId = "";
  private long communityModifiedTime = 0;
  private String communityDeleteUserId = "";
  private long communityDeleteTime = 0;
  private String communityLastModifiedUserId = "";
  private long communityLastModifiedTime = 0;
  private String communityGroupId = "";
  private String communityDdfId = "";
  private String communityLanguage = "";
  private boolean dashboard = false;
  private String specialType = "";
}
