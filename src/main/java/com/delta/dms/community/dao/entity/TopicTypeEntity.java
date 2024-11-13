package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class TopicTypeEntity {
  private int topicTypeId;
  private String topicType;
  private boolean editable;
  private String name;
  private String appFieldDefaultId;
  private String appFieldDefaultName;
  private boolean archiveConclusionAttachment;
}
