package com.delta.dms.community.adapter.entity;

import lombok.Data;

@Data
public class SourceGroup {
  private String id;
  private String name;
  private String parentId;
  private String parentType;
  private String parentName;
}
