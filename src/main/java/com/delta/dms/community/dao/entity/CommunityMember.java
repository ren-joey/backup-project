package com.delta.dms.community.dao.entity;

import java.util.*;

import com.delta.dms.community.adapter.entity.JwtToken;
import com.delta.dms.community.swagger.model.CommunityCategory;
import com.delta.dms.community.swagger.model.GroupUserField;
import com.delta.dms.community.swagger.model.MyDmsGroupData;
import com.delta.dms.community.utils.GroupConstants;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.swagger.model.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.delta.dms.community.utils.GroupConstants.*;
import static com.delta.dms.community.utils.GroupConstants.SYSTEM_GROUP_NAME_KM;

@Data
@EqualsAndHashCode(of = {"userId", "roleId"})
public class CommunityMember {

  private static final String ADMIN_LABEL_TW = "管理員";
  private static final String ADMIN_LABEL_CN = "管理员";
  private static final String ADMIN_LABEL_EN = "Admin";
  private static final String MEMBER_LABEL_TW = "成員";
  private static final String MEMBER_LABEL_CN = "成员";
  private static final String MEMBER_LABEL_EN = "Member";
  private static final String MANAGER_LABEL_PROJECT_TW = "負責人";
  private static final String MANAGER_LABEL_DEPT = "主管";
  private static final String MANAGER_LABEL_PROJECT_CN = "负责人";
  private static final String MANAGER_LABEL_PROJECT_EN = "Owner";
  private static final String MANAGER_LABEL_DEPT_EN = "Manager";

  private static final String KNOWLEDGE_ADMIN_LABEL_TW = "知識管理員";
  private static final String KNOWLEDGE_ADMIN_LABEL_CN = "知识管理员";
  private static final String KNOWLEDGE_ADMIN_LABEL_EN = "Knowledge Admin";

  private static final String KM_KNOWLEDGE_UNIT_LABEL_TW = "技術知識單元 編輯人員";
  private static final String KM_KNOWLEDGE_UNIT_LABEL_CN = "技术知识单元 编辑人员";
  private static final String KM_KNOWLEDGE_UNIT_LABEL_EN = "Technology Knowledge Unit Editor";

  private static final String SUPPLIER_KU_LABEL_TW = "供應商知識單元 編輯人員";
  private static final String SUPPLIER_KU_LABEL_CN = "供应商知识单元 编辑人员";
  private static final String SUPPLIER_KU_LABEL_EN = "Supplier Management Knowledge Unit Editor";

  private static final String KM_LABEL_TW = "領域詞彙 編輯人員";
  private static final String KM_LABEL_CN = "领域词汇 编辑人员";
  private static final String KM_LABEL_EN = "Domain Glossary Editor";

  static final String SRC_STRING_SPLITTER = "．";
  private String userId;
  private String cname;
  private int roleId;
  private UserStatus status;
  private boolean dmsSync;
  private String icon;
  private String department;
  private int priority;
  private boolean lock;
  private UserGroup userGroup;
  private String orgGroupName;
  private List<String> customGroupNames;
  private String mail;
  private String groupNameRawList;
  private int totalCount;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }

  public String getSource(MyDmsGroupData orgGroupItem) {
    if (this.dmsSync && orgGroupItem != null && StringUtils.isNotBlank(this.groupNameRawList)){
      this.orgGroupName = orgGroupItem.getGroupName();
      return String.join(SRC_STRING_SPLITTER, orgGroupName, getRoleName(orgGroupItem));
    } else {
      return null;
    }
  }

  private boolean isCustomGroup() {
    List<String> list = getCustomGroupOnly();
    return StringUtils.isNotBlank(this.groupNameRawList) && ((long) list.size() > 0);
  }

  public String getCustomUserGroupName() {
    List<String> list = getCustomGroupOnly();
    return list.stream().findFirst().orElse("");
  }

  private List<String> getCustomGroupOnly() {
    List<String> list = new ArrayList<>(Arrays.asList(this.groupNameRawList.split("\\s*;\\s*")));
    list.removeIf(s -> s.startsWith("_Member"));
    list.removeIf(s -> s.startsWith("_Admin"));
    if (list.stream().anyMatch(Arrays.asList(SYSTEM_GROUP_NAME_ADMIN,
            SYSTEM_GROUP_NAME_ALLMEMBERS, SYSTEM_GROUP_NAME_MANAGER, SYSTEM_GROUP_NAME_FAMILY,
            SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN,SYSTEM_GROUP_NAME_SUPPLIER_KU, SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT,
            SYSTEM_GROUP_NAME_KM)::contains)) {
      return new ArrayList<>();
    } else {
      return list;
    }
  }

  private int determinePriority() {
    int priority = -1;
    List<String> list = Arrays.asList(this.groupNameRawList.split("\\s*;\\s*"));
    if(list.isEmpty()) {
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_MANAGER)) {
      priority = 1;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_ADMIN)) {
      priority = 2;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_ALLMEMBERS)) {
      priority = 3;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN)) {
      priority = 4;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT)) {
      priority = 5;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_SUPPLIER_KU)) {
      priority = 6;
    } else if(list.contains(GroupConstants.SYSTEM_GROUP_NAME_KM)) {
      priority = 7;
    }
    return priority;
  }

  public String getRoleName(MyDmsGroupData orgGroupItem) {
    this.priority = determinePriority();
    boolean isProjectGroup = CommunityCategory.PROJECT.toString().equals(orgGroupItem.getGroupType());
    final String locale = AcceptLanguage.get();
    switch (locale) {
      case "en-us":
        return getRoleEn(this.priority, isProjectGroup);
      case "zh-cn":
        return getRoleCn(this.priority, isProjectGroup);
      default:
        return getRoleTw(this.priority, isProjectGroup);
    }
  }

  private String getRoleTw(int priority, boolean isProjectGroup) {
    switch (priority) {
      case 1: // manager
        return isProjectGroup ? MANAGER_LABEL_PROJECT_TW : MANAGER_LABEL_DEPT;
      case 2: // admin
        return ADMIN_LABEL_TW;
      case 3: // MEMBER
        return MEMBER_LABEL_TW;
      case 4:
        return KNOWLEDGE_ADMIN_LABEL_TW;
      case 5:
        return KM_KNOWLEDGE_UNIT_LABEL_TW;
      case 6:
        return SUPPLIER_KU_LABEL_TW;
      case 7:
        return KM_LABEL_TW;
      default:
        if (isCustomGroup()) {
          return getCustomUserGroupName();
        } else {
          return MEMBER_LABEL_TW;
        }
    }
  }

  private String getRoleCn(int priority, boolean isProjectGroup) {
    switch (priority) {
      case 1: // manager
        return isProjectGroup ? MANAGER_LABEL_PROJECT_CN : MANAGER_LABEL_DEPT;
      case 2: // admin
        return ADMIN_LABEL_CN;
      case 3: // MEMBER
        return MEMBER_LABEL_CN;
      case 4:
        return KNOWLEDGE_ADMIN_LABEL_CN;
      case 5:
        return KM_KNOWLEDGE_UNIT_LABEL_CN;
      case 6:
        return SUPPLIER_KU_LABEL_CN;
      case 7:
        return KM_LABEL_CN;
      default:
        if (isCustomGroup()) {
          return getCustomUserGroupName();
        } else {
          return MEMBER_LABEL_CN;
        }
    }
  }

  private String getRoleEn(int priority, boolean isProjectGroup) {
    switch (priority) {
      case 1: // manager
        return isProjectGroup ? MANAGER_LABEL_PROJECT_EN : MANAGER_LABEL_DEPT_EN;
      case 2: // admin
        return ADMIN_LABEL_EN;
      case 3: // MEMBER
        return MEMBER_LABEL_EN;
      case 4:
        return KNOWLEDGE_ADMIN_LABEL_EN;
      case 5:
        return KM_KNOWLEDGE_UNIT_LABEL_EN;
      case 6:
        return SUPPLIER_KU_LABEL_EN;
      case 7:
        return KM_LABEL_EN;
      default:
        if (isCustomGroup()) {
          return getCustomUserGroupName();
        } else {
          return MEMBER_LABEL_EN;
        }
    }
  }
}
