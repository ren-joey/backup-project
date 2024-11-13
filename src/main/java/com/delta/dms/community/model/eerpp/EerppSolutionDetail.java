package com.delta.dms.community.model.eerpp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class EerppSolutionDetail extends EerppSolution {
  private String line;
  private int duration;
  private String startTime;
  private String endTime;
  private int frequency;
}
