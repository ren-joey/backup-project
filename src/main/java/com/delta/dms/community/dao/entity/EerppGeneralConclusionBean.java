package com.delta.dms.community.dao.entity;

import java.util.List;
import java.util.Map;
import com.delta.dms.community.model.eerpp.EerppSolution;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerppGeneralConclusionBean {
  private String area;
  private String lossCode;
  private Map<String, String> lossCodeDesc;
  private Map<String, String> reasonCodeDesc;
  private List<EerppSolution> originSolution;
  private List<EerppSolution> newSolution;
}
