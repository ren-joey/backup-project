package com.delta.dms.community.service.eerp.dashboard;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.enums.CommunitySpecialType;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.enums.DbLanguage;
import com.delta.dms.community.enums.EerpmSolutionType;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.swagger.model.ChartDto;
import com.delta.dms.community.swagger.model.DashboardDateDto;
import com.delta.dms.community.swagger.model.EerpDashboardDto;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseDashboardService<T, S, U> {

  static final DateTimeFormatter REPORT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  static final int HALF_MONTH = 15;
  static final String EERP_ID_DELIMITER = "之";
  static final String REPORT_NEW_STATUS = "Y";
  static final String REPORT_DEFAULT_STRING = "N/A";

  final EerpConfig eerpConfig;
  final YamlConfig yamlConfig;
  final EerpDao eerpDao;
  private final CommunityService communityService;

  DateTimeFormatter reportDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm");
  DateTimeFormatter fileNameDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
  private Calendar calendar = Calendar.getInstance();

  public DashboardDateDto getEerpDashboardDate(CommunityInfo community) {
    validateEerpCommunity(community);
    return new DashboardDateDto()
        .start(getConclusionReportStartTime())
        .end(truncateToFirstDayOfMonth(Instant.now()).toEpochMilli());
  }

  public EerpDashboardDto getEerpDashboard(CommunityInfo community, long endTime) {
    validateEerpCommunity(community);
    return getDashboard(community.getCommunityId(), endTime).type(community.getSpecialType());
  }

  public T searchEerpDashboardTopic(
      int communityId, long endTime, Map<String, List<Object>> filterMap, int offset, int limit) {
    validateEerpCommunity(communityId);
    Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
    endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
    long startTime =
        getMonthlyReportStartTime(firstDayOfMonth, eerpConfig.getMConclusionReportDuration());
    validateEndDate(eerpConfig.getMConclusionReportStartTime(), firstDayOfMonth.toEpochMilli());
    List<U> allTopicList = getAllTopics(communityId, startTime, endTime, filterMap);
    List<U> topicList = getTopics(communityId, startTime, endTime, filterMap, offset, limit);

    return searchDashboardTopic(
        allTopicList, topicList, communityId, startTime, endTime, filterMap);
  }

  public S searchEerpDashboardDevice(
      int communityId, long endTime, Map<String, List<Object>> filterMap, int offset, int limit) {
    validateEerpCommunity(communityId);
    Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
    endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
    long startTime =
        getMonthlyReportStartTime(firstDayOfMonth, eerpConfig.getMConclusionReportDuration());
    validateEndDate(eerpConfig.getMConclusionReportStartTime(), firstDayOfMonth.toEpochMilli());
    List<U> allTopicList = getAllTopics(communityId, startTime, endTime, filterMap);
    Map<String, U> allTopicHistoryMap = convertToTopicHistoryMap(allTopicList);
    allTopicHistoryMap = filterOtherItems(allTopicHistoryMap, filterMap);
    List<U> allTopicHistoryList = allTopicHistoryMap.values().stream().collect(Collectors.toList());
    List<U> topicHistoryList =
        (NumberUtils.INTEGER_ZERO <= offset && NumberUtils.INTEGER_ZERO <= limit)
            ? allTopicHistoryList.stream().skip(offset).limit(limit).collect(Collectors.toList())
            : allTopicHistoryList;

    return searchDashboardDevice(
        allTopicHistoryList, topicHistoryList, allTopicList, communityId, startTime, endTime);
  }

  protected abstract CommunitySpecialType getType();

  protected abstract T searchDashboardTopic(
      List<U> allTopicList,
      List<U> topicList,
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap);

  protected abstract S searchDashboardDevice(
      List<U> allTopicHistoryList,
      List<U> topicHistoryList,
      List<U> allTopicList,
      int communityId,
      long startTime,
      long endTime);

  protected abstract long getConclusionReportDuration();

  protected abstract long getConclusionReportStartTime();

  protected abstract long getConclusionReportEffectiveDuration();

  public abstract List<U> getAllTopics(int communityId, long startTime, long endTime);

  public abstract List<U> getAllTopics(
      int communityId, long startTime, long endTime, Map<String, List<Object>> filterMap);

  protected abstract List<U> getTopics(
      int communityId,
      long startTime,
      long endTime,
      Map<String, List<Object>> filterMap,
      int offset,
      int limit);

  protected abstract Map<String, U> filterOtherItems(
      Map<String, U> topicHistoryMap, Map<String, List<Object>> filterMap);

  public abstract Map<String, U> convertToTopicHistoryMap(List<U> topics);

  protected abstract Map<String, List<U>> convertToPreviousDeviceIdMap(List<U> topics);

  protected abstract List<LabelValueDto> getEffectiveSolution(
      Map<String, List<U>> previousDeviceIdMap, Map<String, U> topicHistoryMap);

  protected abstract List<ChartDto> convertToConclusionChart(List<U> topics);

  protected abstract List<ChartDto> convertToFactoryChart(List<U> topics);

  protected abstract List<ChartDto> convertToDepartmentChart(List<U> topics);

  protected abstract List<ChartDto> convertToForumChart(List<U> topics);

  protected abstract List<ChartDto> convertToErrorLevelChart(Map<String, U> topicHistoryMap);

  Instant truncateToFirstDayOfMonth(long time) {
    return truncateToFirstDayOfMonth(Instant.ofEpochMilli(time));
  }

  Instant truncateToFirstDayOfMonth(Instant time) {
    return ZonedDateTime.ofInstant(time, ZoneId.systemDefault())
        .truncatedTo(ChronoUnit.DAYS)
        .with(TemporalAdjusters.firstDayOfMonth())
        .toInstant();
  }

  long truncateToHalfMonth(long time) {
    Instant instant = truncateToFirstDayOfMonth(time);
    return isFirstHalfOfMonth(time)
        ? instant.toEpochMilli()
        : instant.plus(HALF_MONTH, ChronoUnit.DAYS).toEpochMilli();
  }

  public long getMonthlyReportStartTime(Instant endTime, long duration) {
    return ZonedDateTime.ofInstant(endTime, ZoneId.systemDefault())
        .minusMonths(duration)
        .toInstant()
        .toEpochMilli();
  }

  void validateEndDate(long startTime, long endTime) {
    if (Instant.now().toEpochMilli() < endTime || startTime > endTime) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  List<ChartDto> convertToConclusionChartDetailDto(
      Map<Integer, String> conclusionStateMap, Map<Integer, Long> conclusionMap) {
    List<ChartDto> result = new ArrayList<>();
    result.add(
        new ChartDto()
            .value(ConclusionState.CONCLUDED.getId())
            .label(conclusionStateMap.get(ConclusionState.CONCLUDED.getId()))
            .total(
                Optional.ofNullable(conclusionMap.get(ConclusionState.CONCLUDED.getId()))
                    .orElseGet(() -> NumberUtils.LONG_ZERO))
            .color(eerpConfig.getEerpChartConclusionColor(ConclusionState.CONCLUDED.getId())));
    result.add(
        new ChartDto()
            .value(ConclusionState.UNCONCLUDED.getId())
            .label(conclusionStateMap.get(ConclusionState.UNCONCLUDED.getId()))
            .total(
                Optional.ofNullable(conclusionMap.get(ConclusionState.UNCONCLUDED.getId()))
                    .orElseGet(() -> NumberUtils.LONG_ZERO))
            .color(eerpConfig.getEerpChartConclusionColor(ConclusionState.UNCONCLUDED.getId())));
    return result;
  }

  List<ChartDto> convertToHistoryChartDto(
      Map<Long, List<Long>> historyMap, long startTime, long endTime) {
    List<ChartDto> result = new ArrayList<>();
    long time = startTime;
    do {
      result.add(
          new ChartDto()
              .value(time)
              .label(Long.toString(time))
              .color(eerpConfig.getChartDefaultColor()));
      time =
          isFirstHalfOfMonth(time)
              ? Instant.ofEpochMilli(time).plus(HALF_MONTH, ChronoUnit.DAYS).toEpochMilli()
              : ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
                  .plus(NumberUtils.INTEGER_ONE, ChronoUnit.MONTHS)
                  .with(TemporalAdjusters.firstDayOfMonth())
                  .toInstant()
                  .toEpochMilli();
    } while (time <= endTime);
    return result
        .stream()
        .map(
            item ->
                item.total(
                    Optional.ofNullable(historyMap.get(item.getValue()))
                        .orElseGet(ArrayList::new)
                        .parallelStream()
                        .mapToLong(Long::valueOf)
                        .sum()))
        .collect(Collectors.toList());
  }

  private void validateEerpCommunity(int communityId) {
    validateEerpCommunity(communityService.getCommunityInfoById(communityId));
  }

  private void validateEerpCommunity(CommunityInfo communityInfo) {
    if (!communityInfo.isDashboard()) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    if (!communityService.checkUserIsMemberOfCommunity(
        Utility.getUserIdFromSession(),
        communityInfo.getCommunityId(),
        communityInfo.getCommunityGroupId())) {
      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
  }

  private EerpDashboardDto getDashboard(int communityId, long endTime) {
    Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
    endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
    long startTime = getMonthlyReportStartTime(firstDayOfMonth, getConclusionReportDuration());
    validateEndDate(getConclusionReportStartTime(), firstDayOfMonth.toEpochMilli());
    List<U> topicList = getAllTopics(communityId, startTime, endTime);
    Map<String, U> topicHistoryMap = convertToTopicHistoryMap(topicList);
    Instant previousEndTime = Instant.ofEpochMilli(startTime);
    Map<String, List<U>> previousDeviceIdMap =
        convertToPreviousDeviceIdMap(
            getAllTopics(
                communityId,
                getMonthlyReportPrevioustartTime(
                    previousEndTime, getConclusionReportEffectiveDuration()),
                previousEndTime.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli()));
    List<LabelValueDto> effectiveSolutionList =
        getEffectiveSolution(previousDeviceIdMap, topicHistoryMap);

    return new EerpDashboardDto()
        .conclusionChart(convertToConclusionChart(topicList))
        .factoryChart(convertToFactoryChart(topicList))
        .departmentChart(convertToDepartmentChart(topicList))
        .forumChart(convertToForumChart(topicList))
        .errorLevelChart(convertToErrorLevelChart(topicHistoryMap))
        .effectiveSolutionChart(
            convertToEffectiveSolutionChart(
                previousDeviceIdMap.keySet().parallelStream().collect(Collectors.toList()),
                effectiveSolutionList
                    .parallelStream()
                    .map(LabelValueDto::getLabel)
                    .collect(Collectors.toList())))
        .effectiveSolutions(effectiveSolutionList);
  }

  private long getMonthlyReportPrevioustartTime(Instant endTime, long duration) {
    return ZonedDateTime.ofInstant(endTime, ZoneId.systemDefault())
        .minusMonths(duration)
        .toInstant()
        .toEpochMilli();
  }

  private boolean isFirstHalfOfMonth(long time) {
    calendar.setTimeInMillis(time);
    return (calendar.get(Calendar.DAY_OF_MONTH) <= HALF_MONTH);
  }

  private List<ChartDto> convertToEffectiveSolutionChart(
      List<String> totalDeviceIdList, List<String> effectiveSolutionList) {
    if (CollectionUtils.isEmpty(totalDeviceIdList)) {
      return Collections.emptyList();
    }
    DbLanguage lang =
        Optional.ofNullable(DbLanguage.fromValue(AcceptLanguage.getLanguageForDb()))
            .orElseGet(() -> DbLanguage.ZHTW);
    List<ChartDto> result = new ArrayList<>();
    if (NumberUtils.INTEGER_ZERO < effectiveSolutionList.size()) {
      result.add(
          new ChartDto()
              .value(EerpmSolutionType.EFFECTIVE_SOLUTION.getId())
              .label(EerpmSolutionType.EFFECTIVE_SOLUTION.getValue(lang))
              .total(Long.valueOf(effectiveSolutionList.size()))
              .color(
                  eerpConfig.getEerpChartEffectiveSolutionColor(
                      EerpmSolutionType.EFFECTIVE_SOLUTION.getId())));
    }
    Optional.of(Math.subtractExact(totalDeviceIdList.size(), effectiveSolutionList.size()))
        .filter(size -> INTEGER_ZERO < size)
        .map(Long::valueOf)
        .ifPresent(
            size ->
                result.add(
                    new ChartDto()
                        .value(EerpmSolutionType.INEFFECTIVE_SOLUTION.getId())
                        .label(EerpmSolutionType.INEFFECTIVE_SOLUTION.getValue(lang))
                        .total(size)
                        .color(
                            eerpConfig.getEerpChartEffectiveSolutionColor(
                                EerpmSolutionType.INEFFECTIVE_SOLUTION.getId()))));
    return result;
  }
}
