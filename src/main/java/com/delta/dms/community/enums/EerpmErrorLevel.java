package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EerpmErrorLevel {
  LEVEL_1(1, "Top 3 x 1", 1, 1),
  LEVEL_2(2, "Top 3 x 2", 2, 2),
  LEVEL_3(3, "Top 3 ~ 6", 3, 6),
  LEVEL_HIGH(4, "高發 ≥ 7", 7, -1);

  private int id;
  private String value;
  private int minCount;
  private int maxCount;

  @Override
  public String toString() {
    return value;
  }

  public int getId() {
    return id;
  }

  public static EerpmErrorLevel fromCount(int count) {
    if (LEVEL_HIGH.minCount <= count) {
      return LEVEL_HIGH;
    }
    for (EerpmErrorLevel b : EerpmErrorLevel.values()) {
      if (b.minCount <= count && b.maxCount >= count) {
        return b;
      }
    }
    return null;
  }

  public static EerpmErrorLevel fromId(int id) {
    for (EerpmErrorLevel b : EerpmErrorLevel.values()) {
      if (b.id == id) {
        return b;
      }
    }
    return null;
  }

  public static EerpmErrorLevel fromValue(String value) {
    for (EerpmErrorLevel b : EerpmErrorLevel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}
