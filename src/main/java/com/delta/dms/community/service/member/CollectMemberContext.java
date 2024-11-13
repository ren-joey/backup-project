package com.delta.dms.community.service.member;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.model.AcceptLanguage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

@Slf4j
@Data
public class CollectMemberContext {

  private List<CommunityMember> generalMembers = new ArrayList<>();
  private List<CommunityMember> customGroupMembers = new ArrayList<>();
  private List<CommunityMember> aggregateGroupMembers = new ArrayList<>();
  private String communityGroupId;
  private String orgGroupName;
  private List<UserGroup> userGroups;
  private Map<String, List<String>> customGroupNameMap = new HashMap<>();

  public CollectMemberContext(String communityGroupId) {
    this.communityGroupId = communityGroupId;
  }

  public CollectMemberContext() {
  }

  public static CollectMemberContext newContext() {
    return new CollectMemberContext();
  }

  public static CollectMemberContext of(String communityGroupId) {
    return new CollectMemberContext(communityGroupId);
  }

  public void setupOrgGroupName(OrgGroup orgGroup) {
    String locale = AcceptLanguage.get();
    this.orgGroupName = locale.contains("en") ? StringUtils.defaultString(orgGroup.getEname(), orgGroup.getName()) : orgGroup.getName();
  }

  public void distinctMember() {
    this.aggregateGroupMembers = new ArrayList<>(distinctUser(aggregateGroupMembers).values());
  }

  private Map<String, CommunityMember> distinctUser(List<CommunityMember> communityMembers) {
    return communityMembers.stream()
        .collect(toMap(CommunityMember::getUserId, Function.identity(), BinaryOperator.minBy(comparing(CommunityMember::getPriority))));
  }

  public void populateCustomGroupNames() {
    Map<String, Set<String>> collect = customGroupMembers.stream()
        .filter(communityMember -> communityMember.getUserGroup().isCustomGroup())
        .collect(groupingBy(CommunityMember::getUserId, mapping(CommunityMember::getCustomUserGroupName, toSet())));
    collect.forEach((k, v) -> {if (v.size() > 1) {customGroupNameMap.put(k, new ArrayList<>(v));}});
    for (CommunityMember customGroupMember : this.customGroupMembers) {
      if (customGroupNameMap.get(customGroupMember.getUserId()) != null) {
        customGroupMember.setCustomGroupNames(customGroupNameMap.get(customGroupMember.getUserId()));
      }
    }
  }

  public void joinMembers() {
    aggregateGroupMembers.addAll(generalMembers);
    aggregateGroupMembers.addAll(customGroupMembers);
  }

}
