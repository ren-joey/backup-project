package com.delta.dms.community.dao.entity;

import com.delta.dms.community.swagger.model.DiaMemberType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiaMemberEntity {
  private String userId;
  private DiaMemberType userType;
}
