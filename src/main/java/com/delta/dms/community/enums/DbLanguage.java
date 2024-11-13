package com.delta.dms.community.enums;

import com.delta.dms.community.typehandler.GeneralEnumTypeHandler;

public enum DbLanguage {
  ZHTW("zhTw"),

  ZHCN("zhCn"),

  ENUS("enUs");

  private String value;

  DbLanguage(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static DbLanguage fromValue(String text) {
    return GeneralEnumTypeHandler.fromValue(DbLanguage.class, text);
  }
}
