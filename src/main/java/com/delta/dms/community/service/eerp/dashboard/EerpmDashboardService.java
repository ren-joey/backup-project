package com.delta.dms.community.service.eerp.dashboard;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.TopicEerpmEntity;
import com.delta.dms.community.enums.CommunitySpecialType;
import com.delta.dms.community.enums.EerpmDashboardTableColumn;
import com.delta.dms.community.enums.EerpmErrorLevel;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.swagger.model.ChartDto;
import com.delta.dms.community.swagger.model.ColumnDto;
import com.delta.dms.community.swagger.model.ConclusionChartDto;
import com.delta.dms.community.swagger.model.EerpmDashboardDeviceDataDto;
import com.delta.dms.community.swagger.model.EerpmDashboardDeviceDto;
import com.delta.dms.community.swagger.model.EerpmDashboardDeviceTableDto;
import com.delta.dms.community.swagger.model.EerpmDashboardTopicDataDto;
import com.delta.dms.community.swagger.model.EerpmDashboardTopicDto;
import com.delta.dms.community.swagger.model.EerpmDashboardTopicTableDto;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerpmDashboardService
    extends BaseDashboardService<
        EerpmDashboardTopicDto, EerpmDashboardDeviceDto, TopicEerpmEntity> {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Autowired
  public EerpmDashboardService(
      CommunityService communityService,
      EerpConfig eerpConfig,
      YamlConfig yamlConfig,
      EerpDao eerpDao) {
    super(eerpConfig, yamlConfig, eerpDao, communityService);
  }

  public List<ChartDto> getDashboardDeviceHistory(
      int communityId,
      long endTime,
      String factory,
      String forum,
      String deviceModel,
      String errorCode) {
    Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
    endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
    long startTime =
        getMonthlyReportStartTime(firstDayOfMonth, eerpConfig.getMConclusionReportDuration());
    validateEndDate(eerpConfig.getMConclusionReportStartTime(), firstDayOfMonth.toEpochMilli());
    if (StringUtils.isAnyBlank(factory, forum, deviceModel)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }

    Map<String, List<Object>> filterMap =
        convertToFilterMap(factory, forum, deviceModel, errorCode);
    TopicEerpmEntity topicHistoryEntity =
        convertToTopicHistoryMap(getAllTopics(communityId, startTime, endTime, filterMap))
            .values()
            .stream()
            .findFirst()
            .orElseGet(TopicEerpmEntity::new);
    return convertToHistoryChartDto(topicHistoryEntity.getHistory(), startTime, endTime);
  }

  public Map<String, List<Object>> convertToFilterMap(
      String factory, String forum, String deviceModel, String errorCode) {
    Map<String, List<Object>> filterMap = new HashMap<>();
    filterMap.put(EerpmDashboardTableColumn.FACTORY.toString(), Collections.singletonList(factory));
    filterMap.put(EerpmDashboardTableColumn.FORUM.toString(), Collections.singletonList(forum));
    filterMap.put(
        EerpmDashboardTableColumn.DEVICE_MODEL.toString(), Collections.singletonList(deviceModel));
    filterMap.put(
        EerpmDashboardTableColumn.ERROR_CODE.toString(), Collections.singletonList(errorCode));
    return filterMap;
  }

  @Override
  protected CommunitySpecialType getType() {
    return CommunitySpecialType.EERPM;
  }

  @Override
  protected EerpmDashboardTopicDto searchDashboardTopic(
      List<TopicEerpmEntity> allTopicList,
      List<TopicEerpmEntity> topicList,
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap) {
    return new EerpmDashboardTopicDto()
        .table(
            convertToEerpmDashboardTopicTableDto(topicList, communityId, startTime, endTime)
                .total(eerpDao.countEerpmTopic(communityId, startTime, endTime, filterMap)))
        .factoryChart(convertToEerpmFactoryConclusionChart(allTopicList))
        .forumChart(convertToEerpmForumConclusionChart(allTopicList));
  }

  @Override
  protected EerpmDashboardDeviceDto searchDashboardDevice(
      List<TopicEerpmEntity> allTopicHistoryList,
      List<TopicEerpmEntity> topicHistoryList,
      List<TopicEerpmEntity> allTopicList,
      int communityId,
      long startTime,
      long endTime) {
    return new EerpmDashboardDeviceDto()
        .table(
            convertToEerpmDashboardDeviceTableDto(topicHistoryList, communityId, startTime, endTime)
                .total(allTopicHistoryList.size()))
        .deviceModelChart(convertToDeviceModelChart(allTopicHistoryList))
        .errorLevelChart(convertToEerpErrorLevelChart(allTopicHistoryList));
  }

  @Override
  protected long getConclusionReportDuration() {
    return eerpConfig.getMConclusionReportDuration();
  }

  @Override
  protected long getConclusionReportStartTime() {
    return eerpConfig.getMConclusionReportStartTime();
  }

  @Override
  protected long getConclusionReportEffectiveDuration() {
    return eerpConfig.getMConclusionReportEffectiveDuration();
  }

  @Override
  public List<TopicEerpmEntity> getAllTopics(int communityId, long startTime, long endTime) {
    return getAllTopics(communityId, startTime, endTime, Collections.emptyMap());
  }

  @Override
  public List<TopicEerpmEntity> getAllTopics(
      int communityId, long startTime, long endTime, Map<String, List<Object>> filterMap) {
    return getTopics(
        communityId,
        startTime,
        endTime,
        filterMap,
        NumberUtils.INTEGER_MINUS_ONE,
        NumberUtils.INTEGER_MINUS_ONE);
  }

  @Override
  protected List<TopicEerpmEntity> getTopics(
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap,
      int offset,
      int limit) {
    List<TopicEerpmEntity> result =
        eerpDao.getEerpmTopic(communityId, startTime, endTime, filterMap, offset, limit);
    result.forEach(
        item ->
            item.setId(
                String.join(
                    EERP_ID_DELIMITER,
                    item.getFactory(),
                    item.getForumName(),
                    item.getDeviceModel(),
                    item.getErrorCode())));
    return result;
  }

  @Override
  protected Map<String, TopicEerpmEntity> filterOtherItems(
      Map<String, TopicEerpmEntity> topicHistoryMap, Map<String, List<Object>> filterMap) {
    List<Integer> filterErrorLevelList =
        filterMap
            .getOrDefault(EerpmDashboardTableColumn.ERROR_LEVEL.toString(), emptyList())
            .stream()
            .map(item -> Integer.valueOf(item.toString()))
            .collect(Collectors.toList());

    return topicHistoryMap
        .entrySet()
        .stream()
        .filter(
            entry ->
                filterErrorLevelList.isEmpty()
                    || filterErrorLevelList.contains(entry.getValue().getLevel().getId()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Map<String, TopicEerpmEntity> convertToTopicHistoryMap(List<TopicEerpmEntity> topics) {
    Map<String, TopicEerpmEntity> result = new LinkedHashMap<>();
    topics.forEach(
        item -> {
          TopicEerpmEntity defaultTopicHistory =
              new TopicEerpmEntity()
                  .setForumName(item.getForumName())
                  .setFactory(item.getFactory())
                  .setDeviceModel(item.getDeviceModel())
                  .setErrorCode(item.getErrorCode())
                  .setId(item.getId());
          TopicEerpmEntity topicHistory =
              Optional.ofNullable(result.putIfAbsent(item.getId(), defaultTopicHistory))
                  .orElse(defaultTopicHistory);
          topicHistory.setErrorCount(topicHistory.getErrorCount() + item.getErrorCount());
          try {
            String[] worstDevices = mapper.readValue(item.getWorstDeviceId(), String[].class);
            topicHistory.getWorstDevices().addAll(Arrays.asList(worstDevices));
          } catch (IOException e) {
            e.printStackTrace();
          }

          long historyKey = truncateToHalfMonth(item.getTopicCreateTime());
          Optional.ofNullable(
                  topicHistory
                      .getHistory()
                      .putIfAbsent(
                          historyKey, new ArrayList<>(Arrays.asList(item.getErrorCount()))))
              .ifPresent(countList -> countList.add(item.getErrorCount()));
        });
    result
        .values()
        .forEach(
            item -> {
              item.setLevel(convertToEerpErrorLevel(item.getHistory()));
              item.setWorstDevices(
                  item.getWorstDevices().stream().distinct().collect(Collectors.toList()));
            });
    return result;
  }

  @Override
  protected Map<String, List<TopicEerpmEntity>> convertToPreviousDeviceIdMap(
      List<TopicEerpmEntity> topics) {
    return topics
        .stream()
        .collect(
            Collectors.groupingBy(
                TopicEerpmEntity::getId, LinkedHashMap::new, Collectors.toList()));
  }

  @Override
  protected List<LabelValueDto> getEffectiveSolution(
      Map<String, List<TopicEerpmEntity>> previousDeviceIdMap,
      Map<String, TopicEerpmEntity> topicHistoryMap) {
    return previousDeviceIdMap
        .entrySet()
        .stream()
        .filter(entry -> Objects.isNull(topicHistoryMap.get(entry.getKey())))
        .map(
            entry ->
                new LabelValueDto()
                    .value(
                        entry
                            .getValue()
                            .parallelStream()
                            .mapToInt(TopicEerpmEntity::getTopicId)
                            .max()
                            .orElse(NumberUtils.INTEGER_ZERO))
                    .label(entry.getKey()))
        .collect(Collectors.toList());
  }

  @Override
  protected List<ChartDto> convertToConclusionChart(List<TopicEerpmEntity> topics) {
    Map<Integer, String> conclusionStateMap =
        eerpDao
            .getAllConclusionState(AcceptLanguage.getLanguageForDb())
            .stream()
            .collect(
                toMap(item -> Integer.valueOf(item.getId().toString()), IdNameEntity::getName));
    Map<Integer, Long> topicConclusionMap =
        topics.stream().collect(groupingBy(TopicEerpmEntity::getConclusionStateId, counting()));
    return topicConclusionMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ChartDto()
                    .value(entry.getKey())
                    .label(conclusionStateMap.get(entry.getKey()))
                    .total(entry.getValue())
                    .color(eerpConfig.getEerpChartConclusionColor(entry.getKey())))
        .sorted(
            comparing(
                ChartDto::getValue,
                (v1, v2) ->
                    Integer.valueOf(v2.toString()).compareTo(Integer.valueOf(v1.toString()))))
        .collect(toList());
  }

  @Override
  protected List<ChartDto> convertToFactoryChart(List<TopicEerpmEntity> topics) {
    Map<String, Long> topicFactoryMap =
        topics.stream().collect(groupingBy(TopicEerpmEntity::getFactory, counting()));
    return topicFactoryMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ChartDto()
                    .value(entry.getKey())
                    .label(entry.getKey())
                    .total(entry.getValue())
                    .color(eerpConfig.getChartDefaultColor()))
        .sorted(comparing(ChartDto::getTotal).reversed())
        .collect(toList());
  }

  @Override
  protected List<ChartDto> convertToDepartmentChart(List<TopicEerpmEntity> topics) {
    return emptyList();
  }

  @Override
  protected List<ChartDto> convertToForumChart(List<TopicEerpmEntity> topics) {
    Map<String, Long> topicForumMap =
        topics
            .stream()
            .collect(Collectors.groupingBy(TopicEerpmEntity::getForumName, Collectors.counting()));
    return topicForumMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ChartDto()
                    .value(entry.getKey())
                    .label(entry.getKey())
                    .total(entry.getValue())
                    .color(eerpConfig.getChartDefaultColor()))
        .sorted(comparing(ChartDto::getTotal).reversed())
        .collect(toList());
  }

  @Override
  protected List<ChartDto> convertToErrorLevelChart(Map<String, TopicEerpmEntity> topicHistoryMap) {
    return convertToEerpErrorLevelChart(topicHistoryMap.values().stream().collect(toList()));
  }

  private List<ChartDto> convertToEerpErrorLevelChart(List<TopicEerpmEntity> topicHistoryList) {
    Map<EerpmErrorLevel, Long> topicErrorLevelMap =
        topicHistoryList.stream().collect(groupingBy(TopicEerpmEntity::getLevel, counting()));
    return topicErrorLevelMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ChartDto()
                    .value(entry.getKey().getId())
                    .label(entry.getKey().toString())
                    .total(entry.getValue())
                    .color(eerpConfig.getEerpChartErrorLevelColor(entry.getKey().getId())))
        .sorted(
            Comparator.comparing(
                ChartDto::getValue,
                (v1, v2) ->
                    Integer.valueOf(v2.toString()).compareTo(Integer.valueOf(v1.toString()))))
        .collect(toList());
  }

  private EerpmErrorLevel convertToEerpErrorLevel(Map<Long, List<Long>> historyMap) {
    return Optional.ofNullable(
            EerpmErrorLevel.fromCount(
                historyMap.values().parallelStream().mapToInt(CollectionUtils::size).sum()))
        .orElseThrow(() -> new IllegalArgumentException(Constants.ERR_INVALID_PARAM));
  }

  private EerpmDashboardTopicTableDto convertToEerpmDashboardTopicTableDto(
      List<TopicEerpmEntity> topicList, int communityId, long startTime, long endTime) {
    Map<Integer, String> conclusionStateMap =
        eerpDao
            .getAllConclusionState(AcceptLanguage.getLanguageForDb())
            .stream()
            .collect(
                Collectors.toMap(
                    item -> Integer.valueOf(item.getId().toString()), IdNameEntity::getName));
    List<EerpmDashboardTopicDataDto> dataList =
        topicList
            .stream()
            .map(
                item ->
                    new EerpmDashboardTopicDataDto()
                        .id(item.getTopicId())
                        .factory(item.getFactory())
                        .forum(item.getForumName())
                        .topicTitle(item.getTopicTitle())
                        .conclusion(conclusionStateMap.get(item.getConclusionStateId())))
            .collect(Collectors.toList());
    List<ColumnDto> columnList = new ArrayList<>();
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.FACTORY.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.FACTORY.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.FORUM.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.FORUM.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.TOPIC_TITLE.toString())
            .filters(Collections.emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.CONCLUSION.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.CONCLUSION.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(
                        item -> new LabelValueDto().value(item).label(conclusionStateMap.get(item)))
                    .collect(Collectors.toList())));

    return new EerpmDashboardTopicTableDto().data(dataList).columns(columnList);
  }

  private List<ConclusionChartDto> convertToEerpmFactoryConclusionChart(
      List<TopicEerpmEntity> topicList) {
    return convertToConclusionChartDto(
        topicList.stream().collect(Collectors.groupingBy(TopicEerpmEntity::getFactory)));
  }

  private List<ConclusionChartDto> convertToEerpmForumConclusionChart(
      List<TopicEerpmEntity> topicList) {
    return convertToConclusionChartDto(
        topicList.stream().collect(Collectors.groupingBy(TopicEerpmEntity::getForumName)));
  }

  private List<ConclusionChartDto> convertToConclusionChartDto(
      Map<String, List<TopicEerpmEntity>> topicMap) {
    Map<Integer, String> conclusionStateMap =
        eerpDao
            .getAllConclusionState(AcceptLanguage.getLanguageForDb())
            .stream()
            .collect(
                Collectors.toMap(
                    item -> Integer.valueOf(item.getId().toString()), IdNameEntity::getName));
    Map<String, Map<Integer, Long>> topicConclusionMap =
        topicMap
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    e ->
                        e.getValue()
                            .stream()
                            .collect(
                                Collectors.groupingBy(
                                    TopicEerpmEntity::getConclusionStateId,
                                    Collectors.counting()))));
    return topicConclusionMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ConclusionChartDto()
                    .label(entry.getKey())
                    .total(
                        entry
                            .getValue()
                            .entrySet()
                            .stream()
                            .collect(Collectors.summarizingLong(Entry::getValue))
                            .getSum())
                    .data(convertToConclusionChartDetailDto(conclusionStateMap, entry.getValue())))
        .sorted(Comparator.comparing(ConclusionChartDto::getTotal).reversed())
        .collect(Collectors.toList());
  }

  private EerpmDashboardDeviceTableDto convertToEerpmDashboardDeviceTableDto(
      List<TopicEerpmEntity> topicList, int communityId, long startTime, long endTime) {
    List<EerpmDashboardDeviceDataDto> dataList =
        topicList
            .stream()
            .map(
                item ->
                    new EerpmDashboardDeviceDataDto()
                        .factory(item.getFactory())
                        .forum(item.getForumName())
                        .deviceModel(item.getDeviceModel())
                        .worstDevices(item.getWorstDevices())
                        .errorCode(item.getErrorCode())
                        .errorCount(item.getErrorCount())
                        .errorLevel(item.getLevel().toString()))
            .collect(Collectors.toList());
    List<ColumnDto> columnList = new ArrayList<>();
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.FACTORY.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.FACTORY.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.FORUM.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.FORUM.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.DEVICE_MODEL.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.DEVICE_MODEL.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.WORST_DEVICES.toString())
            .filters(Collections.emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.ERROR_CODE.toString())
            .filters(
                eerpDao
                    .getEerpmDistinctColumn(
                        EerpmDashboardTableColumn.ERROR_CODE.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(item -> new LabelValueDto().value(item).label(item.toString()))
                    .collect(Collectors.toList())));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.ERROR_COUNT.toString())
            .filters(Collections.emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.ERROR_LEVEL.toString())
            .filters(
                EnumSet.allOf(EerpmErrorLevel.class)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.getId()).label(item.toString()))
                    .collect(Collectors.toList())));

    return new EerpmDashboardDeviceTableDto().data(dataList).columns(columnList);
  }

  private List<ChartDto> convertToDeviceModelChart(List<TopicEerpmEntity> topicList) {
    Map<String, Long> topicDeviceModelMap =
        topicList
            .stream()
            .collect(Collectors.groupingBy(TopicEerpmEntity::getDeviceModel))
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    e -> e.getValue().stream().mapToLong(TopicEerpmEntity::getErrorCount).sum()));
    return topicDeviceModelMap
        .entrySet()
        .stream()
        .map(
            entry ->
                new ChartDto()
                    .value(entry.getKey())
                    .label(entry.getKey())
                    .total(entry.getValue())
                    .color(eerpConfig.getChartDefaultColor()))
        .sorted(Comparator.comparing(ChartDto::getTotal).reversed())
        .collect(Collectors.toList());
  }
}
