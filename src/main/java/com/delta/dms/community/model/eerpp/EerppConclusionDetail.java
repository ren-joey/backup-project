package com.delta.dms.community.model.eerpp;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerppConclusionDetail {
  private int topicId;
  private String factory;
}
