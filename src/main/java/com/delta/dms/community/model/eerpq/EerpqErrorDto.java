package com.delta.dms.community.model.eerpq;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqErrorDto {
  private int pageNum;
  private int pageSize;
  private String factory;
  private String phenomenonyCode;
  private String dutyCode;
}
