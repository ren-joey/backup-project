package com.delta.dms.community.model.eerpq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqFailureCodeData {
  private String failureCode;
  private String failureDesc;

  @JsonProperty("failureDesc_en")
  private String failureDescEn;
}
