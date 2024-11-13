package com.delta.dms.community.dao.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForumConclusionAlert {
  private List<ForumConclusionAlertGroup> groups = new ArrayList<>();
  private List<ForumConclusionAlertRule> rules = new ArrayList<>();
  private long groupLastModifiedTime = 0;
  private long ruleLastModifiedTime = 0;
}
