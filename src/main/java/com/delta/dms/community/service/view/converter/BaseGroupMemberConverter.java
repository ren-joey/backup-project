package com.delta.dms.community.service.view.converter;

import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.service.member.CollectMemberContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseGroupMemberConverter implements GroupMemberConverter {

    abstract int roleId();

    protected abstract int getPriority();

    protected abstract boolean getLock();

    public List<CommunityMember> convert(UserGroup userGroup, CollectMemberContext context) {
        return Arrays.stream(userGroup.getMembers())
                .map(member -> {
                            CommunityMember communityMember = new CommunityMember();
                            communityMember.setOrgGroupName(context.getOrgGroupName());
                            communityMember.setUserId(member.getMemberId());
                            communityMember.setCname(member.getMemberName());
                            communityMember.setDmsSync(isSync());
                            communityMember.setLock(getLock());
                            communityMember.setRoleId(roleId());
                            communityMember.setPriority(getPriority());
                            communityMember.setUserGroup(userGroup);
                            return communityMember;
                        }
                ).collect(Collectors.toList());
    }
}
