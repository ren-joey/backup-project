package com.delta.dms.community.service.eerp.topic;

import static com.delta.dms.community.swagger.model.TopicType.EERPMGENERAL;
import static com.delta.dms.community.swagger.model.TopicType.EERPMHIGHLEVEL;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.entity.EerpNewCauseSolution;
import com.delta.dms.community.dao.entity.EerpOriginCauseSolution;
import com.delta.dms.community.dao.entity.EerpmGeneralConclusionBean;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.dao.entity.TopicEerpmEntity;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.model.eerpm.EerpmDeviceRawData;
import com.delta.dms.community.model.eerpm.EerpmTopicHistory;
import com.delta.dms.community.model.eerpm.EerpmTopicRawData;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.ReplyService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.service.eerp.dashboard.EerpmDashboardService;
import com.delta.dms.community.swagger.model.EerpTopicCreationData;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.TopicCreationData;
import com.delta.dms.community.swagger.model.TopicType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerpmGeneralService extends BaseEerpTopicService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final int HIGH_LEVEL_SIZE = 6;
  private static final String HIGH_LEVEL_TITLE_SUFFIX = " (高發問題)";

  private EerpmDashboardService eerpmDashboardService;
  private ForumService forumService;
  private ReplyService replyService;

  public EerpmGeneralService(
      TopicService topicService,
      EerpConfig eerpConfig,
      EerpmDashboardService eerpmDashboardService,
      ForumService forumService,
      ReplyService replyService) {
    super(eerpConfig, topicService);
    this.eerpmDashboardService = eerpmDashboardService;
    this.forumService = forumService;
    this.replyService = replyService;
  }

  @Override
  protected TopicType getType() {
    return EERPMGENERAL;
  }

  @Override
  protected void updateConclusionText(TopicInfo topic) throws IOException {
    ReplyInfo origin = replyService.getConclusionByTopicId(topic.getTopicId());
    if (Objects.isNull(origin)) {
      return;
    }
    EerpmDeviceRawData deviceRawData =
        mapper
            .readValue(topic.getTopicText(), EerpmTopicRawData.class)
            .getDeviceDatas()
            .get(NumberUtils.INTEGER_ZERO);
    EerpmGeneralConclusionBean conclusionText =
        mapper.readValue(origin.getReplyConclusionText(), EerpmGeneralConclusionBean.class);
    conclusionText
        .setDeviceModel(deviceRawData.getDeviceModel())
        .setErrorCode(deviceRawData.getErrorCode());
    origin.setReplyConclusionText(
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conclusionText));
    replyService.updateText(origin);
  }

  @Override
  protected TopicCreationData convertToTopicCreationData(EerpTopicCreationData data) {
    ForumInfo forum = forumService.getForumInfoById(data.getForumId());
    TopicCreationData topic =
        new TopicCreationData()
            .forumId(data.getForumId())
            .title(data.getTitle())
            .tag(data.getTag())
            .attachment(Collections.emptyList())
            .notificationType(null)
            .recipient(Collections.emptyList())
            .appField(
                Collections.singletonList(
                    new LabelValueDto().value(eerpConfig.getDefaultAppFieldId())));
    try {
      EerpmTopicRawData content = mapper.readValue(data.getText(), EerpmTopicRawData.class);
      EerpmDeviceRawData device = content.getDeviceDatas().get(INTEGER_ZERO);
      Instant endTime = Instant.now();
      long startTime =
          eerpmDashboardService.getMonthlyReportStartTime(
              endTime, eerpConfig.getMConclusionReportDuration());
      List<TopicEerpmEntity> topics =
          eerpmDashboardService.getAllTopics(
              forum.getCommunityId(),
              startTime,
              endTime.toEpochMilli(),
              eerpmDashboardService.convertToFilterMap(
                  device.getFactory(),
                  forum.getForumName(),
                  device.getDeviceModel(),
                  device.getErrorCode()));
      TopicType topicType = getTopicType(topics);
      topic
          .type(topicType)
          .title(generateTitle(topicType, topic.getTitle()))
          .text(generateText(topicType, content, topics));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return topic;
  }

  private TopicType getTopicType(List<TopicEerpmEntity> topics) {
    if (topics.size() >= HIGH_LEVEL_SIZE) {
      return EERPMHIGHLEVEL;
    }
    return EERPMGENERAL;
  }

  private String generateTitle(TopicType type, String title) {
    return EERPMHIGHLEVEL == type ? title.concat(HIGH_LEVEL_TITLE_SUFFIX) : title;
  }

  private String generateText(
      TopicType topicType, EerpmTopicRawData content, List<TopicEerpmEntity> topics)
      throws IOException {
    if (EERPMHIGHLEVEL == topicType) {
      content.setHistories(
          topics
              .stream()
              .map(
                  t -> {
                    EerpmTopicHistory history =
                        new EerpmTopicHistory()
                            .setCreateTime(t.getTopicCreateTime())
                            .setErrorCode(t.getErrorCode())
                            .setErrorCount(t.getErrorCount())
                            .setErrorDesc(t.getErrorDesc());
                    try {
                      history.setWorstDeviceIds(
                          Arrays.asList(mapper.readValue(t.getWorstDeviceId(), String[].class)));
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                    try {
                      if (isNotEmpty(t.getConclusion())) {
                        EerpmGeneralConclusionBean conclusion =
                            mapper.readValue(t.getConclusion(), EerpmGeneralConclusionBean.class);
                        history
                            .setErrorDesc(conclusion.getErrorDesc())
                            .setCauses(
                                Stream.concat(
                                        defaultList(conclusion.getOriginCauseSolution())
                                            .stream()
                                            .map(EerpOriginCauseSolution::getCauseDesc),
                                        defaultList(conclusion.getNewCauseSolution())
                                            .stream()
                                            .map(EerpNewCauseSolution::getCauseDesc))
                                    .filter(StringUtils::isNotEmpty)
                                    .distinct()
                                    .collect(toList()))
                            .setSolutions(
                                Stream.concat(
                                        defaultList(conclusion.getOriginCauseSolution())
                                            .stream()
                                            .map(
                                                s ->
                                                    isEmpty(s.getNewSolution())
                                                        ? s.getOriginSolution()
                                                        : s.getNewSolution()),
                                        defaultList(conclusion.getNewCauseSolution())
                                            .stream()
                                            .map(EerpNewCauseSolution::getSolution))
                                    .filter(StringUtils::isNotEmpty)
                                    .distinct()
                                    .collect(toList()));
                      }
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                    return history;
                  })
              .sorted(comparing(EerpmTopicHistory::getCreateTime))
              .collect(toList()));
    }
    return mapper.writeValueAsString(content);
  }

  private <T> List<T> defaultList(List<T> list) {
    return ofNullable(list).orElseGet(Collections::emptyList);
  }
}
