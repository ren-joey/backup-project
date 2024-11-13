package com.delta.dms.community.dao.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RoleDetailEntity {
  private String userId;
  private String groupId;
  private int forumId;
  private int scopeId;
  private int roleId;
}
