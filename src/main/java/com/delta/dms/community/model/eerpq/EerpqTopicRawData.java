package com.delta.dms.community.model.eerpq;

import java.util.List;
import lombok.Data;

@Data
public class EerpqTopicRawData {
  private String factory;
  private String empNo;
  private String empName;
  private String ntAccount;
  private String troubleDesc;
  private List<EerpqRepairData> repairList;
}
