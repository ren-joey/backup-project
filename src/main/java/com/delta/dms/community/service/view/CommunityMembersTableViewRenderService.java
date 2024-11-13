package com.delta.dms.community.service.view;

import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.swagger.model.MembersTableViewResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
@Slf4j
@RequiredArgsConstructor
@Service
public class CommunityMembersTableViewRenderService {

  private final CommunityService communityService;
  private final CommunityMemberTableViewRenderFactory tableViewRenderFactory;
  public MembersTableViewResult findByCriteria(CommunityMemberTableViewRequest request){
    String groupId = communityService.getCommunityInfoById(request.getCommunityId()).getCommunityGroupId();
    Optional.ofNullable(groupId).ifPresent(request::setCommunityGroupId);
    boolean isDerivativeCommunity = StringUtils.isNotBlank(groupId);
    CommunityMemberTableViewRender render = tableViewRenderFactory.getRender(isDerivativeCommunity);
    return render.renderView(request);
  }
}
