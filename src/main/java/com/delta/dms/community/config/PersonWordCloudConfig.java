package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.utils.Constants;
import lombok.Setter;

@Setter
@Configuration
@ConfigurationProperties("personwordcloud")
public class PersonWordCloudConfig {

  private String host;
  private String version;
  private String name;

  public String getBaseUrl() {
    return new StringBuilder()
        .append(host)
        .append(version)
        .append(Constants.SLASH)
        .append(name)
        .toString();
  }
}
