package com.delta.dms.community.model.eerpp;

import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerppSolution {
  private String solutionCode = "";
  private Map<String, String> solutionCodeDesc;
}
