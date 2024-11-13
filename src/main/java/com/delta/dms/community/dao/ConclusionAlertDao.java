package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.EerpRangeDayEntity;
import com.delta.dms.community.dao.entity.ForumConclusionAlert;
import com.delta.dms.community.dao.entity.ForumConclusionAlertGroup;
import com.delta.dms.community.dao.entity.ForumConclusionAlertMember;
import com.delta.dms.community.dao.entity.ForumConclusionAlertRule;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.RuleColumnEntity;
import com.delta.dms.community.enums.ConclusionAlertRuleType;

public interface ConclusionAlertDao {

  public ForumConclusionAlert getConclusionAlert(@Param("forumId") int forumId);

  public List<ForumConclusionAlertGroup> getConclusionAlertGroup(@Param("forumId") int forumId);

  public List<ForumConclusionAlertRule> getConclusionAlertRule(@Param("forumId") int forumId);

  public Integer upsertConclusionAlertGroupModifiedTime(
      @Param("forumId") int forumId, @Param("userId") String userId, @Param("time") long time);

  public Integer upsertConclusionAlertRuleModifiedTime(
      @Param("forumId") int forumId, @Param("userId") String userId, @Param("time") long time);

  public Integer insertConclusionAlertGroup(@Param("group") List<ForumConclusionAlertGroup> group);

  public Integer insertConclusionAlertGroupMember(
      @Param("group") List<ForumConclusionAlertGroup> group);

  public Integer updateConclusionAlertGroup(@Param("group") List<ForumConclusionAlertGroup> group);

  public Integer deleteConclusionAlertGroup(@Param("groupId") List<Integer> groupId);

  public Integer deleteConclusionAlertGroupMember(@Param("groupId") List<Integer> groupId);

  public Integer insertConclusionAlertRule(@Param("rule") List<ForumConclusionAlertRule> rule);

  public Integer insertConclusionAlertRuleMember(
      @Param("rule") List<ForumConclusionAlertRule> rule);

  public Integer updateConclusionAlertRule(@Param("rule") List<ForumConclusionAlertRule> rule);

  public Integer deleteConclusionAlertRule(@Param("ruleId") List<Integer> ruleId);

  public Integer deleteConclusionAlertRuleMember(@Param("ruleId") List<Integer> ruleId);

  public Integer deleteConclusionAlertRuleGroupMember(@Param("memberId") List<Integer> memberId);

  public List<ForumConclusionAlertMember> searchGroupByName(
      @Param("forumId") int forumId, @Param("q") String q, @Param("limit") int limit);

  public List<ForumConclusionAlertRule> getAllConclusionAlertRuleWthFlatMember();

  public List<ForumConclusionAlertRule> getConclusionAlertRuleWthFlatMember(
      @Param("forumId") int forumId,
      @Param("ruleType") ConclusionAlertRuleType ruleType,
      @Param("day") int day);

  public List<RuleColumnEntity> getRuleColumnByCommunityId(@Param("communityId") int communityId);

  public List<IdNameEntity> getDropdownByColumnId(@Param("columnId") int columnId);

  public EerpRangeDayEntity getRangeDayByColumnId(@Param("columnId") int columnId);
}
