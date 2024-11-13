package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import com.delta.dms.community.swagger.model.DiaClassification;
import com.delta.dms.community.utils.Constants;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("dia")
public class DiaConfig {

  private String authUser;
  private String authPassword;
  private String smbHost;
  private int smbPort;
  private String smbSharedDir;
  private String smbAuthDomain;
  private String adminUsername;
  private String adminPassword;
  private String communityNameFormat;
  private String defaultAppFieldId;

  public String getDecodedAuthToken() {
    return new StringBuilder(authUser).append(Constants.COLON).append(authPassword).toString();
  }

  public String getCommunityName(int year, DiaClassification classification) {
    return String.format(communityNameFormat, year, classification.toString());
  }

  public AuthenticationToken getAdminAuthenticationToken() {
    return new AuthenticationToken().username(adminUsername).password(adminPassword);
  }
}
