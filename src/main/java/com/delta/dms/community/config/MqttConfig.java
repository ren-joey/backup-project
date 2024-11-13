package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.utils.Constants;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("mqtt")
public class MqttConfig {

  private String host;
  private String port;
  private String username;
  private String password;

  public String getBaseUrl() {
    return new StringBuilder().append(host).append(Constants.COLON).append(port).toString();
  }
}
