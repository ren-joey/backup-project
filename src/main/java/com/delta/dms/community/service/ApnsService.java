package com.delta.dms.community.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.publish.ApnsPublisher;
import com.delta.dms.community.swagger.model.PublishMessage;

@Service
public class ApnsService {

  private ApnsPublisher apnsPublisher;
  private static final String TYPE = "type";
  private static final String TARGET_ID = "targetId";
  private static final String NOTIFICATION_ID = "notificationId";
  private static final String HTML_BOLD = "<b>|</b>";

  @Autowired
  public ApnsService(ApnsPublisher apnsPublisher) {
    this.apnsPublisher = apnsPublisher;
  }

  public void publishMessage(PublishMessage publishMessage, Set<String> deviceTokens) {
    Map<String, String> customProperty = new HashMap<>();
    customProperty.put(TYPE, publishMessage.getType().toString());
    customProperty.put(TARGET_ID, String.valueOf(publishMessage.getTargetId()));
    customProperty.put(NOTIFICATION_ID, String.valueOf(publishMessage.getId()));
    apnsPublisher.push(
        deviceTokens,
        publishMessage.getTitle(),
        publishMessage.getDesc().replaceAll(HTML_BOLD, ""),
        publishMessage.getContent(),
        customProperty,
        publishMessage.getNotificationCount());
  }
}
