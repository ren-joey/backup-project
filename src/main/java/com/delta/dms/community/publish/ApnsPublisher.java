package com.delta.dms.community.publish;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.ApnsConfig;
import com.delta.dms.community.service.DeviceService;
import com.delta.set.utils.LogUtil;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

@Component
public class ApnsPublisher {

  private static final LogUtil log = LogUtil.getInstance();
  private static final String BAD_DEVICE_TOKEN = "BadDeviceToken";

  private DeviceService deviceService;
  private ApnsConfig apnsConfig;
  private ApnsClient apnsClient;
  private Semaphore semaphore;

  @Autowired
  public ApnsPublisher(DeviceService deviceService, ApnsConfig apnsConfig) {
    this.deviceService = deviceService;
    this.apnsConfig = apnsConfig;
    semaphore = new Semaphore(apnsConfig.getSemaphoreCount());
  }

  private void connect() {
    log.debug("Connect to apns");
    InputStream clientCredential = null;
    try {
      clientCredential = this.getClass().getClassLoader().getResourceAsStream(apnsConfig.getCert());
      EventLoopGroup eventLoopGroup = new NioEventLoopGroup(apnsConfig.getEventThreads());
      apnsClient =
          new ApnsClientBuilder()
              .setApnsServer(apnsConfig.getHost())
              .setClientCredentials(clientCredential, apnsConfig.getPassword())
              .setConcurrentConnections(apnsConfig.getConnectionPool())
              .setEventLoopGroup(eventLoopGroup)
              .build();
    } catch (IOException e) {
      log.error(e);
    } finally {
      try {
        if (clientCredential != null) {
          clientCredential.close();
        }
      } catch (IOException e) {
        log.error(e);
      }
    }
  }

  public void push(
      Set<String> deviceTokens,
      String title,
      String subtitle,
      String body,
      Map<String, String> customProperty,
      int badgeNumber) {
    if (null == apnsClient) {
      this.connect();
    }
    for (String deviceToken : deviceTokens) {
      ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
      payloadBuilder.setAlertTitle(title);
      payloadBuilder.setAlertSubtitle(subtitle);
      payloadBuilder.setAlertBody(body);
      payloadBuilder.setBadgeNumber(badgeNumber);
      customProperty
          .entrySet()
          .forEach(entry -> payloadBuilder.addCustomProperty(entry.getKey(), entry.getValue()));

      String payload = payloadBuilder.buildWithDefaultMaximumLength();
      final String token = TokenUtil.sanitizeTokenString(deviceToken);
      SimpleApnsPushNotification pushNotification =
          new SimpleApnsPushNotification(token, apnsConfig.getTopic(), payload);

      try {
        semaphore.acquire();
      } catch (InterruptedException e) {
        log.error(String.format("ios push get semaphore failed, deviceToken:%s", deviceToken));
        Thread.currentThread().interrupt();
      }
      final Future<PushNotificationResponse<SimpleApnsPushNotification>> future =
          apnsClient.sendNotification(pushNotification);
      future.addListener(
          pushNotificationResponseFuture -> {
            if (future.isSuccess()) {
              final PushNotificationResponse<SimpleApnsPushNotification> response = future.getNow();
              handleResponse(response, deviceToken);
            } else {
              log.error(future.cause());
              log.error(
                  String.format(
                      "send notification device token=%s is failed %s",
                      token, future.cause().getMessage()));
            }
            semaphore.release();
          });
    }
  }

  private void handleResponse(
      PushNotificationResponse<SimpleApnsPushNotification> response, String deviceToken) {
    if (!response.isAccepted()) {
      log.error("Notification rejected by the APNs gateway: " + response.getRejectionReason());
      if (BAD_DEVICE_TOKEN.equals(response.getRejectionReason())) {
        deviceService.deleteBadDeviceToken(deviceToken);
      }
    }
  }
}
