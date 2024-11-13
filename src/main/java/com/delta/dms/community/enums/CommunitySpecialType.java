package com.delta.dms.community.enums;

import com.delta.dms.community.typehandler.GeneralEnumTypeHandler;
import lombok.Getter;

@Getter
public enum CommunitySpecialType {
  NONE(""),

  EERPP("eerpp"),

  EERPQ("eerpq"),

  EERPM("eerpm");

  private String value;

  CommunitySpecialType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static CommunitySpecialType fromValue(String text) {
    return GeneralEnumTypeHandler.fromValue(CommunitySpecialType.class, text);
  }
}
