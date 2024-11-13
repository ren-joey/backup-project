package com.delta.dms.community.enums;

public enum EerpRuleCellType {
  RANGE_DAY("rangeDay"),
  DROPDOWN("dropdown"),
  AUTO_COMPLETE("autoComplete");

  private String value;

  private EerpRuleCellType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static EerpRuleCellType fromValue(String value) {
    for (EerpRuleCellType b : EerpRuleCellType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}
