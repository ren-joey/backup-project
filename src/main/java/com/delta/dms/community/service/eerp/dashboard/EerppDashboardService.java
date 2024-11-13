package com.delta.dms.community.service.eerp.dashboard;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
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
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.TopicEerppEntity;
import com.delta.dms.community.enums.CommunitySpecialType;
import com.delta.dms.community.enums.EerpErrorLevel;
import com.delta.dms.community.enums.EerpmDashboardTableColumn;
import com.delta.dms.community.enums.EerppDashboardTableColumn;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.swagger.model.ChartDto;
import com.delta.dms.community.swagger.model.ColumnDto;
import com.delta.dms.community.swagger.model.ConclusionChartDto;
import com.delta.dms.community.swagger.model.EerppDashboardDeviceDataDto;
import com.delta.dms.community.swagger.model.EerppDashboardDeviceDto;
import com.delta.dms.community.swagger.model.EerppDashboardDeviceTableDto;
import com.delta.dms.community.swagger.model.EerppDashboardTopicDataDto;
import com.delta.dms.community.swagger.model.EerppDashboardTopicDto;
import com.delta.dms.community.swagger.model.EerppDashboardTopicTableDto;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerppDashboardService
    extends BaseDashboardService<
        EerppDashboardTopicDto, EerppDashboardDeviceDto, TopicEerppEntity> {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public EerppDashboardService(
      EerpConfig eerpConfig,
      YamlConfig yamlConfig,
      EerpDao eerpDao,
      CommunityService communityService) {
    super(eerpConfig, yamlConfig, eerpDao, communityService);
  }

  public List<ChartDto> getDashboardDeviceHistory(
      int communityId, long endTime, String factory, String forum, String lossCode) {
    Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
    endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
    long startTime =
        getMonthlyReportStartTime(firstDayOfMonth, eerpConfig.getMConclusionReportDuration());
    validateEndDate(eerpConfig.getMConclusionReportStartTime(), firstDayOfMonth.toEpochMilli());
    if (StringUtils.isAnyBlank(factory, forum, lossCode)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }

    Map<String, List<Object>> filterMap = new HashMap<>();
    filterMap.put(EerppDashboardTableColumn.FACTORY.toString(), Collections.singletonList(factory));
    filterMap.put(EerppDashboardTableColumn.FORUM.toString(), Collections.singletonList(forum));
    filterMap.put(
        EerppDashboardTableColumn.LOSS_CODE.toString(), Collections.singletonList(lossCode));

    TopicEerppEntity topicHistoryEntity =
        convertToTopicHistoryMap(getAllTopics(communityId, startTime, endTime, filterMap))
            .values()
            .stream()
            .findFirst()
            .orElseGet(TopicEerppEntity::new);
    return convertToHistoryChartDto(
        topicHistoryEntity
            .getHistory()
            .entrySet()
            .stream()
            .collect(
                toMap(
                    Entry::getKey, e -> e.getValue().stream().map(Math::round).collect(toList()))),
        startTime,
        endTime);
  }

  @Override
  protected CommunitySpecialType getType() {
    return CommunitySpecialType.EERPP;
  }

  @Override
  protected EerppDashboardTopicDto searchDashboardTopic(
      List<TopicEerppEntity> allTopicList,
      List<TopicEerppEntity> topicList,
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap) {
    return new EerppDashboardTopicDto()
        .table(
            convertToDashboardTopicTableDto(topicList, communityId, startTime, endTime)
                .total(eerpDao.countEerppTopic(communityId, startTime, endTime, filterMap)))
        .factoryChart(convertToFactoryConclusionChart(allTopicList))
        .forumChart(convertToForumConclusionChart(allTopicList))
        .departmentChart(convertToDepartmentConclusionChart(allTopicList));
  }

  @Override
  protected EerppDashboardDeviceDto searchDashboardDevice(
      List<TopicEerppEntity> allTopicHistoryList,
      List<TopicEerppEntity> topicHistoryList,
      List<TopicEerppEntity> allTopicList,
      int communityId,
      long startTime,
      long endTime) {
    return new EerppDashboardDeviceDto()
        .table(
            convertToDashboardDeviceTableDto(topicHistoryList, communityId, startTime, endTime)
                .total(allTopicHistoryList.size()))
        .departmentChart(convertToDepartmentDurationChart(allTopicHistoryList))
        .lossDescChart(convertToLossDescChart(allTopicList))
        .errorLevelChart(convertToErrorLevelChart(allTopicHistoryList));
  }

  @Override
  protected long getConclusionReportDuration() {
    return eerpConfig.getPConclusionReportDuration();
  }

  @Override
  protected long getConclusionReportStartTime() {
    return eerpConfig.getPConclusionReportStartTime();
  }

  @Override
  protected long getConclusionReportEffectiveDuration() {
    return eerpConfig.getPConclusionReportEffectiveDuration();
  }

  @Override
  public List<TopicEerppEntity> getAllTopics(int communityId, long startTime, long endTime) {
    return getAllTopics(communityId, startTime, endTime, Collections.emptyMap());
  }

  @Override
  public List<TopicEerppEntity> getAllTopics(
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
  protected List<TopicEerppEntity> getTopics(
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap,
      int offset,
      int limit) {
    List<TopicEerppEntity> result =
        eerpDao.getEerppTopic(
            communityId, startTime, endTime, filterMap, offset, limit, AcceptLanguage.get());
    result.forEach(
        item ->
            item.setId(
                    String.join(
                        EERP_ID_DELIMITER,
                        item.getFactory(),
                        item.getForumName(),
                        item.getLossCode()))
                .setDurations(convertToDurations(item.getDuration()))
                .setLines(convertToLines(item.getLine())));
    return result;
  }

  @Override
  protected Map<String, TopicEerppEntity> filterOtherItems(
      Map<String, TopicEerppEntity> topicHistoryMap, Map<String, List<Object>> filterMap) {
    List<Integer> filterErrorLevelList =
        filterMap
            .getOrDefault(EerppDashboardTableColumn.ERROR_LEVEL.toString(), emptyList())
            .stream()
            .map(item -> Integer.valueOf(item.toString()))
            .collect(Collectors.toList());
    List<String> filterAreaList =
        filterMap
            .getOrDefault(EerppDashboardTableColumn.AREAS.toString(), emptyList())
            .stream()
            .map(item -> item.toString())
            .collect(Collectors.toList());

    return topicHistoryMap
        .entrySet()
        .stream()
        .filter(
            entry ->
                filterErrorLevelList.isEmpty()
                    || filterErrorLevelList.contains(entry.getValue().getLevel().getId()))
        .filter(
            entry ->
                filterAreaList.isEmpty()
                    || filterAreaList
                        .parallelStream()
                        .anyMatch(filter -> entry.getValue().getAreas().contains(filter)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Map<String, TopicEerppEntity> convertToTopicHistoryMap(List<TopicEerppEntity> topics) {
    Map<String, TopicEerppEntity> result = new LinkedHashMap<>();
    topics.forEach(
        item -> {
          TopicEerppEntity defaultTopicHistory =
              new TopicEerppEntity()
                  .setFactory(item.getFactory())
                  .setDepartment(item.getDepartment())
                  .setForumName(item.getForumName())
                  .setLossCode(item.getLossCode())
                  .setLossCodeDesc(item.getLossCodeDesc())
                  .setId(item.getId());
          TopicEerppEntity topicHistory =
              Optional.ofNullable(result.putIfAbsent(item.getId(), defaultTopicHistory))
                  .orElse(defaultTopicHistory);
          topicHistory.getLines().addAll(item.getLines());
          topicHistory.getAreas().add(item.getArea());
          topicHistory.getLossCodeDescriptions().add(item.getLossCodeDesc());
          topicHistory.getDurations().addAll(item.getDurations());

          long historyKey = truncateToHalfMonth(item.getTopicCreateTime());
          double duration = item.getDurations().parallelStream().mapToDouble(d -> d).sum();
          Optional.ofNullable(
                  topicHistory
                      .getHistory()
                      .putIfAbsent(historyKey, new ArrayList<>(Arrays.asList(duration))))
              .ifPresent(countList -> countList.add(duration));
        });
    result
        .values()
        .forEach(
            item -> {
              item.setLevel(convertToEerpErrorLevel(item.getHistory()));
              item.setLines(distinct(item.getLines()));
              item.setAreas(distinct(item.getAreas()));
              item.setLossCodeDescriptions(distinct(item.getLossCodeDescriptions()));
              item.setDurationCount(
                  Math.round(item.getDurations().parallelStream().mapToDouble(d -> d).sum()));
            });
    return result;
  }

  @Override
  protected Map<String, List<TopicEerppEntity>> convertToPreviousDeviceIdMap(
      List<TopicEerppEntity> topics) {
    return topics
        .stream()
        .collect(
            Collectors.groupingBy(
                TopicEerppEntity::getId, LinkedHashMap::new, Collectors.toList()));
  }

  @Override
  protected List<LabelValueDto> getEffectiveSolution(
      Map<String, List<TopicEerppEntity>> previousDeviceIdMap,
      Map<String, TopicEerppEntity> topicHistoryMap) {
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
                            .mapToInt(TopicEerppEntity::getTopicId)
                            .max()
                            .orElse(NumberUtils.INTEGER_ZERO))
                    .label(entry.getKey()))
        .collect(Collectors.toList());
  }

  @Override
  protected List<ChartDto> convertToConclusionChart(List<TopicEerppEntity> topics) {
    Map<Integer, String> conclusionStateMap =
        eerpDao
            .getAllConclusionState(AcceptLanguage.getLanguageForDb())
            .stream()
            .collect(
                toMap(item -> Integer.valueOf(item.getId().toString()), IdNameEntity::getName));
    Map<Integer, Long> topicConclusionMap =
        topics.stream().collect(groupingBy(TopicEerppEntity::getConclusionStateId, counting()));
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
  protected List<ChartDto> convertToFactoryChart(List<TopicEerppEntity> topics) {
    Map<String, Long> topicFactoryMap =
        topics
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getFactory, Collectors.counting()));
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
        .sorted(Comparator.comparing(ChartDto::getTotal).reversed())
        .collect(Collectors.toList());
  }

  @Override
  protected List<ChartDto> convertToDepartmentChart(List<TopicEerppEntity> topics) {
    Map<String, Long> topicDepartmentMap =
        topics
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getDepartment, Collectors.counting()));
    return topicDepartmentMap
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

  @Override
  protected List<ChartDto> convertToForumChart(List<TopicEerppEntity> topics) {
    Map<String, Long> topicForumMap =
        topics
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getForumName, Collectors.counting()));
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
        .sorted(Comparator.comparing(ChartDto::getTotal).reversed())
        .collect(Collectors.toList());
  }

  @Override
  protected List<ChartDto> convertToErrorLevelChart(Map<String, TopicEerppEntity> topicHistoryMap) {
    return convertToErrorLevelChart(topicHistoryMap.values().stream().collect(Collectors.toList()));
  }

  private EerppDashboardTopicTableDto convertToDashboardTopicTableDto(
      List<TopicEerppEntity> topicList, int communityId, long startTime, long endTime) {
    Map<Integer, String> conclusionStateMap =
        eerpDao
            .getAllConclusionState(AcceptLanguage.getLanguageForDb())
            .stream()
            .collect(
                Collectors.toMap(
                    item -> Integer.valueOf(item.getId().toString()), IdNameEntity::getName));
    List<EerppDashboardTopicDataDto> dataList =
        topicList
            .stream()
            .map(
                item ->
                    new EerppDashboardTopicDataDto()
                        .id(item.getTopicId())
                        .factory(item.getFactory())
                        .department(item.getDepartment())
                        .forum(item.getForumName())
                        .area(item.getArea())
                        .lossCode(item.getLossCode())
                        .lossDescription(item.getLossCodeDesc())
                        .duration(
                            Math.round(
                                item.getDurations().parallelStream().mapToDouble(d -> d).sum()))
                        .conclusion(conclusionStateMap.get(item.getConclusionStateId())))
            .collect(Collectors.toList());
    List<ColumnDto> columnList = new ArrayList<>();
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.FACTORY, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.DEPARTMENT, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.FORUM, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.AREA, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.LOSS_CODE, communityId, startTime, endTime));
    columnList.add(
        new ColumnDto()
            .value(EerppDashboardTableColumn.LOSS_DESC.toString())
            .filters(Collections.emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerppDashboardTableColumn.DURATION.toString())
            .filters(Collections.emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerppDashboardTableColumn.CONCLUSION.toString())
            .filters(
                eerpDao
                    .getEerppDistinctColumn(
                        EerppDashboardTableColumn.CONCLUSION.getDbColumnName(),
                        communityId,
                        startTime,
                        endTime)
                    .stream()
                    .map(
                        item -> new LabelValueDto().value(item).label(conclusionStateMap.get(item)))
                    .collect(Collectors.toList())));

    return new EerppDashboardTopicTableDto().data(dataList).columns(columnList);
  }

  private List<ConclusionChartDto> convertToFactoryConclusionChart(
      List<TopicEerppEntity> topicList) {
    return convertToConclusionChartDto(
        topicList.stream().collect(Collectors.groupingBy(TopicEerppEntity::getFactory)));
  }

  private List<ConclusionChartDto> convertToForumConclusionChart(List<TopicEerppEntity> topicList) {
    return convertToConclusionChartDto(
        topicList.stream().collect(Collectors.groupingBy(TopicEerppEntity::getForumName)));
  }

  private List<ConclusionChartDto> convertToDepartmentConclusionChart(
      List<TopicEerppEntity> topicList) {
    return convertToConclusionChartDto(
        topicList.stream().collect(Collectors.groupingBy(TopicEerppEntity::getDepartment)));
  }

  private List<ConclusionChartDto> convertToConclusionChartDto(
      Map<String, List<TopicEerppEntity>> topicMap) {
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
                                    TopicEerppEntity::getConclusionStateId,
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

  private ColumnDto convertToColumnDto(
      EerppDashboardTableColumn column, int communityId, long startTime, long endTime) {
    return new ColumnDto()
        .value(column.toString())
        .filters(
            eerpDao
                .getEerppDistinctColumn(column.getDbColumnName(), communityId, startTime, endTime)
                .stream()
                .map(item -> new LabelValueDto().value(item.toString()).label(item.toString()))
                .collect(Collectors.toList()));
  }

  private EerppDashboardDeviceTableDto convertToDashboardDeviceTableDto(
      List<TopicEerppEntity> topicList, int communityId, long startTime, long endTime) {
    List<EerppDashboardDeviceDataDto> dataList =
        topicList
            .stream()
            .map(
                item ->
                    new EerppDashboardDeviceDataDto()
                        .factory(item.getFactory())
                        .department(item.getDepartment())
                        .forum(item.getForumName())
                        .lines(item.getLines())
                        .areas(item.getAreas())
                        .lossCode(item.getLossCode())
                        .lossDescriptions(item.getLossCodeDescriptions())
                        .duration(
                            Math.round(
                                item.getDurations().parallelStream().mapToDouble(d -> d).sum()))
                        .errorLevel(item.getLevel().toString()))
            .collect(Collectors.toList());
    List<ColumnDto> columnList = new ArrayList<>();
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.FACTORY, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.DEPARTMENT, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.FORUM, communityId, startTime, endTime));
    columnList.add(
        new ColumnDto().value(EerppDashboardTableColumn.LINE.toString()).filters(emptyList()));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.AREAS, communityId, startTime, endTime));
    columnList.add(
        convertToColumnDto(EerppDashboardTableColumn.LOSS_CODE, communityId, startTime, endTime));
    columnList.add(
        new ColumnDto()
            .value(EerppDashboardTableColumn.LOSS_DESCS.toString())
            .filters(emptyList()));
    columnList.add(
        new ColumnDto().value(EerppDashboardTableColumn.DURATION.toString()).filters(emptyList()));
    columnList.add(
        new ColumnDto()
            .value(EerpmDashboardTableColumn.ERROR_LEVEL.toString())
            .filters(
                EnumSet.allOf(EerpErrorLevel.class)
                    .stream()
                    .map(item -> new LabelValueDto().value(item.getId()).label(item.toString()))
                    .collect(Collectors.toList())));

    return new EerppDashboardDeviceTableDto().data(dataList).columns(columnList);
  }

  private List<ChartDto> convertToDepartmentDurationChart(List<TopicEerppEntity> topicList) {
    Map<String, Long> topicDepartmentMap =
        topicList
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getDepartment))
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    e ->
                        Math.round(
                            e.getValue()
                                .parallelStream()
                                .flatMapToDouble(
                                    t -> t.getDurations().parallelStream().mapToDouble(d -> d))
                                .sum())));
    return topicDepartmentMap
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

  private List<ChartDto> convertToLossDescChart(List<TopicEerppEntity> topicList) {
    Map<String, Long> topicDepartmentMap =
        topicList
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getLossCodeDesc))
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    e ->
                        Math.round(
                            e.getValue()
                                .parallelStream()
                                .flatMapToDouble(
                                    t -> t.getDurations().parallelStream().mapToDouble(d -> d))
                                .sum())));
    return topicDepartmentMap
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

  private List<ChartDto> convertToErrorLevelChart(List<TopicEerppEntity> topicHistoryList) {
    Map<EerpErrorLevel, Long> topicErrorLevelMap =
        topicHistoryList
            .stream()
            .collect(Collectors.groupingBy(TopicEerppEntity::getLevel, Collectors.counting()));
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
        .collect(Collectors.toList());
  }

  private List<Double> convertToDurations(String duration) {
    try {
      return Arrays.asList(mapper.readValue(duration, Double[].class));
    } catch (Exception e) {
      e.printStackTrace();
      return emptyList();
    }
  }

  private List<String> convertToLines(String line) {
    try {
      return Arrays.stream(mapper.readValue(line, String[].class)).distinct().collect(toList());
    } catch (Exception e) {
      e.printStackTrace();
      return emptyList();
    }
  }

  private EerpErrorLevel convertToEerpErrorLevel(Map<Long, List<Double>> historyMap) {
    return Optional.ofNullable(
            EerpErrorLevel.fromCount(
                historyMap.values().parallelStream().mapToInt(CollectionUtils::size).sum()))
        .orElseThrow(() -> new IllegalArgumentException(Constants.ERR_INVALID_PARAM));
  }

  private List<String> distinct(List<String> list) {
    return list.stream().distinct().collect(toList());
  }
}
