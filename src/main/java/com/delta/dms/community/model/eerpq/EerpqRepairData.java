package com.delta.dms.community.model.eerpq;

import lombok.Data;

@Data
public class EerpqRepairData {
  private String sn;
  private String model;
  private String moNumber;
  private String line;
  private String section;
  private String custCode;
  private String group;
  private String station;
  private String tester;
  private String testTime;
  private String errorCode;
  private String finishFlag;
}
