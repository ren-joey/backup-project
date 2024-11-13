package com.delta.dms.community.model.eerpm;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmDeviceRawData {
  private String deviceModel;
  private String factory;
  private String errorCode;
  private int errorCount;
  private List<String> worstDeviceId;
  private List<EerpmErrorRawData> methods;
}
