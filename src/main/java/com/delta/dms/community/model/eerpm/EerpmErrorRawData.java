package com.delta.dms.community.model.eerpm;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmErrorRawData {
  private String description;
  private String cause;
  private String causeId;
  private String quickCountermeasure;
  private String quickCountermeasureId;
  private String temporaryCountermeasure;
  private int actioncount;
}
