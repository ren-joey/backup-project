package com.delta.dms.community.service.eerp.topic;

import static com.delta.dms.community.swagger.model.TopicType.EERPMHIGHLEVEL;
import java.io.IOException;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.model.eerpm.EerpmTopicRawData;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.ReplyService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.service.eerp.dashboard.EerpmDashboardService;
import com.delta.dms.community.swagger.model.EerpTopicData;
import com.delta.dms.community.swagger.model.TopicType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EerpmHighLevelService extends EerpmGeneralService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public EerpmHighLevelService(
      TopicService topicService,
      EerpConfig eerpConfig,
      EerpmDashboardService eerpmDashboardService,
      ForumService forumService,
      ReplyService replyService) {
    super(topicService, eerpConfig, eerpmDashboardService, forumService, replyService);
  }

  @Override
  protected TopicType getType() {
    return EERPMHIGHLEVEL;
  }

  @Override
  protected String getTopicText(EerpTopicData data, TopicInfo origin) {
    try {
      EerpmTopicRawData originContent =
          mapper.readValue(origin.getTopicText(), EerpmTopicRawData.class);
      EerpmTopicRawData content = mapper.readValue(data.getText(), EerpmTopicRawData.class);
      content.setHistories(originContent.getHistories());
      return mapper.writeValueAsString(content);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return data.getText();
  }
}
