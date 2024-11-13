package com.delta.dms.community.service.view.converter;

import static com.delta.dms.community.utils.Constants.COMMUNITY_ROLE_PRIORITY_MANAGER;
import org.springframework.stereotype.Component;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;

@Component
public class ManagerGroupMemberConverter extends BaseGroupMemberConverter
    implements GroupMemberConverter {

  @Override
  public ManagerConverterType type() {
    return ManagerConverterType.__Manager;
  }

  @Override
  int roleId() {
    return Role.COMMUNITY_ADMIN.getId();
  }

  @Override
  protected int getPriority() {
    return COMMUNITY_ROLE_PRIORITY_MANAGER;
  }

  @Override
  protected boolean getLock() {
    return true;
  }
}
