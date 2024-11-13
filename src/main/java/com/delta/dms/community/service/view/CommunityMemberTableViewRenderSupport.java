package com.delta.dms.community.service.view;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.swagger.model.*;
import com.delta.dms.community.utils.Utility;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.delta.dms.community.dao.CommunityDao;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.service.CommunityService;

public abstract class CommunityMemberTableViewRenderSupport {

  @Autowired protected CommunityDao communityDao;
  @Autowired protected UserService userService;
  @Autowired protected CommunityService communityService;

  static MembersTableViewResult EMPTY_RESULT =
      new MembersTableViewResult().result(Collections.emptyList()).numFound(0L);

  /**
   * get internal talent map by user ids
   *
   * @param userIds ids
   * @return map of internal talent
   */
  public Map<String, UserSession> getInternalTalentMap(List<String> userIds) {
    return userService
        .getUserById(userIds, Lists.newArrayList())
        .stream()
        .collect(Collectors.toMap(UserSession::getCommonUUID, Function.identity()));
  }

    protected MembersTableViewResult getMemberViewDetailList(CommunityMemberTableViewRequest request) {
        int offset = (request.getPage() - 1) * request.getPageSize();
        List<CommunityMember> totalCount = (communityDao.getMemberListWithSortAndLimit(true,
                request.getCommunityId(), null, -1, request.getName(),
                null, offset, request.getPageSize(), request.getSort()));
        Optional<CommunityMember> optional = totalCount.stream().findFirst();
        List<CommunityMember> communityMemberList = communityDao.getMemberListWithSortAndLimit(false,
                request.getCommunityId(), null, -1, request.getName(),
                null, offset, request.getPageSize(), request.getSort());

        if (!communityMemberList.isEmpty() && optional.isPresent()) {
            long total = optional.get().getTotalCount();
            return new MembersTableViewResult()
                    .result(convertViewDetail(request.getCommunityId(), communityMemberList))
                    .numFound(total);
        }
        return EMPTY_RESULT;
    }

  private boolean isRoleAdmin(int roleId) {
    return roleId == Role.COMMUNITY_ADMIN.getId();
  }

  protected List<MembersTableViewDetail> convertViewDetail(int communityId, List<CommunityMember> members) {
      CommunityInfo communityInfo = communityService.getCommunityInfoById(communityId);
      MyDmsGroupData orgGroupItem = communityService.getOrgGroupItem(communityInfo.getCommunityGroupId());
      final Map<String, UserSession> internalTalentMap =
        getInternalTalentMap(
            members.stream().map(CommunityMember::getUserId).collect(Collectors.toList()));

      return members
        .stream()
        .map(
            member -> {
              final UserSession internalTalentUser =
                  internalTalentMap.get(member.getUserId());
              MembersTableViewDetail detail = new MembersTableViewDetail();
              detail.id(member.getUserId());
              detail.setName(member.getCname());
              detail.setDepartment(
                  Objects.nonNull(internalTalentUser)
                      ? internalTalentUser.getProfileDeptName()
                      : "");
              detail.setStatus(
                  Objects.nonNull(internalTalentUser)
                      ? UserStatus.fromValue(internalTalentUser.getStatus())
                      : UserStatus.INACTIVE);
              detail.setImgAvatar(
                  Objects.nonNull(internalTalentUser) ? internalTalentUser.getCommonImage() : "");
              detail.setIsAdmin(isRoleAdmin(member.getRoleId()));
              detail.setSrc(member.getSource(orgGroupItem));
              detail.setCustomGroupNames(member.getCustomGroupNames());
              detail.setIsFromMyDMS(member.isDmsSync());
              return detail;
            })
        .collect(Collectors.toList());
  }
}
