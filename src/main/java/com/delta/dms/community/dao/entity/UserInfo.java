package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data
public class UserInfo {
  private String userId = "";
  private String account = "";
  private String cname = "";
  private String mail = "";
  private int status = 0;
  private int batchId = 0;
  private String type = "";
}
