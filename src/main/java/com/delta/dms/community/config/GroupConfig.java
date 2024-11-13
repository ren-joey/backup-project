package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.utils.Constants;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("group")
public class GroupConfig {

  private String host;
  private String version;
  private String basics;
  private String basicsField;
  private String basicsMids;
  private String basicsUids;
  private String deep;
  private String filter;
  private String group;
  private String id;
  private String path;
  private String pathGid;
  private String token;
  private String newtoken;
  private String tokenPassword;
  private String tokenUsername;
  private String uid;
  private String members;
  private String orgGroup;
  private String deltapoints;
  private String startTime;
  private String endTime;
  private String withDetail;
  private String name;
  private String groupList;
  private String userGroup;

  public StringBuilder getBaseUrl() {
    return new StringBuilder().append(host).append(version).append(Constants.SLASH);
  }

  public String getBasicsUrl() {
    return getBaseUrl().append(basics).toString();
  }

  public String getGroupUrl() {
    return getBaseUrl().append(group).toString();
  }

  public String getPathUrl() {
    return getBaseUrl().append(path).toString();
  }

  public String getTokenUrl() {
    return getBaseUrl().append(token).toString();
  }
  
  public String getNewtokenUrl() {
    return getBaseUrl().append(newtoken).toString();
  }

  public String getMembersUrl(String gid) {
    return getBaseUrl().append(members).append(Constants.SLASH).append(gid).toString();
  }

  public String getOrgGroupUrl(String groupId) {
    return getBaseUrl().append(orgGroup).append(Constants.SLASH).append(groupId).toString();
  }

  public String getOrgGroupMembersUrl() {
    return getBaseUrl().append(orgGroup).append(Constants.SLASH).append(members).toString();
  }

  public String getDeltapointsUrl(String userId) {
    return getBaseUrl().append(deltapoints).append(Constants.SLASH).append(userId).toString();
  }

  public String getGroupListUrl() {
    return getBaseUrl().append(groupList).toString();
  }

  public String getUserGroupUrl() {
    return getBaseUrl().append(userGroup).toString();
  }
}
