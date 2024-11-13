package com.delta.dms.community.bean.notification.v2;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NotificationV2Entities {
  COMMUNITY_REVIEW("Sys-8-12");

  @JsonValue private String key;

  @Override
  public String toString() {
    return key;
  }
}
