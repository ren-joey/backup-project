package com.delta.dms.community.enums;

public enum EerpmDashboardTableColumn {
  FACTORY("factory", "factory"),
  FORUM("forum", "forum_name"),
  TOPIC_TITLE("topicTitle", "topic_title"),
  CONCLUSION("conclusion", "conclusion_state_id"),
  DEVICE_MODEL("deviceModel", "device_model"),
  WORST_DEVICES("worstDevices", "worst_device_id"),
  ERROR_CODE("errorCode", "error_code"),
  ERROR_COUNT("errorCount", "error_count"),
  ERROR_LEVEL("errorLevel", "");

  private String value;
  private String dbColumnName;

  private EerpmDashboardTableColumn(String value, String dbColumnName) {
    this.value = value;
    this.dbColumnName = dbColumnName;
  }

  @Override
  public String toString() {
    return value;
  }

  public String getDbColumnName() {
    return dbColumnName;
  }

  public static EerpmDashboardTableColumn fromValue(String value) {
    for (EerpmDashboardTableColumn b : EerpmDashboardTableColumn.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}
