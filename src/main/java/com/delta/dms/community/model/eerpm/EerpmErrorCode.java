package com.delta.dms.community.model.eerpm;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorCode {
  private String typeCode;
  private List<EerpmErrorCodeDetail> errorCodeList;
}
