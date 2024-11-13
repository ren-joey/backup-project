package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DdfCustomField {
  COMMUNITY_MEMBER_COUNT("communityMemberCount"),

  COMMUNITY_AVATAR("communityAvatar"),

  VIDEO_ID("videoId");

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
