package com.delta.dms.community.adapter.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class OrgGroup {
  private String id;
  private String name;
  private String ename;
  private OrgGroupType type;
  private String description;
  private String parentId;
  private String parentName;
  private OrgGroupType parentType;
  private String code;
  private List<InnerUserGroup> children = new ArrayList<>();
  private List<String> customizedFields = new ArrayList<>();

  public enum OrgGroupType {
    DeptGroup,
    ProjectGroup,
    UserGroup;

    public String domainString() {
      switch (this) {
        case DeptGroup:
          return "Department";
        case ProjectGroup:
          return "Project";
        case UserGroup:
          return "UserGroup";
        default:
          return "Unknown";
      }
    }
  }

  @Getter
  @Setter
  public static class InnerUserGroup {
    private String id;
    private String name;
    private OrgGroupType type;
  }
}
