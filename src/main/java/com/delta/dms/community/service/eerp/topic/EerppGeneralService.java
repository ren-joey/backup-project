package com.delta.dms.community.service.eerp.topic;

import static com.delta.dms.community.swagger.model.TopicType.EERPPGENERAL;
import java.io.IOException;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.model.TopicType;

@Service
public class EerppGeneralService extends BaseEerpTopicService {

  public EerppGeneralService(EerpConfig eerpConfig, TopicService topicService) {
    super(eerpConfig, topicService);
  }

  @Override
  protected TopicType getType() {
    return EERPPGENERAL;
  }

  @Override
  protected void updateConclusionText(TopicInfo topic) throws IOException {
    // Do nothing
  }
}
