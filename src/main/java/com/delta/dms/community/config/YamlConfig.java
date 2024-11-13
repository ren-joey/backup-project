package com.delta.dms.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import lombok.Data;

@Data
@Configuration
@RefreshScope
public class YamlConfig {

  @Value("${host}")
  private String host;

  @Value("${info.version}")
  private String version;

  @Value("${env.identity}")
  private String envIdentity;

  @Value("${appkey.community}")
  private String appId;

  @Value("${appkey.mydms}")
  private String mydmsAppId;

  @Value("${mobile.host}")
  private String mobileHost;

  @Value("${mobile.download-path}")
  private String mobileDownloadPath;

  @Value("${hive.cache.path}")
  private String hiveCachePath;

  @Value("${log-timeunit}")
  private String logTimeUnit;

  @Value("${sys.admin-gid}")
  private String sysAdminGid;

  @Value("${sys.admin-username}")
  private String sysAdminUserName;

  @Value("${sys.admin-password}")
  private String sysAdminPassword;

  @Value("${deltatube.allow-upload.community}")
  private boolean allowUploadDtu;

  @Value("${public-search-gid}")
  private String publicSearchGid;

  @Value("${group-setting.root-id}")
  private String appGroupRootId;

  @Value("${general-setting.hot-lasting-min}")
  private Integer hotLastingMin;

  public AuthenticationToken getSysAdminAuthenticationToken() {
    return new AuthenticationToken().username(sysAdminUserName).password(sysAdminPassword);
  }

  public String getMobileDownloadUrl() {
    if (isTestingEnv(envIdentity)) {
      return mobileHost;
    } else {
      return new StringBuilder().append(mobileHost).append(mobileDownloadPath).toString();
    }
  }

  private boolean isTestingEnv(String env) {
    return "dev".contentEquals(env) || "test".contentEquals(env);
  }
}
