package com.delta.dms.community.dao.entity;

import com.delta.dms.community.swagger.model.ConclusionAlertMemberType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForumConclusionAlertMember {
  private String memberId;
  private String memberName;
  private ConclusionAlertMemberType memberType;
}
