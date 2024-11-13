package com.delta.dms.community.model.eerpm;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmTopicHistory {
  private long createTime;
  private String errorCode;
  private long errorCount;
  private List<String> worstDeviceIds;
  private String errorDesc;
  private List<String> causes = new ArrayList<>();
  private List<String> solutions = new ArrayList<>();
}
