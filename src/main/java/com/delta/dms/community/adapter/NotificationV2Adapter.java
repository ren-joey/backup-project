package com.delta.dms.community.adapter;

import static java.util.Objects.isNull;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.delta.dms.community.adapter.entity.JwtToken;
import com.delta.dms.community.bean.notification.v2.NotificationV2Request;
import com.delta.dms.community.config.NotificationV2Config;
import com.delta.set.utils.LogUtil;

@Component
public class NotificationV2Adapter {

  private static final LogUtil log = LogUtil.getInstance();

  @Autowired private NotificationV2Config notificationV2Config;
  @Autowired private UserGroupAdapter userGroupAdapter;
  @Autowired private AdapterUtil adapterUtil;

  public void sendNotification(NotificationV2Request request) {
    HttpHeaders headers =
        adapterUtil.generateHeaderWithJwt(
            Optional.ofNullable(
                    userGroupAdapter.getUserTokenBySamAccountAndPassword(
                        notificationV2Config.getAuthenticationToken(),""))
                .map(JwtToken::getAccessToken)
                .orElse(""));
    ResponseEntity<String> response =
        adapterUtil.sendRequest(
            notificationV2Config.getUrl(), HttpMethod.POST, headers, request, null, String.class);
    if (isNull(response) || !HttpStatus.OK.equals(response.getStatusCode())) {
      log.error("send notification failed");
    } else {
      log.info("send notification success");
    }
  }
}
