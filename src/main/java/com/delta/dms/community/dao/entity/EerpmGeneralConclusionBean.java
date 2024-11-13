package com.delta.dms.community.dao.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmGeneralConclusionBean {
  private String deviceModel = "";
  private String errorCode = "";
  private String errorDesc = "";
  private List<EerpOriginCauseSolution> originCauseSolution = new ArrayList<>();
  private List<EerpNewCauseSolution> newCauseSolution = new ArrayList<>();
  private List<String> ecn = new ArrayList<>();
  private List<String> pcn = new ArrayList<>();
  private String dfauto = "";
  private EerpAmbu ambu = new EerpAmbu();
}
