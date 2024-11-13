package com.delta.dms.community.service.view.converter;

import static com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType.__Admin;
import static com.delta.dms.community.utils.Constants.COMMUNITY_ROLE_PRIORITY_ADMIN;
import org.springframework.stereotype.Component;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;

@Component
public class AdminGroupMemberConverter extends BaseGroupMemberConverter
    implements GroupMemberConverter {

  @Override
  public ManagerConverterType type() {
    return __Admin;
  }

  @Override
  int roleId() {
    return Role.COMMUNITY_ADMIN.getId();
  }

  @Override
  protected int getPriority() {
    return COMMUNITY_ROLE_PRIORITY_ADMIN;
  }

  @Override
  protected boolean getLock() {
    return true;
  }
}
