package com.delta.dms.community.enums;

public enum DiaAttachmentPathStatus {
  WAIT("wait"),
  PROCESSING("processing"),
  CHECKED("checked"),
  DOWNLOADED("downloaded");

  private String value;

  private DiaAttachmentPathStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static DiaAttachmentPathStatus fromValue(String status) {
    for (DiaAttachmentPathStatus b : DiaAttachmentPathStatus.values()) {
      if (b.value.equals(status)) {
        return b;
      }
    }
    return null;
  }
}
