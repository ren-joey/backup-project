package com.delta.dms.community.service;

import static com.delta.dms.community.enums.ConclusionAlertRuleType.GENERAL;
import static com.delta.dms.community.enums.ConclusionAlertRuleType.HIGH;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.ConclusionAlertDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.ConclusionAlertTopicInfo;
import com.delta.dms.community.dao.entity.EerpRangeDayEntity;
import com.delta.dms.community.dao.entity.ForumConclusionAlert;
import com.delta.dms.community.dao.entity.ForumConclusionAlertGroup;
import com.delta.dms.community.dao.entity.ForumConclusionAlertMember;
import com.delta.dms.community.dao.entity.ForumConclusionAlertRule;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.RuleColumnEntity;
import com.delta.dms.community.enums.ConclusionAlertRuleType;
import com.delta.dms.community.enums.EerpRuleCellType;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.exception.UpdateConflictException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.eerpm.EerpmTopicRawData;
import com.delta.dms.community.model.eerpp.EerppTopicRawData;
import com.delta.dms.community.model.eerpq.EerpqTopicRawData;
import com.delta.dms.community.swagger.model.AlertRuleType;
import com.delta.dms.community.swagger.model.AutoCompleteUser;
import com.delta.dms.community.swagger.model.CellComponentDto;
import com.delta.dms.community.swagger.model.ConclusionAlertDetail;
import com.delta.dms.community.swagger.model.ConclusionAlertGroup;
import com.delta.dms.community.swagger.model.ConclusionAlertGroupDetail;
import com.delta.dms.community.swagger.model.ConclusionAlertMember;
import com.delta.dms.community.swagger.model.ConclusionAlertMemberType;
import com.delta.dms.community.swagger.model.ConclusionAlertRule;
import com.delta.dms.community.swagger.model.ConclusionAlertRuleDetail;
import com.delta.dms.community.swagger.model.EmailType;
import com.delta.dms.community.swagger.model.EmailWithChineseAndEnglishContext;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.RangeDayDto;
import com.delta.dms.community.swagger.model.TemplateType;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Collections;

@Service
public class ConclusionAlertService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final int MIN_DAY = 1;
  private static final int MAX_DAY = 30;
  private static final String ALERT_TITLE = "[超過%d天未結論通知] %s";
  private static final int FACTORY_ID_ALL = NumberUtils.INTEGER_ONE;
  private static final String EERPM_HIGH_LEVEL_MAIL_SENDER = "【 EERP 高發問題發佈 】 DMS";
  private static final String EERPM_HIGH_LEVEL_MAIL_TITLE_FORMAT = "%s : %s";

  private LogUtil log = LogUtil.getInstance();
  private UserService userService;
  private EventPublishService eventPublishService;
  private ForumService forumService;
  private YamlConfig yamlConfig;
  private EerpConfig eerpConfig;
  private ConclusionAlertDao conclusionAlertDao;
  private TopicDao topicDao;

  @Autowired
  public ConclusionAlertService(
      UserService userService,
      EventPublishService eventPublishService,
      ForumService forumService,
      YamlConfig yamlConfig,
      EerpConfig eerpConfig,
      ConclusionAlertDao conclusionAlertDao,
      TopicDao topicDao) {
    this.userService = userService;
    this.eventPublishService = eventPublishService;
    this.forumService = forumService;
    this.yamlConfig = yamlConfig;
    this.eerpConfig = eerpConfig;
    this.conclusionAlertDao = conclusionAlertDao;
    this.topicDao = topicDao;
  }

  public ConclusionAlertDetail getForumConclusionAlert(int forumId) {
    ForumInfo forumInfo = validatePermission(forumId);
    ForumConclusionAlert entity =
        Optional.ofNullable(conclusionAlertDao.getConclusionAlert(forumId))
            .orElseGet(ForumConclusionAlert::new);
    List<RuleColumnEntity> ruleColumnList =
        conclusionAlertDao.getRuleColumnByCommunityId(forumInfo.getCommunityId());
    Map<String, List<LabelValueDto>> dropdownMap = new HashMap<>();
    Map<String, RangeDayDto> rangeDayMap = new HashMap<>();
    ruleColumnList.forEach(
        item -> {
          EerpRuleCellType type = EerpRuleCellType.fromValue(item.getRuleColumnType());
          if (EerpRuleCellType.DROPDOWN == type) {
            dropdownMap.put(
                item.getRuleColumnName(),
                convertToLabelValueDto(
                    conclusionAlertDao.getDropdownByColumnId(item.getRuleColumnId())));
          } else if (EerpRuleCellType.RANGE_DAY == type) {
            rangeDayMap.put(
                item.getRuleColumnName(),
                convertToRangeDayDto(
                    conclusionAlertDao.getRangeDayByColumnId(item.getRuleColumnId())));
          }
        });
    ConclusionAlertDetail result =
        new ConclusionAlertDetail()
            .rules(convertToConclusionAlertRuleDto(entity.getRules(), GENERAL))
            .ruleLastModifiedTime(entity.getRuleLastModifiedTime())
            .highLevelRules(convertToConclusionAlertRuleDto(entity.getRules(), HIGH))
            .ruleColumns(convertToCellComponentDto(ruleColumnList))
            .dropdown(dropdownMap)
            .rangeDay(rangeDayMap);
    result.setGroups(convertToConclusionAlertGroupDto(entity.getGroups()));
    result.setGroupLastModifiedTime(entity.getGroupLastModifiedTime());
    return result;
  }

  @Transactional
  public void upsertForumConclusionAlertGroup(int forumId, ConclusionAlertGroupDetail data) {
    validatePermission(forumId);
    if (data.getGroups()
        .parallelStream()
        .anyMatch(
            item ->
                StringUtils.isNotEmpty(item.getKey()) && !StringUtils.isNumeric(item.getKey()))) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    data.getGroups()
        .forEach(
            item -> {
              if (data.getGroups()
                  .parallelStream()
                  .filter(group -> !group.getKey().equals(item.getKey()))
                  .anyMatch(group -> StringUtils.equals(group.getLabel(), item.getLabel()))) {
                throw new IllegalArgumentException(
                    I18nConstants.MSG_FORUM_CONCLUSION_ALERT_GROUP_DUPLICATE);
              }
            });
    ForumConclusionAlert origin =
        Optional.ofNullable(conclusionAlertDao.getConclusionAlert(forumId))
            .orElseGet(ForumConclusionAlert::new);
    if (origin.getGroupLastModifiedTime() != data.getGroupLastModifiedTime()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }

    List<Integer> deleteGroupId =
        origin
            .getGroups()
            .parallelStream()
            .filter(
                item ->
                    data.getGroups()
                        .parallelStream()
                        .noneMatch(
                            group ->
                                StringUtils.equals(
                                    group.getKey(), Integer.toString(item.getGroupId()))))
            .map(ForumConclusionAlertGroup::getGroupId)
            .collect(Collectors.toList());
    deleteConclusionAlertGroup(deleteGroupId);
    List<ForumConclusionAlertGroup> group =
        convertToConclusionAlertGroupEntity(forumId, data.getGroups());
    List<ForumConclusionAlertGroup> newGroup =
        group
            .stream()
            .filter(item -> NumberUtils.INTEGER_ZERO == item.getGroupId())
            .collect(Collectors.toList());
    List<ForumConclusionAlertGroup> updateGroup =
        group
            .stream()
            .filter(item -> NumberUtils.INTEGER_ZERO != item.getGroupId())
            .collect(Collectors.toList());
    updateConclusionAlertGroup(updateGroup);
    insertConclusionAlertGroup(newGroup);
    conclusionAlertDao.upsertConclusionAlertGroupModifiedTime(
        forumId, Utility.getUserIdFromSession(), Instant.now().toEpochMilli());
  }

  @Transactional
  public void upsertForumConclusionAlertRule(
      int forumId, AlertRuleType ruleType, ConclusionAlertRuleDetail data) {
    validatePermission(forumId);
    ForumConclusionAlert origin =
        Optional.ofNullable(conclusionAlertDao.getConclusionAlert(forumId))
            .orElseGet(ForumConclusionAlert::new);
    if (origin.getRuleLastModifiedTime() != data.getRuleLastModifiedTime()) {
      throw new UpdateConflictException(I18nConstants.MSG_COMMON_DATA_EXPIRED);
    }
    List<String> originGroups =
        origin
            .getGroups()
            .parallelStream()
            .map(item -> Integer.toString(item.getGroupId()))
            .collect(Collectors.toList());
    List<String> updateGroupMembers =
        data.getRules()
            .parallelStream()
            .flatMap(item -> item.getMembers().parallelStream())
            .filter(item -> ConclusionAlertMemberType.GROUP == item.getType())
            .map(ConclusionAlertMember::getKey)
            .collect(Collectors.toList());
    if (updateGroupMembers.parallelStream().anyMatch(item -> !originGroups.contains(item))) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }

    ConclusionAlertRuleType type = convertToConclusionAlertRuleType(ruleType);
    List<Integer> deleteRuleId =
        origin
            .getRules()
            .parallelStream()
            .filter(r -> type == r.getRuleType())
            .filter(
                item ->
                    data.getRules()
                        .parallelStream()
                        .noneMatch(rule -> rule.getKey() == item.getRuleId()))
            .map(ForumConclusionAlertRule::getRuleId)
            .collect(Collectors.toList());
    deleteConclusionAlertRule(deleteRuleId);
    List<ForumConclusionAlertRule> rule =
        convertToConclusionAlertRuleEntity(forumId, type, origin.getRules(), data.getRules());
    List<ForumConclusionAlertRule> newRule =
        rule.stream()
            .filter(item -> NumberUtils.INTEGER_ZERO == item.getRuleId())
            .collect(Collectors.toList());
    List<ForumConclusionAlertRule> updateRule =
        rule.stream()
            .filter(item -> NumberUtils.INTEGER_ZERO != item.getRuleId())
            .filter(r -> type == r.getRuleType())
            .collect(Collectors.toList());
    updateConclusionAlertRule(updateRule);
    insertConclusionAlertRule(newRule);
    conclusionAlertDao.upsertConclusionAlertRuleModifiedTime(
        forumId, Utility.getUserIdFromSession(), Instant.now().toEpochMilli());
  }

  public List<ConclusionAlertMember> searchMemberByName(
      int forumId, String q, int limit, boolean withGroup) {
    List<ForumConclusionAlertMember> groups =
        withGroup ? conclusionAlertDao.searchGroupByName(forumId, q, limit) : new ArrayList<>();
    final int userLimit = limit - groups.size();
    List<AutoCompleteUser> users =
        userLimit > NumberUtils.INTEGER_ZERO
            ? userService.searchUserByName(q, userLimit, null, false)
            : new ArrayList<>();
    List<ConclusionAlertMember> result =
        groups
            .stream()
            .map(
                item ->
                    new ConclusionAlertMember()
                        .key(item.getMemberId())
                        .label(item.getMemberName())
                        .type(ConclusionAlertMemberType.GROUP))
            .collect(Collectors.toList());

    result.addAll(
        users
            .stream()
            .map(
                item ->
                    new ConclusionAlertMember()
                        .key(item.getId())
                        .label(item.getName())
                        .type(ConclusionAlertMemberType.USER))
            .collect(Collectors.toList()));
    return result;
  }

  public void alertAllUnconcludedTopics() {
    if (!eerpConfig.getAdminId().equals(Utility.getUserIdFromSession())) {
      throw new UnauthorizedException("");
    }

    Map<Integer, Map<ConclusionAlertRuleType, Map<Integer, List<ForumConclusionAlertRule>>>>
        ruleMap = getAllConclusionAlertRuleMap(MIN_DAY, MAX_DAY);
    List<Integer> forumIdList =
        ruleMap.entrySet().parallelStream().map(Entry::getKey).collect(Collectors.toList());
    List<ConclusionAlertTopicInfo> topicInfoList =
        getAllNeedConclusionAlertTopic(forumIdList, MIN_DAY);
    topicInfoList.forEach(
        topic -> {
          List<String> memberIdList = new ArrayList<>();
          int durationKey =
              topic.getDuration() > MAX_DAY ? NumberUtils.INTEGER_MINUS_ONE : topic.getDuration();

          Optional.ofNullable(ruleMap.get(topic.getForumId()))
              .ifPresent(
                  ruleTypeMap -> {
                    ConclusionAlertRuleType ruleType =
                        TopicType.EERPMHIGHLEVEL == topic.getTopicType() ? HIGH : GENERAL;
                    Optional.ofNullable(ruleTypeMap.get(ruleType))
                        .ifPresent(
                            durationRuleMap ->
                                Optional.ofNullable(durationRuleMap.get(durationKey))
                                    .ifPresent(
                                        ruleList -> {
                                          ruleList.forEach(
                                              rule -> {
                                                if (FACTORY_ID_ALL == rule.getFactoryId()
                                                    || StringUtils.equalsIgnoreCase(
                                                        rule.getFactoryName(),
                                                        topic.getFactory())) {
                                                  memberIdList.addAll(
                                                      rule.getMembers()
                                                          .parallelStream()
                                                          .map(
                                                              ForumConclusionAlertMember
                                                                  ::getMemberId)
                                                          .collect(Collectors.toList()));
                                                }
                                              });
                                          sendMail(
                                              topic,
                                              String.format(
                                                  ALERT_TITLE,
                                                  topic.getDuration(),
                                                  topic.getTopicTitle()),
                                              memberIdList
                                                  .parallelStream()
                                                  .distinct()
                                                  .collect(Collectors.toList()));
                                        }));
                  });
        });
  }

  public void alertNewTopic(ConclusionAlertTopicInfo topic) {
    ConclusionAlertRuleType ruleType =
        TopicType.EERPMHIGHLEVEL == topic.getTopicType() ? HIGH : GENERAL;
    List<ForumConclusionAlertRule> rule =
        conclusionAlertDao.getConclusionAlertRuleWthFlatMember(
            topic.getForumId(), ruleType, NumberUtils.INTEGER_ZERO);
    sendMail(
        topic,
        topic.getTopicTitle(),
        rule.parallelStream()
            .filter(
                item ->
                    FACTORY_ID_ALL == item.getFactoryId()
                        || StringUtils.equalsIgnoreCase(
                            item.getFactoryName(),
                            getFactoryByTopicText(topic.getTopicType(), topic.getTopicText())))
            .flatMap(item -> item.getMembers().parallelStream())
            .map(ForumConclusionAlertMember::getMemberId)
            .distinct()
            .collect(Collectors.toList()));
  }

  private String getFactoryByTopicText(TopicType topicType, String topicText) {
    try {
      switch (topicType) {
        case EERPMGENERAL:
        case EERPMHIGHLEVEL:
          return mapper
              .readValue(topicText, EerpmTopicRawData.class)
              .getDeviceDatas()
              .get(NumberUtils.INTEGER_ZERO)
              .getFactory();
        case EERPQGENERAL:
          return mapper.readValue(topicText, EerpqTopicRawData.class).getFactory();
        case EERPPGENERAL:
          return mapper.readValue(topicText, EerppTopicRawData.class).getFactory();
        default:
          return StringUtils.EMPTY;
      }
    } catch (IOException e) {
      return StringUtils.EMPTY;
    }
  }

  private void insertConclusionAlertGroup(List<ForumConclusionAlertGroup> group) {
    if (CollectionUtils.isEmpty(group)) {
      return;
    }
    conclusionAlertDao.insertConclusionAlertGroup(group);
    conclusionAlertDao.insertConclusionAlertGroupMember(group);
  }

  private void updateConclusionAlertGroup(List<ForumConclusionAlertGroup> group) {
    if (CollectionUtils.isEmpty(group)) {
      return;
    }
    conclusionAlertDao.deleteConclusionAlertGroupMember(
        group
            .parallelStream()
            .map(ForumConclusionAlertGroup::getGroupId)
            .collect(Collectors.toList()));
    conclusionAlertDao.updateConclusionAlertGroup(group);
    conclusionAlertDao.insertConclusionAlertGroupMember(group);
  }

  private void deleteConclusionAlertGroup(List<Integer> groupId) {
    if (CollectionUtils.isEmpty(groupId)) {
      return;
    }
    conclusionAlertDao.deleteConclusionAlertGroup(groupId);
    conclusionAlertDao.deleteConclusionAlertRuleGroupMember(groupId);
  }

  private void insertConclusionAlertRule(List<ForumConclusionAlertRule> rule) {
    if (CollectionUtils.isEmpty(rule)) {
      return;
    }
    conclusionAlertDao.insertConclusionAlertRule(rule);
    insertConclusionAlertRuleMember(rule);
  }

  private void updateConclusionAlertRule(List<ForumConclusionAlertRule> rule) {
    if (CollectionUtils.isEmpty(rule)) {
      return;
    }
    conclusionAlertDao.deleteConclusionAlertRuleMember(
        rule.parallelStream()
            .map(ForumConclusionAlertRule::getRuleId)
            .collect(Collectors.toList()));
    conclusionAlertDao.updateConclusionAlertRule(rule);
    insertConclusionAlertRuleMember(rule);
  }

  private void insertConclusionAlertRuleMember(List<ForumConclusionAlertRule> rule) {
    List<ForumConclusionAlertRule> ruleWithMembers =
        rule.stream()
            .filter(item -> !Collections.isEmpty(item.getMembers()))
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(ruleWithMembers)) {
      return;
    }
    conclusionAlertDao.insertConclusionAlertRuleMember(ruleWithMembers);
  }

  private void deleteConclusionAlertRule(List<Integer> ruleId) {
    if (CollectionUtils.isEmpty(ruleId)) {
      return;
    }
    conclusionAlertDao.deleteConclusionAlertRule(ruleId);
  }

  private List<ConclusionAlertGroup> convertToConclusionAlertGroupDto(
      List<ForumConclusionAlertGroup> group) {
    return group
        .stream()
        .map(
            item ->
                new ConclusionAlertGroup()
                    .key(Integer.toString(item.getGroupId()))
                    .label(item.getGroupName())
                    .members(
                        item.getMembers()
                            .stream()
                            .map(
                                member ->
                                    new ConclusionAlertMember()
                                        .key(member.getMemberId())
                                        .label(member.getMemberName())
                                        .type(ConclusionAlertMemberType.USER))
                            .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  private List<ConclusionAlertRule> convertToConclusionAlertRuleDto(
      List<ForumConclusionAlertRule> rule, ConclusionAlertRuleType type) {
    return rule.stream()
        .filter(r -> type == r.getRuleType())
        .map(
            item ->
                new ConclusionAlertRule()
                    .key(item.getRuleId())
                    .startDay(item.getStartDay())
                    .endDay(item.getEndDay())
                    .factory(
                        new LabelValueDto().label(item.getFactoryName()).value(item.getFactoryId()))
                    .members(
                        item.getMembers()
                            .stream()
                            .map(
                                member ->
                                    new ConclusionAlertMember()
                                        .key(member.getMemberId())
                                        .label(member.getMemberName())
                                        .type(member.getMemberType()))
                            .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  private List<CellComponentDto> convertToCellComponentDto(List<RuleColumnEntity> data) {
    return data.stream()
        .map(
            item ->
                new CellComponentDto()
                    .cell(item.getRuleColumnName())
                    .component(item.getRuleColumnType()))
        .collect(Collectors.toList());
  }

  private List<LabelValueDto> convertToLabelValueDto(List<IdNameEntity> data) {
    return data.stream()
        .map(item -> new LabelValueDto().label(item.getName()).value(item.getId()))
        .collect(Collectors.toList());
  }

  private RangeDayDto convertToRangeDayDto(EerpRangeDayEntity data) {
    return new RangeDayDto()
        .isLimited(data.isAllowLimit())
        .range(Arrays.asList(data.getFromDay(), data.getEndDay()));
  }

  private List<ForumConclusionAlertGroup> convertToConclusionAlertGroupEntity(
      int forumId, List<ConclusionAlertGroup> group) {
    return group
        .stream()
        .map(
            item ->
                new ForumConclusionAlertGroup()
                    .setGroupId(
                        NumberUtils.isCreatable(item.getKey())
                            ? Integer.parseInt(item.getKey())
                            : NumberUtils.INTEGER_ZERO)
                    .setForumId(forumId)
                    .setGroupName(item.getLabel())
                    .setMembers(
                        item.getMembers()
                            .stream()
                            .map(
                                member ->
                                    new ForumConclusionAlertMember().setMemberId(member.getKey()))
                            .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  private List<ForumConclusionAlertRule> convertToConclusionAlertRuleEntity(
      int forumId,
      ConclusionAlertRuleType type,
      List<ForumConclusionAlertRule> origin,
      List<ConclusionAlertRule> rule) {
    Map<Integer, ConclusionAlertRuleType> typeMap =
        origin
            .parallelStream()
            .collect(
                toMap(ForumConclusionAlertRule::getRuleId, ForumConclusionAlertRule::getRuleType));
    return rule.stream()
        .map(
            item -> {
              int id = Objects.isNull(item.getKey()) ? INTEGER_ZERO : item.getKey();
              return new ForumConclusionAlertRule()
                  .setRuleId(id)
                  .setForumId(forumId)
                  .setStartDay(item.getStartDay())
                  .setEndDay(item.getEndDay())
                  .setFactoryId(
                      Objects.isNull(item.getFactory())
                          ? FACTORY_ID_ALL
                          : Integer.valueOf(item.getFactory().getValue().toString()))
                  .setRuleType(INTEGER_ZERO == id ? type : typeMap.get(item.getKey()))
                  .setMembers(
                      item.getMembers()
                          .stream()
                          .map(
                              member ->
                                  new ForumConclusionAlertMember()
                                      .setMemberId(member.getKey())
                                      .setMemberType(member.getType()))
                          .collect(Collectors.toList()));
            })
        .collect(Collectors.toList());
  }

  // <forumId, <ruleType, <duration, ForumConclusionAlertRule>>>
  private Map<Integer, Map<ConclusionAlertRuleType, Map<Integer, List<ForumConclusionAlertRule>>>>
      getAllConclusionAlertRuleMap(int minDay, int maxDay) {
    List<ForumConclusionAlertRule> entity =
        conclusionAlertDao.getAllConclusionAlertRuleWthFlatMember();
    Map<Integer, Map<ConclusionAlertRuleType, Map<Integer, List<ForumConclusionAlertRule>>>>
        result =
            entity
                .stream()
                .collect(groupingBy(ForumConclusionAlertRule::getForumId))
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        entry ->
                            entry
                                .getValue()
                                .stream()
                                .collect(groupingBy(ForumConclusionAlertRule::getRuleType))
                                .entrySet()
                                .stream()
                                .collect(
                                    toMap(
                                        Entry::getKey,
                                        list -> {
                                          Map<Integer, List<ForumConclusionAlertRule>> ruleMap =
                                              new HashMap<>();
                                          ruleMap.put(
                                              NumberUtils.INTEGER_MINUS_ONE, new ArrayList<>());
                                          rangeClosed(minDay, maxDay)
                                              .forEach(i -> ruleMap.put(i, new ArrayList<>()));
                                          list.getValue()
                                              .forEach(
                                                  item -> {
                                                    rangeClosed(
                                                            item.getStartDay(),
                                                            NumberUtils.INTEGER_ZERO
                                                                    > item.getEndDay()
                                                                ? maxDay
                                                                : item.getEndDay())
                                                        .forEach(
                                                            i -> {
                                                              if (i < minDay || i > maxDay) {
                                                                return;
                                                              }
                                                              ruleMap.get(i).add(item);
                                                            });
                                                    if (NumberUtils.INTEGER_ZERO
                                                        > item.getEndDay()) {
                                                      ruleMap
                                                          .get(NumberUtils.INTEGER_MINUS_ONE)
                                                          .add(item);
                                                    }
                                                  });
                                          return ruleMap
                                              .entrySet()
                                              .parallelStream()
                                              .filter(
                                                  rule ->
                                                      CollectionUtils.isNotEmpty(rule.getValue()))
                                              .collect(
                                                  Collectors.toMap(Entry::getKey, Entry::getValue));
                                        }))));
    log.debug("all conclusion alert rule: " + result);
    return result;
  }

  private List<ConclusionAlertTopicInfo> getAllNeedConclusionAlertTopic(
      List<Integer> forumId, int minDay) {
    List<ConclusionAlertTopicInfo> result =
        topicDao.getAllNeedConclusionAlertTopic(forumId, minDay, NumberUtils.INTEGER_MINUS_ONE);
    result.forEach(
        item -> item.setFactory(getFactoryByTopicText(item.getTopicType(), item.getTopicText())));
    return result;
  }

  private void sendMail(ConclusionAlertTopicInfo topic, String title, List<String> recipient) {
    if (CollectionUtils.isEmpty(recipient)) {
      return;
    }
    topic.setCommunityName(forumService.getForumInfoById(topic.getForumId()).getCommunityName());
    if (TopicType.EERPMHIGHLEVEL == topic.getTopicType()) {
      sendEerpmHighLevelMail(topic, recipient);
    } else {
      sendGneralMail(topic, title, recipient);
    }
  }

  private void sendGneralMail(
      ConclusionAlertTopicInfo topic, String title, List<String> recipient) {
    eventPublishService.publishEmailSendingEvent(convertToEmailContext(topic, title, recipient));
  }

  private EmailWithChineseAndEnglishContext convertToEmailContext(
      ConclusionAlertTopicInfo topic, String title, List<String> recipient) {
    final String user = Utility.getUserFromSession().getCommonName();
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.TOPICNOTIFICATION)
        .sender(user)
        .desc(String.format(EmailConstants.TOPIC_NOTIFICATION_CHINESE_FORMAT, user))
        .englishDesc(String.format(EmailConstants.TOPIC_NOTIFICATION_ENGLISH_FORMAT, user))
        .subject(EmailConstants.DMS_COMMUNITY_EMAIL + title)
        .content(topic.getTopicTitle())
        .to(userService.getEmailByUserId(recipient))
        .link(
            String.format(
                EmailConstants.TOPIC_URI_FORMAT,
                yamlConfig.getHost(),
                AcceptLanguage.get(),
                topic.getTopicId()))
        .mobileLink(
            String.format(
                EmailConstants.TOPIC_URI_FORMAT,
                yamlConfig.getMobileDownloadUrl(),
                AcceptLanguage.get(),
                topic.getTopicId()))
        .param(
            Utility.getEmailParamMap(
                String.format(EmailConstants.TITLE_FORMAT_TOPIC, topic.getTopicTitle()),
                String.format(EmailConstants.TITLE_FORMAT_FORUM, topic.getForumName())));
  }

  private void sendEerpmHighLevelMail(ConclusionAlertTopicInfo topic, List<String> recipient) {
    eventPublishService.publishEmailSendingEvent(
        convertToEerpmHighLevelEmailContext(topic, recipient), TemplateType.EERPMHIGHLEVELMAIL);
  }

  private EmailWithChineseAndEnglishContext convertToEerpmHighLevelEmailContext(
      ConclusionAlertTopicInfo topic, List<String> recipient) {
    return new EmailWithChineseAndEnglishContext()
        .type(EmailType.TOPICNOTIFICATION)
        .sender(EERPM_HIGH_LEVEL_MAIL_SENDER)
        .subject(
            String.format(
                EERPM_HIGH_LEVEL_MAIL_TITLE_FORMAT, topic.getCommunityName(), topic.getForumName()))
        .to(userService.getEmailByUserId(recipient))
        .link(
            String.format(
                EmailConstants.TOPIC_URI_FORMAT,
                yamlConfig.getHost(),
                AcceptLanguage.get(),
                topic.getTopicId()))
        .mobileLink(
            String.format(
                EmailConstants.TOPIC_URI_FORMAT,
                yamlConfig.getMobileDownloadUrl(),
                AcceptLanguage.get(),
                topic.getTopicId()))
        .param(generateMailParameters(topic));
  }

  private Map<String, Object> generateMailParameters(ConclusionAlertTopicInfo topic) {
    Map<String, Object> param = new HashMap<>();
    param.put(EmailConstants.MAIL_TITLE, topic.getTopicTitle());
    try {
      EerpmTopicRawData topicContent =
          mapper.readValue(topic.getTopicText(), EerpmTopicRawData.class);
      param.put(
          EmailConstants.MAIL_ERROR_CODE,
          topicContent.getDeviceDatas().get(NumberUtils.INTEGER_ZERO).getErrorCode());
      param.put(EmailConstants.MAIL_HISTORIES, topicContent.getHistories());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return param;
  }

  private ForumInfo validatePermission(int forumId) {
    ForumInfo forumInfo = forumService.getForumInfoById(forumId);
    if (!forumInfo.isConclusionAlert()
        || (!userService.isSysAdmin()
            && !forumService.checkUserIsAdmin(Utility.getUserIdFromSession(), forumInfo))) {
      throw new UnauthorizedException("");
    }
    return forumInfo;
  }

  private ConclusionAlertRuleType convertToConclusionAlertRuleType(AlertRuleType type) {
    switch (type) {
      case GENERAL:
        return GENERAL;
      case HIGH:
        return HIGH;
      default:
        return GENERAL;
    }
  }
}
