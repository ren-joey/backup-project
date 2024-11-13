package com.delta.dms.community.adapter.entity;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MyDmsFolder {
  private int id;
  private String name;
  private Integer parent_id;
  private String gid;

  public int getParentId() {
    return ofNullable(parent_id).orElseGet(() -> INTEGER_ZERO);
  }
}
