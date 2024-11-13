package com.delta.dms.community.enums;

public enum EerppLanguage {
  ZH_TW("zh-tw", 1),

  ZH_CN("zh-cn", 2),

  EN_US("en-us", 3),

  UNDEFINED("undefined", 999);

  private String lang;
  private int order;

  EerppLanguage(String lang, int order) {
    this.lang = lang;
    this.order = order;
  }

  @Override
  public String toString() {
    return lang;
  }

  public int getOrder() {
    return order;
  }

  public static EerppLanguage fromLanguage(String lang) {
    for (EerppLanguage b : EerppLanguage.values()) {
      if (b.lang.equals(lang)) {
        return b;
      }
    }
    return EerppLanguage.UNDEFINED;
  }
}
