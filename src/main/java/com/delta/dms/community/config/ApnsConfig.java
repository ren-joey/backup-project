package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("apns")
public class ApnsConfig {

  private String host;
  private String cert;
  private String password;
  private Integer semaphoreCount;
  private Integer connectionPool;
  private Integer eventThreads;
  private String topic;
}
