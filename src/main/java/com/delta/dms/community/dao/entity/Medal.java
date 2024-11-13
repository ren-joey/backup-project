package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class Medal {
  private String targetId;
  private boolean selected;
  private int id;
  private String name;
  private boolean disabled;
  private Long expireTime;
  private String frame;
  private String title;
  private String certification;
  private int certificationOrder;
}
