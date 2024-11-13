package com.delta.dms.community.model.eerpm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorDetail {
  private String typeCode;
  private String errorCode;
  private String causeCode;
}
