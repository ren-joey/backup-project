package com.delta.dms.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@RefreshScope
public class DLConfig {

  @Value("${applicationkey.mobile}")
  private String mobileAppKey;
  
  @Value("${dl.allow-user-name}")
  private String dlUserName;
  
  @Value("${dl.allow-user-id}")
  private String dlUserId;
  
  @Value("${dl.allow-community-id}")
  private String dlCommunityId;
  
  @Value("${dl.allow-forum-id}")
  private String dlForumId;
}
