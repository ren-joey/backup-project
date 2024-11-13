package com.delta.dms.community.controller.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
@Builder
public class CommunityMemberTableViewRequest {

  private int communityId;
  private int page;
  private int pageSize;
  String sort;
  String name;
  String communityGroupId;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
