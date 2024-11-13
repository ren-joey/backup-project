package com.delta.dms.community.service.eerp.topic;

import static com.delta.dms.community.swagger.model.TopicType.EERPMSUMMARY;
import java.io.IOException;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.model.TopicType;

@Service
public class EerpmSummaryService extends BaseEerpTopicService {

  public EerpmSummaryService(EerpConfig eerpConfig, TopicService topicService) {
    super(eerpConfig, topicService);
  }

  @Override
  protected TopicType getType() {
    return EERPMSUMMARY;
  }

  @Override
  protected void updateConclusionText(TopicInfo topic) throws IOException {
    // Do nothing
  }
}
