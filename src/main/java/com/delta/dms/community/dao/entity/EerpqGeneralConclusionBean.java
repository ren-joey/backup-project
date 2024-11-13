package com.delta.dms.community.dao.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqGeneralConclusionBean {
  private String phenomenonCode;
  private String failureCode;
  private String dutyCode;
  private String reasonCode;
  private String solutionCode;
  private String location;
}
