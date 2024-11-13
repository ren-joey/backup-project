package com.delta.dms.community.model.eerpm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorSolutionDetail {
  private String solutionCode;
  private String description;
}
