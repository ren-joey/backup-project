package com.delta.dms.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.publish.MqttPublisher;

@Service
public class MqttService {

  private MqttPublisher mqttPublisher;

  @Autowired
  public MqttService(MqttPublisher mqttPublisher) {
    this.mqttPublisher = mqttPublisher;
  }

  public void publishMessage(String topic, String message) {
    mqttPublisher.publishMessage(topic, message);
  }
}
