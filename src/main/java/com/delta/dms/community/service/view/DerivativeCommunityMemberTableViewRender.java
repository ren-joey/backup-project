package com.delta.dms.community.service.view;

import static com.delta.dms.community.service.view.CommunityMemberTableViewRender.TableViewRenderType.DERIVATIVE;
import static java.util.Comparator.comparing;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.swagger.model.MembersTableViewResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DerivativeCommunityMemberTableViewRender extends CommunityMemberTableViewRenderSupport
    implements CommunityMemberTableViewRender {

  public static final String SORT_ROLE = "role";

  @Autowired UserGroupAdapter userGroupAdapter;

  @Autowired CommunityService communityService;

  @Override
  public TableViewRenderType renderType() {
    return DERIVATIVE;
  }

  @Override
  public MembersTableViewResult renderView(CommunityMemberTableViewRequest request) {
    return getMemberViewDetailList(request);
  }

  private void filterByName(
      CommunityMemberTableViewRequest request, List<CommunityMember> totalMembers) {
    if (!StringUtils.isEmpty(request.getName())) {
      totalMembers.removeIf(m -> !StringUtils.containsIgnoreCase(m.getCname(), request.getName()));
    }
  }

  private List<CommunityMember> sortByCriteria(
      CommunityMemberTableViewRequest request, List<CommunityMember> communityMembers) {
    if (request.getSort().equals(SORT_ROLE)) {
      final Comparator<CommunityMember> roleSort =
          comparing(CommunityMember::getPriority).thenComparing(CommunityMember::getCname);
      communityMembers = communityMembers.stream().sorted(roleSort).collect(Collectors.toList());
    } else {
      communityMembers =
          communityMembers
              .stream()
              .sorted(comparing(CommunityMember::getCname))
              .collect(Collectors.toList());
    }
    return communityMembers;
  }

  private List<CommunityMember> pagingByCriteria(
      CommunityMemberTableViewRequest request, List<CommunityMember> totalMembers) {
    return totalMembers
        .stream()
        .skip((long) (request.getPage() - 1) * request.getPageSize())
        .limit(request.getPageSize())
        .collect(Collectors.toList());
  }
}
