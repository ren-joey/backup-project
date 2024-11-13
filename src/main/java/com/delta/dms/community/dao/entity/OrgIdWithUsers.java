package com.delta.dms.community.dao.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgIdWithUsers {
  private String orgId;
  private String users;

  @Override
  public String toString() {
    return "OrgIdWithUsers [orgId=" + orgId + ", users=" + users + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((orgId == null) ? 0 : orgId.hashCode());
    result = prime * result + ((users == null) ? 0 : users.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    OrgIdWithUsers other = (OrgIdWithUsers) obj;
    if (orgId == null) {
      if (other.orgId != null) return false;
    } else if (!orgId.equals(other.orgId)) return false;
    if (users == null) {
      if (other.users != null) return false;
    } else if (!users.equals(other.users)) return false;
    return true;
  }
}
