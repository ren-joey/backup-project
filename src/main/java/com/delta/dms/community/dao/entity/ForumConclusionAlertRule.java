package com.delta.dms.community.dao.entity;

import java.util.ArrayList;
import java.util.List;
import com.delta.dms.community.enums.ConclusionAlertRuleType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForumConclusionAlertRule {
  private int ruleId;
  private int forumId;
  private int startDay;
  private int endDay;
  private int factoryId;
  private String factoryName;
  private ConclusionAlertRuleType ruleType;
  private List<ForumConclusionAlertMember> members = new ArrayList<>();
}
