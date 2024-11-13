package com.delta.dms.community.service.eerp.report;

import static java.util.Collections.singletonList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.enums.DbLanguage;
import com.delta.dms.community.enums.EerpmSolutionType;
import com.delta.dms.community.enums.ExcelEerpmHeaderHistory;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.ExcelHeaderDetail;
import com.delta.dms.community.swagger.model.ChartDto;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.swagger.model.GeneralStatus;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ConclusionReportUtils {

  static final DateTimeFormatter REPORT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  static final int HALF_MONTH = 15;
  static final String EERP_ID_DELIMITER = "之";
  static final String REPORT_NEW_STATUS = "Y";
  static final String REPORT_DEFAULT_STRING = "N/A";
  private static final String LOG_SUBJECT_FORMAT = "[DMS] Eerp conclusion report log [%s]";
  private static final String LOG_CONTENT_FORMAT_SUCCEED =
      "<p>[Succeeded]</p><p>process period: %s~%s</p><p>created document: %s</p>";
  private static final String LOG_CONTENT_FORMAT_FAIL =
      "<p>[Failed]</p><p>process period: %s~%s</p><p>error message: %s</p>";
  private static final int LOG_PRIORITY = 3;

  final EerpConfig eerpConfig;
  final YamlConfig yamlConfig;
  final EerpDao eerpDao;
  private final MyDmsConfig myDmsConfig;
  private final MailDao mailDao;

  DateTimeFormatter reportDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm");
  DateTimeFormatter fileNameDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
  private Calendar calendar = Calendar.getInstance();

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

  boolean isFirstHalfOfMonth(long time) {
    calendar.setTimeInMillis(time);
    return (calendar.get(Calendar.DAY_OF_MONTH) <= HALF_MONTH);
  }

  long getMonthlyReportStartTime(Instant endTime, long duration) {
    return ZonedDateTime.ofInstant(endTime, ZoneId.systemDefault())
        .minusMonths(duration)
        .toInstant()
        .toEpochMilli();
  }

  long getMonthlyReportPrevioustartTime(Instant endTime, long duration) {
    return ZonedDateTime.ofInstant(endTime, ZoneId.systemDefault())
        .minusMonths(duration)
        .toInstant()
        .toEpochMilli();
  }

  void insertReportLogAndMail(
      String startTime,
      String endTime,
      EerpType type,
      GeneralStatus result,
      String msg,
      long reportStartTime,
      long reportEndTime) {
    String resultMsg = StringUtils.defaultString(msg);
    eerpDao.insertEerpReportLog(
        startTime, endTime, type, result, resultMsg, reportStartTime, reportEndTime);
    insertMail(startTime, endTime, result, resultMsg);
  }

  void insertMail(String startTime, String endTime, GeneralStatus status, String message) {
    mailDao.insertMail(
        Utility.getUserIdFromSession(),
        Constants.DEFAULT_LOG_SENDER,
        eerpDao
            .getEerpReportLogRecipient()
            .stream()
            .collect(Collectors.joining(Constants.COMMA_DELIMITER)),
        String.format(LOG_SUBJECT_FORMAT, yamlConfig.getEnvIdentity()),
        String.format(
            GeneralStatus.SUCCESS == status ? LOG_CONTENT_FORMAT_SUCCEED : LOG_CONTENT_FORMAT_FAIL,
            startTime,
            endTime,
            message),
        LOG_PRIORITY);
  }

  void validateEndDate(long startTime, long endTime) {
    if (Instant.now().toEpochMilli() < endTime || startTime > endTime) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  String defaultReportString(String data) {
    return StringUtils.defaultIfEmpty(data, REPORT_DEFAULT_STRING);
  }

  String convertToDateTime(DateTimeFormatter dateTimeFormatter, long time) {
    ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
    return dateTime.format(dateTimeFormatter);
  }

  Map<String, List<Object>> convertToMyDmsFileTagMap(String fileName) {
    return convertToMyDmsFileTagMap(
        fileName,
        Utility.getUserIdFromSession(),
        Utility.getUserFromSession().getCommonName(),
        singletonList(eerpConfig.getDefaultAppFieldId()));
  }

  public Map<String, List<Object>> convertToMyDmsFileTagMap(
      String fileName, String authorId, String authorName, List<String> appFields) {
    Map<String, List<Object>> tagMap = new HashMap<>();
    tagMap.put(
        myDmsConfig.getTagAuthor(),
        singletonList(new LabelValueDto().label(authorName).value(authorId)));
    tagMap.put(myDmsConfig.getTagTitle(), singletonList(FilenameUtils.getBaseName(fileName)));
    tagMap.put(myDmsConfig.getTagAppField(), new ArrayList<>(appFields));

    return tagMap;
  }

  public void putRecordTypeTag(Map<String, List<Object>> tagMap, String recordTypeId) {
    tagMap.put(myDmsConfig.getTagRecordType(), singletonList(recordTypeId));
  }

  ExcelHeaderDetail convertToExcelHeaderDetail(ExcelEerpmHeaderHistory header) {
    return new ExcelHeaderDetail()
        .setKey(header.getKey())
        .setValue(header.getHeader())
        .setWidth(header.getWidth());
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

  List<ChartDto> convertToEffectiveSolutionChart(
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
    result.add(
        new ChartDto()
            .value(EerpmSolutionType.INEFFECTIVE_SOLUTION.getId())
            .label(EerpmSolutionType.INEFFECTIVE_SOLUTION.getValue(lang))
            .total(
                Long.valueOf(
                    Math.subtractExact(totalDeviceIdList.size(), effectiveSolutionList.size())))
            .color(
                eerpConfig.getEerpChartEffectiveSolutionColor(
                    EerpmSolutionType.INEFFECTIVE_SOLUTION.getId())));
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
}
