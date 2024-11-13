package com.delta.dms.community.model.eerpm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmOriginCauseSolution {
  private EerpmCodeStatus cause;
  private EerpmCodeStatus improveSolution;
  private EerpmCodeStatus newSolution;
}
