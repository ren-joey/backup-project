package com.delta.dms.community.model.eerpm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmCodeStatus {
  private String code;
  private String desc;
  private int status;
}
