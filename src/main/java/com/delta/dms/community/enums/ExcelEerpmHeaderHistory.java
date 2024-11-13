package com.delta.dms.community.enums;

public enum ExcelEerpmHeaderHistory {
  ID("id", "設備型號組合", 40),

  FACTORY("factory", "廠區名稱", 10),

  FORUM_NAME("forumName", "討論區名稱", 20),

  DEVICE_MODEL("deviceModel", "設備型號", 20),

  ERROR_CODE("errorCode", "錯誤代碼", 10),

  COUNT("count", "", 12),

  LEVEL("level", "問題嚴重度", 12);

  private String key;
  private String header;
  private int width;

  ExcelEerpmHeaderHistory(String key, String header, int width) {
    this.key = key;
    this.header = header;
    this.width = width;
  }

  @Override
  public String toString() {
    return key;
  }

  public String getKey() {
    return key;
  }

  public String getHeader() {
    return header;
  }

  public int getWidth() {
    return width;
  }

  public static ExcelEerpmHeaderHistory fromKey(String key) {
    for (ExcelEerpmHeaderHistory b : ExcelEerpmHeaderHistory.values()) {
      if (b.key.equals(key)) {
        return b;
      }
    }
    return null;
  }
}
