package com.delta.dms.community.enums;

public enum EerppDashboardTableColumn {
  FACTORY("factory", "factory"),
  DEPARTMENT("department", "department"),
  FORUM("forum", "forum_name"),
  AREA("area", "area"),
  AREAS("areas", "area"),
  LOSS_CODE("lossCode", "loss_code"),
  LOSS_DESC("lossDescription", "loss_code_desc"),
  LOSS_DESCS("lossDescriptions", "loss_code_desc"),
  DURATION("duration", "duration"),
  CONCLUSION("conclusion", "conclusion_state_id"),
  LINE("lines", "line"),
  ERROR_LEVEL("errorLevel", "");

  private String value;
  private String dbColumnName;

  private EerppDashboardTableColumn(String value, String dbColumnName) {
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

  public static EerppDashboardTableColumn fromValue(String value) {
    for (EerppDashboardTableColumn b : EerppDashboardTableColumn.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}
