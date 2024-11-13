package com.delta.dms.community.service.eerp.report;

import static com.delta.dms.community.swagger.model.EerpType.P;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.adapter.MyDmsAdapter;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.CommunityDao;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.EerppGeneralConclusionBean;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.enums.EerppLanguage;
import com.delta.dms.community.enums.ExcelEerppHeader;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.CustomByteArrayResource;
import com.delta.dms.community.model.ExcelHeaderDetail;
import com.delta.dms.community.model.eerpp.EerppSolution;
import com.delta.dms.community.model.eerpp.EerppTopicRawData;
import com.delta.dms.community.service.EventPublishService;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.swagger.model.EmailType;
import com.delta.dms.community.swagger.model.EmailWithChineseAndEnglishContext;
import com.delta.dms.community.swagger.model.GeneralStatus;
import com.delta.dms.community.swagger.model.TemplateType;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EmailConstants;
import com.delta.dms.community.utils.ExcelUtility;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerppReportService extends BaseEerpReportService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final MyDmsAdapter myDmsAdapter;
  private final UserService userService;
  private final EventPublishService eventPublishService;
  private final TopicDao topicDao;
  private final CommunityDao communityDao;

  public EerppReportService(
      EerpConfig eerpConfig,
      YamlConfig yamlConfig,
      EerpDao eerpDao,
      MyDmsConfig myDmsConfig,
      MailDao mailDao,
      MyDmsAdapter myDmsAdapter,
      UserService userService,
      EventPublishService eventPublishService,
      TopicDao topicDao,
      CommunityDao communityDao) {
    super(eerpConfig, yamlConfig, eerpDao, myDmsConfig, mailDao);
    this.myDmsAdapter = myDmsAdapter;
    this.userService = userService;
    this.eventPublishService = eventPublishService;
    this.topicDao = topicDao;
    this.communityDao = communityDao;
  }

  @Override
  protected EerpType getType() {
    return P;
  }

  @Override
  protected void uploadConclusionReportToMyDms(long startTime, long endTime) {
    if (NumberUtils.LONG_MINUS_ONE == startTime) {
      startTime = eerpDao.getLastReportTime(EerpType.P);
    }
    String reportName =
        eerpConfig.getConclusionReportName(
            EerpType.P,
            convertToDateTime(fileNameDateFormat, startTime),
            convertToDateTime(fileNameDateFormat, endTime));
    generateEerppConclusionReportToMyDms(startTime, endTime, reportName);
  }

  private void generateEerppConclusionReportToMyDms(
      long startTime, long endTime, String reportName) {
    Map<String, List<TopicInfo>> topicMap = getEerppConcludedTopic(startTime, endTime);
    topicMap
        .entrySet()
        .forEach(
            item -> {
              final String processStartTime = REPORT_DATE_FORMAT.format(LocalDateTime.now());
              GeneralStatus result = GeneralStatus.SUCCESS;
              String resultMsg = StringUtils.EMPTY;
              try {
                int mydmsFolderId = eerpConfig.getEerppConclusionReportPath(item.getKey());
                ByteArrayResource resource =
                    new CustomByteArrayResource(
                        reportName, getEerppConclusionReport(item.getValue()));
                Map<String, List<Object>> tagMap = convertToMyDmsFileTagMap(reportName);
                resultMsg =
                    myDmsAdapter.uploadFile(
                        resource, item.getKey(), mydmsFolderId, mapper.writeValueAsString(tagMap));
                sendEerppReviewReportMail(
                    item.getKey(),
                    mydmsFolderId,
                    reportName,
                    eerpConfig.getEerppReportContactMail(item.getKey()));
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
                  processStartTime,
                  processEndTime,
                  EerpType.P,
                  result,
                  resultMsg,
                  startTime,
                  endTime);
            });

    final String now = REPORT_DATE_FORMAT.format(LocalDateTime.now());
    eerpConfig
        .getPConclusionReportPath()
        .entrySet()
        .stream()
        .filter(entry -> Objects.isNull(topicMap.get(entry.getKey())))
        .forEach(
            entry ->
                insertReportLogAndMail(
                    now,
                    now,
                    EerpType.P,
                    GeneralStatus.SUCCESS,
                    StringUtils.EMPTY,
                    startTime,
                    endTime));
  }

  private Map<String, List<TopicInfo>> getEerppConcludedTopic(long startTime, long endTime) {
    Map<Integer, List<TopicInfo>> topicMap =
        topicDao
            .getConcludedTopicByTopicType(
                Arrays.asList(TopicType.EERPPGENERAL.toString()), startTime, endTime)
            .stream()
            .collect(Collectors.groupingBy(TopicInfo::getCommunityId));
    if (topicMap.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Integer, String> communityMap =
        communityDao
            .getCommunityById(topicMap.keySet().stream().collect(Collectors.toList()))
            .stream()
            .collect(
                Collectors.toMap(
                    CommunityInfo::getCommunityId, CommunityInfo::getCommunityGroupId));
    Map<String, List<TopicInfo>> result =
        topicMap
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> communityMap.get(e.getKey()), Map.Entry::getValue));
    eerpConfig
        .getPConclusionReportPath()
        .entrySet()
        .stream()
        .filter(
            e ->
                communityMap
                    .entrySet()
                    .parallelStream()
                    .noneMatch(item -> StringUtils.equals(item.getValue(), e.getKey())))
        .forEach(e -> result.put(e.getKey(), Collections.emptyList()));
    return result;
  }

  private void sendEerppReviewReportMail(
      String gid, int folderId, String fileName, List<String> recipient) {
    String link =
        String.format(
            EmailConstants.EERPP_REVIEW_REPORT_URI_FORMAT,
            yamlConfig.getHost(),
            AcceptLanguage.get(),
            gid,
            folderId);
    String content =
        new StringBuilder(
                String.format(
                    EmailConstants.EERPP_REVIEW_REPORT_NOTIFICATION_ENGLISH_FORMAT, fileName))
            .append(Constants.LINE_BREAKS)
            .append(
                String.format(
                    EmailConstants.EERPP_REVIEW_REPORT_NOTIFICATION_CHINESE_FORMAT, fileName))
            .toString();
    EmailWithChineseAndEnglishContext context =
        new EmailWithChineseAndEnglishContext()
            .type(EmailType.CONCLUSIONMADE)
            .sender(Utility.getUserFromSession().getCommonName())
            .desc(EmailConstants.EERPP_REVIEW_REPORT_DESC)
            .subject(String.format(EmailConstants.EERPP_REVIEW_REPORT_SUBJECT_FORMAT, fileName))
            .content(Utility.getTextFromHtml(content))
            .to(userService.getEmailByUserId(recipient))
            .link(link);
    eventPublishService.publishEmailSendingEvent(context, TemplateType.EERPMAIL);
  }

  private byte[] getEerppConclusionReport(List<TopicInfo> topicList) throws IOException {
    List<ExcelHeaderDetail> headerList = new ArrayList<>();
    for (ExcelEerppHeader header : ExcelEerppHeader.values()) {
      headerList.add(
          new ExcelHeaderDetail()
              .setKey(header.getKey())
              .setValue(header.getHeader())
              .setWidth(header.getWidth()));
    }
    return ExcelUtility.convertToExcel(
        headerList,
        topicList
            .stream()
            .map(
                item -> {
                  try {
                    return convertToEerppConclusionReportDetail(item);
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                  return null;
                })
            .flatMap(List::stream)
            .filter(item -> !Objects.isNull(item))
            .collect(Collectors.toList()));
  }

  private List<Map<String, String>> convertToEerppConclusionReportDetail(TopicInfo data)
      throws IOException {
    List<Map<String, String>> result = new ArrayList<>();
    EerppTopicRawData topicText = mapper.readValue(data.getTopicText(), EerppTopicRawData.class);
    EerppGeneralConclusionBean conclusionText =
        mapper.readValue(data.getConclusion(), EerppGeneralConclusionBean.class);
    final Map<String, String> baseReportDetail =
        getEerppBaseReportDetail(topicText, conclusionText);
    for (EerppSolution solution : conclusionText.getOriginSolution()) {
      Map<String, String> detail = new HashMap<>(baseReportDetail);
      detail.put(ExcelEerppHeader.STATUS.toString(), StringUtils.EMPTY);
      detail.put(ExcelEerppHeader.SOLUTION_CODE.toString(), solution.getSolutionCode());
      detail.put(
          ExcelEerppHeader.SOLUTION_DESC_ZH.toString(),
          StringUtils.defaultString(
              solution.getSolutionCodeDesc().get(EerppLanguage.ZH_TW.toString())));
      detail.put(
          ExcelEerppHeader.SOLUTION_DESC_EN.toString(),
          StringUtils.defaultString(
              solution.getSolutionCodeDesc().get(EerppLanguage.EN_US.toString())));
      result.add(detail);
    }
    for (EerppSolution solution : conclusionText.getNewSolution()) {
      Map<String, String> detail = new HashMap<>(baseReportDetail);
      detail.put(ExcelEerppHeader.STATUS.toString(), REPORT_NEW_STATUS);
      detail.put(ExcelEerppHeader.SOLUTION_CODE.toString(), StringUtils.EMPTY);
      detail.put(
          ExcelEerppHeader.SOLUTION_DESC_ZH.toString(),
          StringUtils.defaultString(
              solution.getSolutionCodeDesc().get(EerppLanguage.ZH_TW.toString())));
      detail.put(
          ExcelEerppHeader.SOLUTION_DESC_EN.toString(),
          StringUtils.defaultString(
              solution.getSolutionCodeDesc().get(EerppLanguage.EN_US.toString())));
      result.add(detail);
    }
    return result;
  }

  private Map<String, String> getEerppBaseReportDetail(
      EerppTopicRawData topicText, EerppGeneralConclusionBean conclusionText) {
    Map<String, String> result = new HashMap<>();
    result.put(ExcelEerppHeader.AREA.toString(), topicText.getArea());
    result.put(ExcelEerppHeader.LOSS_CODE.toString(), topicText.getLossCode());
    result.put(
        ExcelEerppHeader.LOSS_DESC_ZH.toString(),
        StringUtils.defaultString(
            conclusionText.getLossCodeDesc().get(EerppLanguage.ZH_TW.toString())));
    result.put(
        ExcelEerppHeader.LOSS_DESC_EN.toString(),
        StringUtils.defaultString(
            conclusionText.getLossCodeDesc().get(EerppLanguage.EN_US.toString())));
    result.put(ExcelEerppHeader.TYPE_CODE.toString(), topicText.getTypeCode());
    result.put(ExcelEerppHeader.DELAY_TYPE.toString(), topicText.getDelayType());
    result.put(ExcelEerppHeader.REASON_CODE.toString(), topicText.getReasonCode());
    result.put(
        ExcelEerppHeader.REASON_DESC_ZH.toString(),
        StringUtils.defaultString(
            conclusionText.getReasonCodeDesc().get(EerppLanguage.ZH_TW.toString())));
    result.put(
        ExcelEerppHeader.REASON_DESC_EN.toString(),
        StringUtils.defaultString(
            conclusionText.getReasonCodeDesc().get(EerppLanguage.EN_US.toString())));
    return result;
  }
}
