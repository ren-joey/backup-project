package com.delta.dms.community.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Role {
  COMMUNITY_ADMIN(1),

  COMMUNITY_MEMBER(2),

  FORUM_ADMIN(3),

  FORUM_MEMBER(4);

  private int id;

  @Override
  public String toString() {
    return String.valueOf(id);
  }

  public int getId() {
    return id;
  }

  public static Role getRoleByValue(final int value) {
    for (Role oprname : Role.values()) {
      if (value == (oprname.getId())) {
        return oprname;
      }
    }
    return null;
  }
}
