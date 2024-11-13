package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DdfDocCat {
  COMMUNITY("Community"),

  FORUM("Forum"),

  TOPIC("Topic"),

  TOPIC_IMAGE("Topic Image"),
  TOPIC_RICHTEXTIMAGE("RichTextImage-Topic"),
  TOPIC_ATTACHMENT("Topic Attachment");

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
