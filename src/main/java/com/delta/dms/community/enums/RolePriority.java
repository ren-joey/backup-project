package com.delta.dms.community.enums;

public enum RolePriority {
  MANAGER(1, "__Manager", 1),
  ADMIN(2, "__Admin", 1),
  MEMBER(3, "__AllMembers", 3),
  KNOWLEDGE_ADMIN(4, "__KnowledgeAdmin", 3),
  KM_KNOWLEDGE_UNIT(5, "__KmKnowledgeUnit", 3),
  SUPPLIER_KU(6, "__SupplierKU", 3),
  KM(7, "__Km", 3),
  CUSTOM(8, "Custom", 3);
  int priority;
  String groupName;
  int role;

  RolePriority(int priority, String groupName, int role) {
    this.priority = priority;
    this.groupName = groupName;
    this.role = role;
  }

  public static RolePriority from(String groupName) {
    for (RolePriority rolePriority : RolePriority.values()) {
      if (rolePriority.groupName.equals(groupName)) {
        return rolePriority;
      }
    }
    return CUSTOM;
  }

  public int priority() {
    return priority;
  }
}
