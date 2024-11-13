package com.delta.dms.community.service.member;

import static org.apache.commons.lang.StringUtils.EMPTY;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.delta.dms.community.enums.RolePriority;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.adapter.entity.OrgGroup.InnerUserGroup;
import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.dao.CommunityDao;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.service.view.converter.GroupMemberConverter;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory;
import com.delta.dms.community.service.view.converter.GroupMemberConverterFactory.ManagerConverterType;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.swagger.model.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class CollectMemberService {

  private final UserGroupAdapter userGroupAdapter;
  private final GroupMemberConverterFactory memberConverterFactory;
  private final CommunityDao communityDao;
  private final UserService userService;

  public List<CommunityMember> getCommunityMembers(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int role, int limit) {
    List<CommunityMember> communityMemberList = communityDao.getMemberListWithSortAndLimit(
            false, communityId, toGetDmsMember, role, EMPTY,
            userIdList, 0, limit, EMPTY);
    /*
    if(includeDmsMembers) {
      context = getDmsMember(communityGroupId);
    }
    List<CommunityMember> manualCommunityMembers = communityDao.findAllMembers(communityId, EMPTY);
     */
    CollectMemberContext context = CollectMemberContext.newContext();
    context
        .getAggregateGroupMembers()
        .addAll(convertManualMembers(communityMemberList, context));
    context.distinctMember();
    populateCnameIfNotExist(context.getAggregateGroupMembers());
    populateMailIfNotExist(context.getAggregateGroupMembers());
    populateStatus(context.getAggregateGroupMembers());
    return context.getAggregateGroupMembers();
  }

  public Integer getCommunityMemberCount(
          int communityId, Boolean toGetDmsMember, List<String> userIdList, int role) {
    List<CommunityMember> totalCount = (communityDao.getMemberListWithSortAndLimit(
            true, communityId, toGetDmsMember, role, EMPTY,
            userIdList, 0, -1, EMPTY));
    Optional<CommunityMember> optional = totalCount.stream().findFirst();

    int total = 0;
    if(optional.isPresent()) {
      total = optional.get().getTotalCount();
    }

    return total;
  }

  public List<User> getGeneralMembersByOrgId(String orgId) {
    CollectMemberContext context = getDmsMember(orgId);
    populateCnameIfNotExist(context.getGeneralMembers());
    populateStatus(context.getGeneralMembers());
    return context
        .getGeneralMembers()
        .stream()
        .map(d -> new User().id(d.getUserId()).name(d.getCname()).status(d.getStatus()))
        .sorted(Comparator.comparing(User::getName))
        .collect(Collectors.toList());
  }

  public CollectMemberContext getDmsMember(String communityGroupId) {
    if (StringUtils.isBlank(communityGroupId)) {
      return CollectMemberContext.newContext();
    }
    log.debug("get dms member by group id {}", communityGroupId);
    OrgGroup orgGroup = userGroupAdapter.getOrgGroup(communityGroupId);
    if (Objects.isNull(orgGroup)) {
      log.debug("org group {} not exist, org group maybe deleted. ", communityGroupId);
      return CollectMemberContext.newContext();
    }
    log.debug("org group {}, {}", orgGroup.getId(), orgGroup.getName());
    final CollectMemberContext context = CollectMemberContext.of(orgGroup.getCode());
    List<InnerUserGroup> userGroupsOfCurrentOrgGroup = orgGroup.getChildren();
    if (userGroupsOfCurrentOrgGroup.isEmpty()) {
      log.debug("user groups of group {} is empty ", communityGroupId);
      return context;
    }

    List<String> userGroupIds =
        userGroupsOfCurrentOrgGroup
            .stream()
            .map(InnerUserGroup::getId)
            .collect(Collectors.toList());
    log.debug("user groups {}", userGroupIds);
    List<UserGroup> userGroups = userGroupAdapter.getUserGroupDetail(userGroupIds);
    context.setUserGroups(userGroups);
    context.setupOrgGroupName(orgGroup);
    collectDmsGroupMember(context);
    return context;
  }

  private void collectDmsGroupMember(CollectMemberContext context) {
    for (UserGroup userGroup : context.getUserGroups()) {
      if (userGroup.isGeneralGroup()) {
        log.debug("general user group [{}] ", userGroup.getName());
        List<CommunityMember> members =
            memberConverterFactory.getConverter(userGroup.getName()).convert(userGroup, context);
        context.getGeneralMembers().addAll(members);
      }

      if (userGroup.isCustomGroup()) {
        log.debug("custom user group [{}] ", userGroup.getName());
        GroupMemberConverter customGroupConverter =
            memberConverterFactory.getConverter(ManagerConverterType.DEFAULT.name());
        List<CommunityMember> customGroupMember1 = customGroupConverter.convert(userGroup, context);
        context.getCustomGroupMembers().addAll(customGroupMember1);

        if (userGroup.getGroupList() != null && userGroup.getGroupList().length > 0) {
          log.debug("group list {}", Arrays.toString(userGroup.getGroupList()));
          List<UserGroup> subUserGroups =
              userGroupAdapter.getUserGroupDetail(Arrays.asList(userGroup.getGroupList()));
          subUserGroups.forEach(
              subUserGroup -> subUserGroup.setParentUserGroupName(userGroup.getName()));
          // custom group user will be member
          subUserGroups
              .stream()
              .filter(UserGroup::notFamilyGroup)
              .forEach(
                  ug ->
                      context
                          .getCustomGroupMembers()
                          .addAll(customGroupConverter.convert(ug, context)));
        }
      }
    }
    //context.populateCustomGroupNames();
    context.joinMembers();
    context.distinctMember();
  }

  private List<CommunityMember> convertManualMembers(
      List<CommunityMember> manualSetupMembers, CollectMemberContext context) {
    return manualSetupMembers
        .stream()
        .map(m -> convertDerivativeCommunityMember(context, m))
        .collect(Collectors.toList());
  }

  private CommunityMember convertDerivativeCommunityMember(
      CollectMemberContext context, CommunityMember m) {
    CommunityMember communityMember = new CommunityMember();
    communityMember.setOrgGroupName(context.getOrgGroupName());
    communityMember.setUserId(m.getUserId());
    communityMember.setCname(m.getCname());
    communityMember.setRoleId(m.getRoleId());
    communityMember.setPriority(RolePriority.from(context.getOrgGroupName()).priority());
    communityMember.setDmsSync(
        isExistInSyncMember(m.getUserId(), context.getAggregateGroupMembers()));
    communityMember.setUserGroup(getExistingUserGroups(context, m.getUserId()));
    communityMember.setCustomGroupNames(
        getExistingCustomUserGroupNames(context, communityMember.getUserId()));
    communityMember.setLock(false);
    return communityMember;
  }

  private List<String> getExistingCustomUserGroupNames(
      CollectMemberContext context, String userId) {
    final Optional<CommunityMember> communityMemberOptional = getExistingUserById(context, userId);
    return communityMemberOptional.map(CommunityMember::getCustomGroupNames).orElse(null);
  }

  private UserGroup getExistingUserGroups(CollectMemberContext context, String userId) {
    final Optional<CommunityMember> communityMemberOptional = getExistingUserById(context, userId);
    return communityMemberOptional.map(CommunityMember::getUserGroup).orElse(null);
  }

  private Optional<CommunityMember> getExistingUserById(
      CollectMemberContext context, String userId) {
    final List<CommunityMember> aggregateGroupMembers = context.getAggregateGroupMembers();
    return aggregateGroupMembers
        .stream()
        .filter(communityMember -> communityMember.getUserId().equals(userId))
        .findFirst();
  }

  private boolean isExistInSyncMember(String userId, List<CommunityMember> syncMembers) {
    final Set<String> syncUserIds =
        syncMembers.stream().map(CommunityMember::getUserId).collect(Collectors.toSet());
    return syncUserIds.contains(userId);
  }

  private void populateCnameIfNotExist(List<CommunityMember> communityMembers) {
    final List<String> emptyCnameUserIds =
        communityMembers
            .stream()
            .filter(c -> StringUtils.isBlank(c.getCname()))
            .map(CommunityMember::getUserId)
            .collect(Collectors.toList());
    if (org.springframework.util.CollectionUtils.isEmpty(emptyCnameUserIds)) {
      return;
    }
    final List<User> userByIds = userService.getUserByIds(emptyCnameUserIds);
    for (User userById : userByIds) {
      for (CommunityMember communityMember : communityMembers) {
        if (communityMember.getUserId().equals(userById.getId())) {
          communityMember.setCname(userById.getName());
        }
      }
    }
  }

  private void populateMailIfNotExist(List<CommunityMember> communityMembers) {
    final List<String> emptyMailUserIds =
        communityMembers
            .stream()
            .filter(c -> StringUtils.isBlank(c.getMail()))
            .map(CommunityMember::getUserId)
            .collect(Collectors.toList());
    if (org.springframework.util.CollectionUtils.isEmpty(emptyMailUserIds)) {
      return;
    }
    final List<User> userByIds = userService.getUserByIds(emptyMailUserIds);
    for (User userById : userByIds) {
      for (CommunityMember communityMember : communityMembers) {
        if (communityMember.getUserId().equals(userById.getId())) {
          communityMember.setMail(userById.getMail());
        }
      }
    }
  }

  private void populateStatus(List<CommunityMember> communityMembers) {
    final Map<String, UserStatus> userStatusMap =
        userService
            .getUserByIds(
                communityMembers
                    .stream()
                    .map(CommunityMember::getUserId)
                    .distinct()
                    .collect(Collectors.toList()))
            .parallelStream()
            .collect(Collectors.toMap(User::getId, User::getStatus));
    communityMembers.forEach(
        item ->
            item.setStatus(
                Optional.ofNullable(userStatusMap.get(item.getUserId()))
                    .orElseGet(() -> UserStatus.INACTIVE)));
  }


}
