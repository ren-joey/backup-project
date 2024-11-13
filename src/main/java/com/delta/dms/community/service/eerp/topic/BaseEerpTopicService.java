package com.delta.dms.community.service.eerp.topic;

import java.io.IOException;
import java.util.Collections;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.model.EerpTopicCreationData;
import com.delta.dms.community.swagger.model.EerpTopicData;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.TopicCreationData;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.swagger.model.TopicUpdatedData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseEerpTopicService {

  final EerpConfig eerpConfig;
  private final TopicService topicService;

  public int createTopic(EerpTopicCreationData topicData) {
    return topicService.createTopic(convertToTopicCreationData(topicData), false);
  }

  public void updateEerpTopic(TopicInfo origin, EerpTopicData data) throws IOException {
    topicService.updateTopic(origin.getTopicId(), convertToTopicUpdatedData(data, origin));
    updateConclusionText(origin.setTopicText(data.getText()));
  }

  protected String getTopicText(EerpTopicData data, TopicInfo origin) {
    return data.getText();
  }

  protected TopicCreationData convertToTopicCreationData(EerpTopicCreationData data) {
    return new TopicCreationData()
        .forumId(data.getForumId())
        .title(data.getTitle())
        .tag(data.getTag())
        .type(getType())
        .text(data.getText())
        .attachment(Collections.emptyList())
        .notificationType(null)
        .recipient(Collections.emptyList())
        .appField(
            Collections.singletonList(
                new LabelValueDto().value(eerpConfig.getDefaultAppFieldId())));
  }

  protected abstract TopicType getType();

  protected abstract void updateConclusionText(TopicInfo topic) throws IOException;

  private TopicUpdatedData convertToTopicUpdatedData(EerpTopicData data, TopicInfo origin) {
    return new TopicUpdatedData()
        .title(data.getTitle())
        .tag(data.getTag())
        .type(getType())
        .text(getTopicText(data, origin))
        .attachment(Collections.emptyList())
        .notificationType(null)
        .recipient(Collections.emptyList())
        .modifiedTime(origin.getTopicModifiedTime());
  }
}
