package com.delta.dms.community.dao.entity;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForumConclusionAlertGroup {
  private int groupId;
  private int forumId;
  private String groupName;
  private List<ForumConclusionAlertMember> members;
}
