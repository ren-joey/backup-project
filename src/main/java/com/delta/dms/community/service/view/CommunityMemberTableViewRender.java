package com.delta.dms.community.service.view;

import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.swagger.model.MembersTableViewResult;

public interface CommunityMemberTableViewRender {

  /**
   * render type
   * @return
   */
  TableViewRenderType renderType();

  MembersTableViewResult renderView(CommunityMemberTableViewRequest request);

  enum TableViewRenderType{
    DERIVATIVE, INDEPENDENT
  }

}
