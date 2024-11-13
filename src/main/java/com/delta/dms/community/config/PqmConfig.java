package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.utils.Constants;
import lombok.Setter;

@Setter
@Configuration
@ConfigurationProperties("pqm")
public class PqmConfig {

  private String host;
  private String path;
  private String appId;

  public String getBaseUrl() {
    return new StringBuilder()
        .append(host)
        .append(path)
        .append(Constants.SLASH)
        .append(appId)
        .toString();
  }
}
