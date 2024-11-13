package com.delta.dms.community.enums;

public enum EerpmSolutionType {
  EFFECTIVE_SOLUTION(1, "Effective", "有效對策", "有效对策"),
  INEFFECTIVE_SOLUTION(2, "Ineffective", "無效對策", "无效对策");

  private int id;
  private String valueEn;
  private String valueTw;
  private String valueCn;

  private EerpmSolutionType(int id, String valueEn, String valueTw, String valueCn) {
    this.id = id;
    this.valueEn = valueEn;
    this.valueTw = valueTw;
    this.valueCn = valueCn;
  }

  @Override
  public String toString() {
    return valueTw;
  }

  public int getId() {
    return id;
  }

  public String getValue(DbLanguage lang) {
    switch (lang) {
      case ENUS:
        return valueEn;
      case ZHCN:
        return valueCn;
      default:
        return valueTw;
    }
  }

  public static EerpmSolutionType fromId(int id) {
    for (EerpmSolutionType b : EerpmSolutionType.values()) {
      if (b.id == id) {
        return b;
      }
    }
    return null;
  }
}
