package com.delta.dms.community.enums;

public enum EerpErrorLevel {
  LEVEL_1(1, "Top 3 x 1", 1),
  LEVEL_2(2, "Top 3 x 2", 2),
  LEVEL_3(3, "Top 3 ≥ 3", 3);

  private int id;
  private String value;
  private int count;

  private EerpErrorLevel(int id, String value, int count) {
    this.id = id;
    this.value = value;
    this.count = count;
  }

  @Override
  public String toString() {
    return value;
  }

  public int getId() {
    return id;
  }

  public static EerpErrorLevel fromCount(int count) {
    if (LEVEL_3.count <= count) {
      return LEVEL_3;
    }
    for (EerpErrorLevel b : EerpErrorLevel.values()) {
      if (b.count == count) {
        return b;
      }
    }
    return null;
  }

  public static EerpErrorLevel fromId(int id) {
    for (EerpErrorLevel b : EerpErrorLevel.values()) {
      if (b.id == id) {
        return b;
      }
    }
    return null;
  }

  public static EerpErrorLevel fromValue(String value) {
    for (EerpErrorLevel b : EerpErrorLevel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}
