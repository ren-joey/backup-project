package com.delta.dms.community.adapter.entity;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;


import static com.delta.dms.community.utils.GroupConstants.*;
import static com.delta.dms.community.utils.GroupConstants.SYSTEM_GROUP_NAME_FAMILY;

@Data
@EqualsAndHashCode(of = {"id"})
public class UserGroup {
    private String id;
    private String name;
    private String applicationId;
    private String applicationName;
    private String parentId;
    private String parentType;
    private String parentName;
    private String parentUserGroupName;
    private String description;
    private String[] customizedFields;
    private String[] groupList;
    private Member[] members;

    @Getter
    @Setter
    @ToString
    public static class Member {
        private String memberId;
        private String memberName;
    }

    public boolean isProjectGroup() {
        return getParentType().equals("ProjectGroup");
    }
  public boolean isGeneralGroup() {
        return Sets.newHashSet(SYSTEM_GROUP_NAME_ADMIN, SYSTEM_GROUP_NAME_ALLMEMBERS, SYSTEM_GROUP_NAME_MANAGER,
                SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN,SYSTEM_GROUP_NAME_SUPPLIER_KU, SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT,
                SYSTEM_GROUP_NAME_KM
        ).contains(name);
    }

    public boolean isCustomGroup() {
        return StringUtils.isNotBlank(parentUserGroupName) || !Sets.newHashSet(SYSTEM_GROUP_NAME_ADMIN,
                SYSTEM_GROUP_NAME_ALLMEMBERS, SYSTEM_GROUP_NAME_MANAGER, SYSTEM_GROUP_NAME_FAMILY,
                SYSTEM_GROUP_NAME_KNOWLEDGE_ADMIN,SYSTEM_GROUP_NAME_SUPPLIER_KU, SYSTEM_GROUP_NAME_KM_KNOWLEDGE_UNIT,
                SYSTEM_GROUP_NAME_KM
                ).contains(name);
    }

    public boolean notFamilyGroup() {
        return !StringUtils.equals(SYSTEM_GROUP_NAME_FAMILY, name);
    }
}
