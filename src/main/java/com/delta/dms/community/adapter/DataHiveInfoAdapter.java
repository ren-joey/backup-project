package com.delta.dms.community.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.delta.datahive.api.DDF.DataHiveUtils;
import com.delta.datahive.structures.Tuple;

@Component
public class DataHiveInfoAdapter {

  private DataHiveUtils dataHiveUtils;

  @Autowired
  public DataHiveInfoAdapter() {
    this.dataHiveUtils = new DataHiveUtils();
  }

  public void setDataHiveUtils(DataHiveUtils dataHiveUtils) {
    this.dataHiveUtils = dataHiveUtils;
  }

  public Map<String, String> getDataHiveInfo() {
    Map<String, String> infoMap = new HashMap<>();
    List<Tuple<String, String>> infoList = dataHiveUtils.getBuildInfo();
    if (infoList != null) {
      infoMap = infoList.stream().collect(Collectors.toMap(Tuple::getKey, Tuple::getVal));
    }
    return infoMap;
  }
}
