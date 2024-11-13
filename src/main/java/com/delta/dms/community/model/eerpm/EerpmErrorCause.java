package com.delta.dms.community.model.eerpm;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorCause {
  private String typeCode;
  private String errorCode;
  private List<EerpmErrorCauseDetail> causeList;
}
