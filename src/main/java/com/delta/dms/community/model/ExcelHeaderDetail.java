package com.delta.dms.community.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExcelHeaderDetail {
  private String key;
  private String value;
  private int width;
}
