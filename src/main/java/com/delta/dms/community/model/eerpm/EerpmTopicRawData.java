package com.delta.dms.community.model.eerpm;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmTopicRawData {
  private String areaTittle;
  private List<EerpmDeviceRawData> deviceDatas;
  private boolean isSummary;
  private List<EerpmTopicHistory> histories = new ArrayList<>();

  private Object areaDataArray;
  private Object fixedColArray;

  public void setIsSummary(boolean isSummary) {
    this.isSummary = isSummary;
  }
}
