package com.delta.dms.community.service.view.converter;

import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.service.member.CollectMemberContext;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;

import java.util.List;

public interface GroupMemberConverter {

  ManagerConverterType type();

  List<CommunityMember> convert(UserGroup userGroup, CollectMemberContext context);

  default boolean isSync() {
    return true;
  }
}
