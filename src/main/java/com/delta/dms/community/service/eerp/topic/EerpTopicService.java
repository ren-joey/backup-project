package com.delta.dms.community.service.eerp.topic;

import static com.delta.dms.community.utils.Constants.ERR_INVALID_PARAM;
import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.model.eerpm.EerpmTopicRawData;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.model.EerpTopicCreationData;
import com.delta.dms.community.swagger.model.EerpTopicData;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EerpTopicService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final Map<TopicType, BaseEerpTopicService> serviceMap =
      new EnumMap<>(TopicType.class);

  private final List<BaseEerpTopicService> services;
  private final TopicService topicService;

  @PostConstruct
  public void init() {
    services.forEach(service -> serviceMap.put(service.getType(), service));
  }

  public int createTopic(EerpTopicCreationData data) {
    return getService(data).createTopic(data);
  }

  public void updateTopic(int topicId, EerpTopicData data) throws IOException {
    TopicInfo origin = topicService.getTopicInfoById(topicId);
    getService(TopicType.fromValue(origin.getTopicType())).updateEerpTopic(origin, data);
  }

  private BaseEerpTopicService getService(EerpTopicCreationData data) {
    return getService(getTopicType(data));
  }

  private BaseEerpTopicService getService(TopicType type) {
    return ofNullable(serviceMap.get(type))
        .orElseThrow(() -> new IllegalArgumentException(ERR_INVALID_PARAM));
  }

  private TopicType getTopicType(EerpTopicCreationData data) {
    if (Objects.isNull(data.getType())) {
      try {
        EerpmTopicRawData textRawData = mapper.readValue(data.getText(), EerpmTopicRawData.class);
        return textRawData.isSummary() ? TopicType.EERPMSUMMARY : TopicType.EERPMGENERAL;
      } catch (IOException e) {
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
      }
    }
    return data.getType();
  }
}
