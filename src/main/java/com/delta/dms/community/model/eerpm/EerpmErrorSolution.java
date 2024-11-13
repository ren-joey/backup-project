package com.delta.dms.community.model.eerpm;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorSolution {
  private String typeCode;
  private String errorCode;
  private String causeCode;
  private List<EerpmErrorSolutionDetail> solutionList;
}
