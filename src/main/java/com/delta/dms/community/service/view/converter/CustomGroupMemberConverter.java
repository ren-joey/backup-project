package com.delta.dms.community.service.view.converter;

import static com.delta.dms.community.utils.Constants.COMMUNITY_ROLE_PRIORITY_CUSTOM;
import org.springframework.stereotype.Component;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;

@Component
public class CustomGroupMemberConverter extends BaseGroupMemberConverter
    implements GroupMemberConverter {

  @Override
  public ManagerConverterType type() {
    return ManagerConverterType.DEFAULT;
  }

  @Override
  int roleId() {
    return Role.COMMUNITY_MEMBER.getId();
  }

  @Override
  protected int getPriority() {
    return COMMUNITY_ROLE_PRIORITY_CUSTOM;
  }

  @Override
  protected boolean getLock() {
    return true;
  }
}
