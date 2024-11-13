package com.delta.dms.community.model.eerpq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqDutyCodeData {
  private String dutyCode;
  private String dutyDesc;

  @JsonProperty("dutyDesc_en")
  private String dutyDescEn;
}
