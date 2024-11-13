package com.delta.dms.community.service.eerp.report;

import static com.delta.dms.community.swagger.model.EerpType.M;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.adapter.MyDmsAdapter;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.dao.entity.EerpNewCauseSolution;
import com.delta.dms.community.dao.entity.EerpOriginCauseSolution;
import com.delta.dms.community.dao.entity.EerpmGeneralConclusionBean;
import com.delta.dms.community.dao.entity.TopicEerpmEntity;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.enums.ExcelEerpmHeaderHistory;
import com.delta.dms.community.enums.ExcelEerpmHeaderRaw;
import com.delta.dms.community.enums.ExcelEerpmTitle;
import com.delta.dms.community.model.CustomByteArrayResource;
import com.delta.dms.community.model.ExcelHeaderDetail;
import com.delta.dms.community.model.eerpm.EerpmErrorRawData;
import com.delta.dms.community.service.eerp.dashboard.EerpmDashboardService;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.swagger.model.GeneralStatus;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.ExcelUtility;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerpmReportService extends BaseEerpReportService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String EERPM_SHEET_NAME_RAW = "總表";
  private static final String EERPM_SHEET_NAME_HISTORY = "設備錯誤碼出現記錄";

  private final MyDmsAdapter myDmsAdapter;
  private final EerpmDashboardService dashboardService;

  private DateTimeFormatter eerpmDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public EerpmReportService(
      EerpConfig eerpConfig,
      YamlConfig yamlConfig,
      EerpDao eerpDao,
      MyDmsConfig myDmsConfig,
      MailDao mailDao,
      MyDmsAdapter myDmsAdapter,
      EerpmDashboardService dashboardService) {
    super(eerpConfig, yamlConfig, eerpDao, myDmsConfig, mailDao);
    this.myDmsAdapter = myDmsAdapter;
    this.dashboardService = dashboardService;
  }

  @Override
  protected EerpType getType() {
    return M;
  }

  @Override
  protected void uploadConclusionReportToMyDms(long startTime, long endTime) {
    if (NumberUtils.LONG_MINUS_ONE == startTime) {
      Instant firstDayOfMonth = truncateToFirstDayOfMonth(endTime);
      endTime = firstDayOfMonth.minus(NumberUtils.INTEGER_ONE, ChronoUnit.SECONDS).toEpochMilli();
      startTime =
          getMonthlyReportStartTime(firstDayOfMonth, eerpConfig.getMConclusionReportDuration());
    }
    String reportName =
        eerpConfig.getConclusionReportName(
            EerpType.M,
            convertToDateTime(fileNameDateFormat, startTime),
            convertToDateTime(fileNameDateFormat, endTime));
    final String processStartTime = REPORT_DATE_FORMAT.format(LocalDateTime.now());
    GeneralStatus result = GeneralStatus.SUCCESS;
    String resultMsg = StringUtils.EMPTY;
    try {
      ByteArrayResource resource =
          new CustomByteArrayResource(reportName, getEerpmReport(startTime, endTime));
      Map<String, List<Object>> tagMap = convertToMyDmsFileTagMap(reportName);
      resultMsg =
          myDmsAdapter.uploadFile(
              resource,
              eerpConfig.getMConclusionReportGid(),
              eerpConfig.getMConclusionReportFolderId(),
              mapper.writeValueAsString(tagMap));
    } catch (HttpClientErrorException e) {
      result = GeneralStatus.FAIL;
      resultMsg =
          new StringBuilder(Integer.toString(e.getStatusCode().value()))
              .append(StringUtils.SPACE)
              .append(e.getStatusText())
              .append(Constants.COLON)
              .append(e.getResponseBodyAsString())
              .toString();
    } catch (Exception e) {
      result = GeneralStatus.FAIL;
      resultMsg = e.getMessage();
    }
    final String processEndTime = REPORT_DATE_FORMAT.format(LocalDateTime.now());
    insertReportLogAndMail(
        processStartTime, processEndTime, EerpType.M, result, resultMsg, startTime, endTime);
  }

  private byte[] getEerpmReport(long startTime, long endTime) throws IOException {
    List<TopicEerpmEntity> topicList =
        dashboardService.getAllTopics(NumberUtils.INTEGER_ZERO, startTime, endTime);
    Map<String, TopicEerpmEntity> topicHistoryMap =
        dashboardService.convertToTopicHistoryMap(topicList);

    Workbook workbook = new XSSFWorkbook();
    ExcelUtility.writeToExcel(
        workbook,
        EERPM_SHEET_NAME_RAW,
        getExcelHeaderOfEerpmRaw(),
        topicList
            .stream()
            .map(
                item -> {
                  try {
                    return convertToEerpmRawSheetDetail(item, topicHistoryMap);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList()), 0);
    ExcelUtility.writeToExcel(
        workbook,
        EERPM_SHEET_NAME_HISTORY,
        getExcelHeaderOfEerpmHistory(truncateToHalfMonth(startTime), truncateToHalfMonth(endTime)),
        topicHistoryMap
            .values()
            .stream()
            .map(this::convertToEerpmHistorySheetDetail)
            .collect(Collectors.toList()), 0);
    return ExcelUtility.convertToByteAndClose(workbook);
  }

  private List<ExcelHeaderDetail> getExcelHeaderOfEerpmRaw() {
    List<ExcelHeaderDetail> result = new ArrayList<>();
    for (ExcelEerpmHeaderRaw header : ExcelEerpmHeaderRaw.values()) {
      result.add(
          new ExcelHeaderDetail()
              .setKey(header.getKey())
              .setValue(header.getHeader())
              .setWidth(header.getWidth()));
    }
    return result;
  }

  private List<ExcelHeaderDetail> getExcelHeaderOfEerpmHistory(long startTime, long endTime) {
    List<ExcelHeaderDetail> result = new ArrayList<>();
    List<ExcelEerpmHeaderHistory> defaultHeaderLst = new ArrayList<>();
    defaultHeaderLst.add(ExcelEerpmHeaderHistory.ID);
    defaultHeaderLst.add(ExcelEerpmHeaderHistory.FACTORY);
    defaultHeaderLst.add(ExcelEerpmHeaderHistory.FORUM_NAME);
    defaultHeaderLst.add(ExcelEerpmHeaderHistory.DEVICE_MODEL);
    defaultHeaderLst.add(ExcelEerpmHeaderHistory.ERROR_CODE);
    defaultHeaderLst.forEach(item -> result.add(convertToExcelHeaderDetail(item)));
    do {
      String header = convertToDateTime(eerpmDateFormat, startTime);
      result.add(
          new ExcelHeaderDetail()
              .setKey(header)
              .setValue(header)
              .setWidth(ExcelEerpmHeaderHistory.COUNT.getWidth()));
      startTime =
          isFirstHalfOfMonth(startTime)
              ? Instant.ofEpochMilli(startTime).plus(HALF_MONTH, ChronoUnit.DAYS).toEpochMilli()
              : ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                  .plus(NumberUtils.INTEGER_ONE, ChronoUnit.MONTHS)
                  .with(TemporalAdjusters.firstDayOfMonth())
                  .toInstant()
                  .toEpochMilli();
    } while (startTime <= endTime);
    result.add(convertToExcelHeaderDetail(ExcelEerpmHeaderHistory.LEVEL));
    return result;
  }

  private Map<String, String> convertToEerpmRawSheetDetail(
      TopicEerpmEntity data, Map<String, TopicEerpmEntity> topicMap) throws IOException {
    Map<String, String> result = new HashMap<>();
    String[] worstDevices = mapper.readValue(data.getWorstDeviceId(), String[].class);
    EerpmErrorRawData[] detail = mapper.readValue(data.getDetail(), EerpmErrorRawData[].class);
    result.put(ExcelEerpmHeaderRaw.FACTORY.toString(), data.getFactory());
    result.put(ExcelEerpmHeaderRaw.FORUM_NAME.toString(), data.getForumName());
    result.put(ExcelEerpmHeaderRaw.DEVICE_MODEL.toString(), data.getDeviceModel());
    result.put(
        ExcelEerpmHeaderRaw.WORST_DEVICE.toString(),
        String.join(Constants.LINE_BREAKS, worstDevices));
    result.put(ExcelEerpmHeaderRaw.ERROR_CODE.toString(), data.getErrorCode());
    result.put(ExcelEerpmHeaderRaw.ID.toString(), data.getId());
    result.put(ExcelEerpmHeaderRaw.ERROR_COUNT.toString(), Long.toString(data.getErrorCount()));
    result.put(
        ExcelEerpmHeaderRaw.CREATE_TOPIC_TIME.toString(),
        convertToDateTime(reportDateFormat, data.getTopicCreateTime()));
    if (ConclusionState.isConcluded(data.getConclusionStateId())) {
      result.put(ExcelEerpmHeaderRaw.CONCLUSION_STATE.toString(), REPORT_NEW_STATUS);
      result.put(
          ExcelEerpmHeaderRaw.CREATE_CONCLUSION_TIME.toString(),
          convertToDateTime(reportDateFormat, data.getConclusionCreateTime()));
    }
    result.put(
        ExcelEerpmHeaderRaw.ACTION_COUNT.toString(),
        Integer.toString(detail[NumberUtils.INTEGER_ZERO].getActioncount()));
    result.put(
        ExcelEerpmHeaderRaw.LEVEL.toString(),
        topicMap.getOrDefault(data.getId(), data).getLevel().toString());
    result.put(ExcelEerpmHeaderRaw.TOPIC_TITLE.toString(), data.getTopicTitle());
    result.put(ExcelEerpmHeaderRaw.ERROR_DESC.toString(), defaultReportString(data.getErrorDesc()));

    EerpmGeneralConclusionBean text = convertToEerpmGeneralConclusionBean(data.getConclusion());
    if (NumberUtils.INTEGER_ZERO
        == text.getOriginCauseSolution().size() + text.getNewCauseSolution().size()) {
      result.put(ExcelEerpmHeaderRaw.CAUSE.toString(), REPORT_DEFAULT_STRING);
    } else {
      StringBuilder cause = new StringBuilder();
      int causeIndex = NumberUtils.INTEGER_ONE;
      for (EerpOriginCauseSolution item : text.getOriginCauseSolution()) {
        cause.append(
            String.format(
                "%s%s%n%s%s%n%s%s%n",
                String.format(ExcelEerpmTitle.CAUSE.toString(), causeIndex++),
                defaultReportString(item.getCauseDesc()),
                ExcelEerpmTitle.OLD_SOLUTION.toString(),
                defaultReportString(item.getOriginSolution()),
                ExcelEerpmTitle.NEW_SOLUTION.toString(),
                defaultReportString(item.getNewSolution())));
      }
      for (EerpNewCauseSolution item : text.getNewCauseSolution()) {
        cause.append(
            String.format(
                "%s%s%n%s%s%n",
                String.format(ExcelEerpmTitle.CAUSE.toString(), causeIndex++),
                defaultReportString(item.getCauseDesc()),
                ExcelEerpmTitle.NEW_SOLUTION.toString(),
                defaultReportString(item.getSolution())));
      }
      result.put(ExcelEerpmHeaderRaw.CAUSE.toString(), cause.toString());
    }
    String ecn =
        NumberUtils.INTEGER_ZERO == text.getEcn().size()
            ? REPORT_DEFAULT_STRING
            : text.getEcn().stream().collect(Collectors.joining(Constants.LINE_BREAKS));
    result.put(ExcelEerpmHeaderRaw.ECN.toString(), ecn);
    String pcn =
        NumberUtils.INTEGER_ZERO == text.getPcn().size()
            ? REPORT_DEFAULT_STRING
            : text.getPcn().stream().collect(Collectors.joining(Constants.LINE_BREAKS));
    result.put(ExcelEerpmHeaderRaw.PCN.toString(), pcn);
    result.put(ExcelEerpmHeaderRaw.DFAUTO.toString(), defaultReportString(text.getDfauto()));
    result.put(
        ExcelEerpmHeaderRaw.AMBU.toString(),
        String.format(
            "%s%s%n%s%s",
            ExcelEerpmTitle.AMBU_SOFTWARE,
            defaultReportString(text.getAmbu().getSoftware()),
            ExcelEerpmTitle.AMBU_MECHANISM,
            defaultReportString(text.getAmbu().getMechanism())));
    return result;
  }

  private Map<String, String> convertToEerpmHistorySheetDetail(TopicEerpmEntity data) {
    Map<String, String> result = new HashMap<>();
    result.put(ExcelEerpmHeaderHistory.ID.toString(), data.getId());
    result.put(ExcelEerpmHeaderHistory.FACTORY.toString(), data.getFactory());
    result.put(ExcelEerpmHeaderHistory.FORUM_NAME.toString(), data.getForumName());
    result.put(ExcelEerpmHeaderHistory.DEVICE_MODEL.toString(), data.getDeviceModel());
    result.put(ExcelEerpmHeaderHistory.ERROR_CODE.toString(), data.getErrorCode());
    result.put(ExcelEerpmHeaderHistory.LEVEL.toString(), data.getLevel().toString());
    data.getHistory()
        .entrySet()
        .forEach(
            entry ->
                result.put(
                    convertToDateTime(eerpmDateFormat, entry.getKey()),
                    Long.toString(
                        entry.getValue().parallelStream().mapToLong(Long::longValue).sum())));
    return result;
  }

  private EerpmGeneralConclusionBean convertToEerpmGeneralConclusionBean(String data)
      throws IOException {
    if (StringUtils.isEmpty(data)) {
      return new EerpmGeneralConclusionBean();
    }
    EerpmGeneralConclusionBean result = mapper.readValue(data, EerpmGeneralConclusionBean.class);
    result.setOriginCauseSolution(
        Optional.ofNullable(result.getOriginCauseSolution()).orElseGet(ArrayList::new));
    result.setNewCauseSolution(
        Optional.ofNullable(result.getNewCauseSolution()).orElseGet(ArrayList::new));
    result.setEcn(
        Optional.ofNullable(result.getEcn())
            .orElseGet(ArrayList::new)
            .stream()
            .filter(item -> StringUtils.isNotEmpty(StringUtils.trim(item)))
            .collect(Collectors.toList()));
    result.setPcn(
        Optional.ofNullable(result.getPcn())
            .orElseGet(ArrayList::new)
            .stream()
            .filter(item -> StringUtils.isNotEmpty(StringUtils.trim(item)))
            .collect(Collectors.toList()));
    return result;
  }
}
