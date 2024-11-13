package com.delta.dms.community.enums;

import org.apache.commons.codec.binary.StringUtils;

public enum ConclusionState {
  UNCONCLUDED(1, "unconcluded"),

  CONCLUDED(2, "concluded");

  private int id;
  private String value;

  ConclusionState(int id, String value) {
    this.id = id;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public int getId() {
    return id;
  }

  public static ConclusionState fromValue(String value) {
    for (ConclusionState b : ConclusionState.values()) {
      if (StringUtils.equals(b.value, value)) {
        return b;
      }
    }
    return null;
  }

  public static ConclusionState fromId(int id) {
    for (ConclusionState b : ConclusionState.values()) {
      if (b.id == id) {
        return b;
      }
    }
    return null;
  }

  public static boolean isConcluded(int id) {
    return CONCLUDED.getId() == id;
  }

  public static boolean isConcluded(String value) {
    return StringUtils.equals(CONCLUDED.toString(), value);
  }
}
