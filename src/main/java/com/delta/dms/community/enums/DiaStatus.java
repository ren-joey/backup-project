package com.delta.dms.community.enums;

public enum DiaStatus {
  WAIT("wait"),
  ATTACHMENT_CREATED("attachmentCreated"),
  CREATING("creating"),
  SUCCESS("success"),
  FAIL("fail");

  private String value;

  private DiaStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static DiaStatus fromValue(String status) {
    for (DiaStatus b : DiaStatus.values()) {
      if (b.value.equals(status)) {
        return b;
      }
    }
    return null;
  }
}
