package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class EerpOriginCauseSolution {
  private String causeCode;
  private String causeDesc;
  private String originSolution;
  private String originSolutionId;
  private String newSolution;
}
