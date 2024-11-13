package com.delta.dms.community.dao.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IdNameEntity {
  private Object id;
  private String name;
}
