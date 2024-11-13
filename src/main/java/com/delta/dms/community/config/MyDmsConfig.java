package com.delta.dms.community.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("mydms")
public class MyDmsConfig {
  private YamlConfig yamlConfig;

  private String basePath;
  private String fileUploadPath;
  private String tagAuthor;
  private String tagTitle;
  private String tagAppField;
  private String tagRecordType;
  private String recordTypePath;
  private String getOrgFolderPathFormat;
  private String createFolderPath;

  @Autowired
  public MyDmsConfig(YamlConfig yamlConfig) {
    this.yamlConfig = yamlConfig;
  }

  private StringBuilder getBaseUrl() {
    return new StringBuilder().append(yamlConfig.getHost()).append(basePath);
  }

  public String getFileUploadUrl() {
    return getBaseUrl().append(fileUploadPath).toString();
  }

  public String getRecordTypeUrl() {
    return getBaseUrl().append(recordTypePath).toString();
  }

  public String getOrgFolderUrl(String gid) {
    return getBaseUrl().append(String.format(getOrgFolderPathFormat, gid)).toString();
  }

  public String getCreateFolderUrl() {
    return getBaseUrl().append(createFolderPath).toString();
  }
}
