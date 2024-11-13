package com.delta.dms.community.model.eerpp;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerppTopicRawData {
  private String factory;
  private String area;
  private String lossCode;
  private String reasonCode;
  private String typeCode;
  private String delayType;
  private Map<String, String> lossCodeDesc;
  private Map<String, String> reasonCodeDesc;
  private List<EerppSolutionDetail> solution;
}
