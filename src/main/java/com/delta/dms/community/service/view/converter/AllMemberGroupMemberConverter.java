package com.delta.dms.community.service.view.converter;

import static com.delta.dms.community.utils.Constants.COMMUNITY_ROLE_PRIORITY_MEMBER;
import org.springframework.stereotype.Component;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;

@Component
public class AllMemberGroupMemberConverter extends BaseGroupMemberConverter
    implements GroupMemberConverter {

  @Override
  int roleId() {
    return Role.COMMUNITY_MEMBER.getId();
  }

  @Override
  protected int getPriority() {
    return COMMUNITY_ROLE_PRIORITY_MEMBER;
  }

  @Override
  protected boolean getLock() {
    return true;
  }

  @Override
  public ManagerConverterType type() {
    return ManagerConverterType.__AllMembers;
  }
}
