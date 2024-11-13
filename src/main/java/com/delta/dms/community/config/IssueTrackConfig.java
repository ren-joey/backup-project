package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("issuetrack")
public class IssueTrackConfig {
	
  private String forumId;
  private String adminId;
}
