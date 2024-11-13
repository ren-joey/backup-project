package com.delta.dms.community.enums;

public enum CommunityMemberTypeEnum {
  ADMIN("admin", 1),
  MEMBER("member", 2);

  private final String value;
  private final Integer number;

  private CommunityMemberTypeEnum(String value, Integer number) {
    this.value = value;
    this.number = number;
  }

  @Override
  public String toString() {
    return value;
  }

  public Integer getNumber() {
    return number;
  }
}
