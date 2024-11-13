package com.delta.dms.community.dao.entity;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmManualConclusionBean {
  private String deviceModel;
  private String errorCode;
  private String errorDesc;
  private String causeCode;
  private String solutionCode;
  private List<EerpOriginCauseSolution> originCauseSolution;
  private List<EerpNewCauseSolution> newCauseSolution;
  private List<String> ecn;
  private List<String> pcn;
  private String dfauto;
  private EerpAmbu ambu;
}
