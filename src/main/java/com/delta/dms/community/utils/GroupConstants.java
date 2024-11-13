package com.delta.dms.community.utils;

public class GroupConstants {

  public static final String USERGROUP_MEMBERID = "memberId";
  public static final String USERGROUP_MEMBERNAME = "memberName";
  public static final String USERGROUP_DEPGROUP = "DeptGroup";
  public static final String USERGROUP_PROJGROUP = "ProjectGroup";
  public static final String USERGROUP_CHILDREN = "children";
  public static final String USERGROUP_TYPE = "type";
  public static final String USERGROUP_DESCRIPTION = "description";
  public static final String USERGROUP_FUN_USERGROUPIDS = "userGroupIds";
  public static final String GIDS = "gids";
  public static final String ID = "id";
  public static final String USERGROUP_NAME = "name";
  public static final String USERGROUP_PARENT_ID = "parentId";
  public static final String USERGROUP_MEMBERS = "members";
  public static final String USERGROUP_CUS_FIELDS = "customizedFields";
  public static final String USERGROUP_GROUP_LIST = "groupList";
  public static final String USERGROUP_UIDS = "uids";
  public static final String USERGROUP_NAME_ADMIN = "_Admin";
  public static final String USERGROUP_NAME_MEMBER = "_Member";
  public static final String USERGROUP_TYPE_COMMUNITY = "|C|";
  public static final String USERGROUP_TYPE_FORUM = "|F|";

  public static final String SYSTEM_GROUP_NAME_ADMIN = "__Admin";
  public static final String SYSTEM_GROUP_NAME_MANAGER = "__Manager";
  public static final String SYSTEM_GROUP_NAME_ALLMEMBERS = "__AllMembers";
  public static final String SYSTEM_GROUP_NAME_FAMILY = "__FamilyMembers";

  public static final String SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN = "__KnowledgeAdmin";
  public static final String SYSTEM_GROUP_NAME_SUPPLIER_KU = "__SupplierKU";
  public static final String SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT = "__KmKnowledgeUnit";
  public static final String SYSTEM_GROUP_NAME_KM = "__Km";
  public static final String SYSTEM_GROUP_NAME_CUSTOM = "CUSTOM";

  public static final String NOT_FAMILY_MEMBER_FILTER_REGEX = "^(?!__FamilyMembers).*";
  public static final String CUSTOM_GROUP_FILTER = "^(?!__).*";
  public static final String ADMIN_MANAGER_ALLMEMBER_FILTER_REGEX =
      "^("
          + SYSTEM_GROUP_NAME_ADMIN
          + "|"
          + SYSTEM_GROUP_NAME_MANAGER
          + "|"
          + SYSTEM_GROUP_NAME_ALLMEMBERS
          + ").*";

  private GroupConstants() {}
}
