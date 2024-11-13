package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DdfQueueStatus {
  WAIT("wait"),

  PROCESSING("processing"),

  PASS("pass");

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
