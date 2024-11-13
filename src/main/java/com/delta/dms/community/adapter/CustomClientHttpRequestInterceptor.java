package com.delta.dms.community.adapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.GroupConfig;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.set.utils.LogUtil;

@Component
public class CustomClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  @Autowired private GroupConfig groupConfig;
  @Autowired private MyDmsConfig myDmsConfig;

  private static final LogUtil log = LogUtil.getInstance();

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    logRequestDetails(request, body);
    ClientHttpResponse response = execution.execute(request, body);
    log.debug(String.format("Response Status: %s", response.getStatusCode()));
    return response;
  }

  private void logRequestDetails(HttpRequest request, byte[] body) throws IOException {
    log.debug(
        String.format(
            "Request Method: %s, Request URI: %s", request.getMethod(), request.getURI()));
    if (StringUtils.equals(myDmsConfig.getFileUploadUrl(), request.getURI().toString())) {
      return;
    }
    if (HttpMethod.POST != request.getMethod()
        || !StringUtils.equals(groupConfig.getTokenUrl(), request.getURI().toString())) {
      log.debug(String.format("Request Body: %s", new String(body, StandardCharsets.UTF_8.name())));
    }
  }
}
