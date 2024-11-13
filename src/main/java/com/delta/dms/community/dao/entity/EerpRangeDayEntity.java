package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class EerpRangeDayEntity {
  private boolean allowLimit;
  private int fromDay;
  private int endDay;
}
