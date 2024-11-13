package com.delta.dms.community.enums;

import com.delta.dms.community.typehandler.GeneralEnumTypeHandler;

public enum ExcelEerpmTitle {
  CAUSE("原因%s："),

  OLD_SOLUTION("舊快排優化："),

  NEW_SOLUTION("新增快排："),

  AMBU_SOFTWARE("AMBU-電控軟體優化："),

  AMBU_MECHANISM("AMBU-機構結構優化：");

  private String value;

  ExcelEerpmTitle(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static ExcelEerpmTitle fromValue(String text) {
    return GeneralEnumTypeHandler.fromValue(ExcelEerpmTitle.class, text);
  }
}
