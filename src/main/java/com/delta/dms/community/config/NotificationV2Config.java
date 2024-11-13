package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
@Configuration
@ConfigurationProperties("notification")
public class NotificationV2Config {
  private String url;

  @Getter(value = AccessLevel.NONE)
  private String sysAdminUsername;

  @Getter(value = AccessLevel.NONE)
  private String sysAdminPassword;

  public AuthenticationToken getAuthenticationToken() {
    return new AuthenticationToken().username(sysAdminUsername).password(sysAdminPassword);
  }
}
