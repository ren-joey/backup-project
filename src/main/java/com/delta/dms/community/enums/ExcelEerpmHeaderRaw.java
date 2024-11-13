package com.delta.dms.community.enums;

public enum ExcelEerpmHeaderRaw {
  FACTORY("factory", "廠區名稱", 10),

  FORUM_NAME("forumName", "討論區名稱", 20),

  DEVICE_MODEL("deviceModel", "設備型號", 20),

  WORST_DEVICE("worstDevice", "最差設備ID", 20),

  ERROR_CODE("errorCode", "錯誤代碼", 10),

  ID("id", "設備型號組合", 40),

  ERROR_COUNT("errorCount", "錯誤次數", 10),

  CREATE_TOPIC_TIME("createTopicTime", "建立主題時間", 16),

  CONCLUSION_STATE("conclusionState", "是否做結論", 10),

  CREATE_CONCLUSION_TIME("createConclusionTime", "建立結論時間", 16),

  ACTION_COUNT("actionCount", "採用次數", 10),

  LEVEL("level", "問題嚴重度", 12),

  TOPIC_TITLE("topicTitle", "主題標題", 35),
  
  ERROR_DESC("errorDesc", "問題描述", 35),
  
  CAUSE("cause", "問題、對策", 35),
  
  ECN("ecn", "ECN", 35),
  
  PCN("pcn", "PCN", 35),
  
  DFAUTO("dfauto", "DFauto", 35),
  
  AMBU("ambu", "AMBU", 35);

  private String key;
  private String header;
  private int width;

  ExcelEerpmHeaderRaw(String key, String header, int width) {
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

  public static ExcelEerpmHeaderRaw fromKey(String key) {
    for (ExcelEerpmHeaderRaw b : ExcelEerpmHeaderRaw.values()) {
      if (b.key.equals(key)) {
        return b;
      }
    }
    return null;
  }
}
