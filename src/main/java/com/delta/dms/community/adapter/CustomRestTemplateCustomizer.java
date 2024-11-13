package com.delta.dms.community.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomRestTemplateCustomizer implements RestTemplateCustomizer {

  @Autowired private CustomClientHttpRequestInterceptor customClientHttpRequestInterceptor;

  @Override
  public void customize(RestTemplate restTemplate) {
    restTemplate.getInterceptors().add(customClientHttpRequestInterceptor);
  }
}
