package com.delta.dms.community.model.eerpq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqReasonCodeData {
  private String reason;
  private String reasonDesc;

  @JsonProperty("reasonDesc_en")
  private String reasonDescEn;
}
