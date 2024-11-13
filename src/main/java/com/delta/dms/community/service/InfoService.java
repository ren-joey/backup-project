package com.delta.dms.community.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.DataHiveInfoAdapter;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.swagger.model.Info;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;

@Service
public class InfoService {

  private static final String SERVER_PREFIX = "DMS";

  private static final LogUtil log = LogUtil.getInstance();
  private DataHiveInfoAdapter dataHiveInfoAdapter;
  private YamlConfig yamlConfig;

  @Autowired
  public InfoService(DataHiveInfoAdapter dataHiveInfoAdapter, YamlConfig yamlConfig) {
    this.dataHiveInfoAdapter = dataHiveInfoAdapter;
    this.yamlConfig = yamlConfig;
  }

  public Info getInfo() {
    Map<String, String> version = new HashMap<>();
    version.put(Constants.INFO_PROJECT_NAME, yamlConfig.getVersion());
    dataHiveInfoAdapter
        .getDataHiveInfo()
        .entrySet()
        .forEach(item -> version.put(item.getKey(), item.getValue()));
    return new Info().serverName(getServerName()).version(version);
  }

  private String getServerName() {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      String hostname = ip.getHostName();
      return String.format("%s(%s)", SERVER_PREFIX, hostname);
    } catch (UnknownHostException e) {
      log.error(e);
    }
    return "";
  }
}
