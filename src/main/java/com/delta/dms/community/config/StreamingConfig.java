package com.delta.dms.community.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Setter;

@Setter
@Configuration
@ConfigurationProperties("streaming")
public class StreamingConfig {
  private YamlConfig yamlConfig;

  private String basePath;
  private String mappingPath;
  private String videoPathFormat;
  private String videoIdPath;

  @Autowired
  public StreamingConfig(YamlConfig yamlConfig) {
    this.yamlConfig = yamlConfig;
  }

  private StringBuilder getBaseUrl() {
    return new StringBuilder().append(yamlConfig.getHost()).append(basePath);
  }

  public String getMappingUrl() {
    return getBaseUrl().append(mappingPath).toString();
  }

  public String getVideoIdUrl(String id) {
    return String.format(getBaseUrl().append(videoPathFormat).toString(), id);
  }

  public String getVideoIdUrl() {
    return getBaseUrl().append(videoIdPath).toString();
  }
}
