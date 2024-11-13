package com.delta.dms.community.service.view;

import static com.delta.dms.community.service.view.CommunityMemberTableViewRender.TableViewRenderType.INDEPENDENT;

import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.swagger.model.MembersTableViewResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndependentCommunityMemberTableViewRender extends CommunityMemberTableViewRenderSupport implements CommunityMemberTableViewRender {

  @Override
  public TableViewRenderType renderType() {
    return INDEPENDENT;
  }

  @Override
  public MembersTableViewResult renderView(CommunityMemberTableViewRequest request) {
    return getMemberViewDetailList(request);
  }

  /**
   * if sort by role then admin user should be first, then sort by name
   *
   * @param sort role/name
   * @return sort criteria
   */
  private String buildSortCriteria(String sort) {
    return "role".equals(sort) ? "role_id asc, cname asc" : "cname asc";
  }
}
