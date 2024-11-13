package com.delta.dms.community.service.view.converter;

import com.delta.dms.community.enums.Role;
import org.springframework.stereotype.Component;

import static com.delta.dms.community.utils.Constants.*;

public abstract class AbstractKmGroupMemberConverter extends BaseGroupMemberConverter
    implements GroupMemberConverter {

  @Override
  public abstract GroupMemberConverterFactory.ManagerConverterType type();

  @Override
  int roleId() {
    return Role.COMMUNITY_MEMBER.getId();
  }

  @Override
  protected boolean getLock() {
    return true;
  }

  @Component
  static class KnowledgeAdminGroupMemberConverter extends AbstractKmGroupMemberConverter {
    @Override
    public GroupMemberConverterFactory.ManagerConverterType type() {
      return GroupMemberConverterFactory.ManagerConverterType.__KnowledgeAdmin;
    }

    @Override
    protected int getPriority() {
      return COMMUNITY_ROLE_PRIORITY_KNOWLEDGE_ADMIN;
    }
  }

  @Component
  static class KmKnowledgeUnitGroupMemberConverter extends AbstractKmGroupMemberConverter {
    @Override
    public GroupMemberConverterFactory.ManagerConverterType type() {
      return GroupMemberConverterFactory.ManagerConverterType.__KmKnowledgeUnit;
    }

    @Override
    protected int getPriority() {
      return COMMUNITY_ROLE_PRIORITY_KM_KNOWLEDGE_UNIT;
    }
  }

  @Component
  static class KmGroupMemberConverter extends AbstractKmGroupMemberConverter {
    @Override
    public GroupMemberConverterFactory.ManagerConverterType type() {
      return GroupMemberConverterFactory.ManagerConverterType.__Km;
    }

    @Override
    protected int getPriority() {
      return COMMUNITY_ROLE_PRIORITY_KM;
    }
  }

  @Component
  static class SupplierKuMemberConverter extends AbstractKmGroupMemberConverter {
    @Override
    public GroupMemberConverterFactory.ManagerConverterType type() {
      return GroupMemberConverterFactory.ManagerConverterType.__SupplierKU;
    }

    @Override
    protected int getPriority() {
      return COMMUNITY_ROLE_PRIORITY_SUPPLIER_KU;
    }
  }
}
