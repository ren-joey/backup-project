package com.delta.dms.community.publish;

import java.util.UUID;
import javax.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.MqttConfig;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;

@Component
public class MqttPublisher implements MqttCallbackExtended {

  private static final LogUtil log = LogUtil.getInstance();

  private MqttConfig mqttConfig;

  private MqttClient mqttClient;

  @Autowired
  public MqttPublisher(MqttConfig mqttConfig) {
    this.mqttConfig = mqttConfig;
  }

  private void connect() {
    log.debug("Connect to mqtt");
    String brokerUrl = Constants.TCP + mqttConfig.getBaseUrl();
    MemoryPersistence persistence = new MemoryPersistence();
    MqttConnectOptions connectionOptions = new MqttConnectOptions();
    try {
      String clientId = UUID.randomUUID().toString();
      this.mqttClient = new MqttClient(brokerUrl, clientId, persistence);
      connectionOptions.setUserName(mqttConfig.getUsername());
      connectionOptions.setPassword(mqttConfig.getPassword().toCharArray());
      connectionOptions.setCleanSession(true);
      connectionOptions.setAutomaticReconnect(true);
      this.mqttClient.setCallback(this);
      this.mqttClient.connect(connectionOptions);
    } catch (MqttException e) {
      log.error(e);
    }
  }

  public void setMqttClient(MqttClient mqttClient) {
    this.mqttClient = mqttClient;
  }

  public void publishMessage(String topic, String message) {
    try {
      MqttMessage mqttmessage = new MqttMessage(message.getBytes());
      mqttmessage.setQos(Constants.QOS);
      if (null == mqttClient || !this.mqttClient.isConnected()) {
        this.connect();
      }
      this.mqttClient.publish(topic, mqttmessage);
    } catch (MqttException e) {
      log.error(e);
    }
  }

  @PreDestroy
  public void disconnect() {
    if (null == mqttClient) {
      return;
    }
    try {
      log.debug("disconnect");
      this.mqttClient.disconnect();
      this.mqttClient.disconnectForcibly();
      this.mqttClient.close();
    } catch (MqttException e) {
      log.error(e);
    }
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken arg0) {
    log.debug("delivery completed");
  }

  @Override
  public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
    log.debug("Message arrived");
  }

  @Override
  public void connectionLost(Throwable cause) {
    log.error("Connection lost");
    log.error(cause);
  }

  @Override
  public void connectComplete(boolean reconnect, String serverUri) {
    log.debug("Connected To Broker successfully");
  }
}
