package com.delta.dms.community.enums;

public enum ExcelEerppHeader {
  AREA("area", "生產區域", 20),

  LOSS_CODE("lossCode", "Loss Code", 15),

  LOSS_DESC_ZH("lossDescZh", "Loss 中文描述", 35),

  LOSS_DESC_EN("lossDescEn", "Loss Description", 35),

  TYPE_CODE("typeCode", "Type Code", 15),

  DELAY_TYPE("delayType", "Delay Type", 15),

  STATUS("status", "新增", 6),

  REASON_CODE("reasonCode", "Reason Code", 15),

  REASON_DESC_ZH("reasonDescZh", "Reason 中文描述", 35),

  REASON_DESC_EN("reasonDescEn", "Reason Description", 35),

  SOLUTION_CODE("solutionCode", "Solution Code", 15),

  SOLUTION_DESC_ZH("solutionDescZh", "Solution 中文描述", 35),

  SOLUTION_DESC_EN("solutionDescEn", "Solution Description", 35);

  private String key;
  private String header;
  private int width;

  ExcelEerppHeader(String key, String header, int width) {
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

  public static ExcelEerppHeader fromKey(String key) {
    for (ExcelEerppHeader b : ExcelEerppHeader.values()) {
      if (b.key.equals(key)) {
        return b;
      }
    }
    return null;
  }
}
