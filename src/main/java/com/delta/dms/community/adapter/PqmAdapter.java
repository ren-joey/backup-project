package com.delta.dms.community.adapter;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.PqmConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.exception.PqmException;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.PqmConstants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;

@Component
public class PqmAdapter {
  private PqmConfig pqmConfig;
  private AdapterUtil adapterUtil;
  private static final LogUtil log = LogUtil.getInstance();
  private static final String RESULT = "$.RESULT";
  private static final String MESSAGE = "$.MESSAGE";
  private static final String SUCCESS = "SUCCESS";

  @Autowired
  public PqmAdapter(PqmConfig pqmConfig, AdapterUtil adapterUtil) {
    this.pqmConfig = pqmConfig;
    this.adapterUtil = adapterUtil;
  }
  
  public String sendMesDataInPqm(TopicInfo topicInfo, String jsonData) {
    String url = pqmConfig.getBaseUrl();
    String param = getPqmParam(topicInfo, jsonData);
    ResponseEntity<String> response =
        adapterUtil.sendRequest(url, HttpMethod.POST, null, param, null, String.class);
    if (response == null) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new PqmException(Constants.CONNECT + url + Constants.ERROR);
    } else {
      String result = JsonPath.read(response.getBody(), RESULT);
      if (!Objects.equals(result, SUCCESS)) {
        log.error(JsonPath.read(response.getBody(), MESSAGE));
        throw new PqmException(JsonPath.read(response.getBody(), MESSAGE));
      }
    }
    return "";
  }

  private String getPqmParam(TopicInfo topicInfo, String jsonData) {
    ObjectNode param = new ObjectMapper().createObjectNode();
    param.put(
        PqmConstants.PQM_PRODUCING_ZONE,
        JsonPath.read(topicInfo.getTopicText(), PqmConstants.JSONPATH_PRODUCING_ZONE).toString());
    param.put(
        PqmConstants.PQM_EQUIPMENT_TYPE,
        JsonPath.read(topicInfo.getTopicText(), PqmConstants.JSONPATH_EQUIPMENT_MODEL).toString());
    param.put(
        PqmConstants.PQM_ERROR_CODE,
        JsonPath.read(topicInfo.getTopicText(), PqmConstants.JSONPATH_ERROR_CODE).toString());
    param.put(
        PqmConstants.PQM_CAUSE_DESCRIPTION,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_REASON).toString());
    param.put(
        PqmConstants.PQM_SOLUTION_DESCRIPTION,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_DESCRIPTION).toString());
    param.put(
        PqmConstants.PQM_NOTIFICATION_ROLE,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_ROLE).toString());
    param.put(
        PqmConstants.PQM_SOLUTION_WAY,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_TYPE).toString());
    param.put(
        PqmConstants.PQM_SOLUTION_CLASS,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_CLASSIFICATION).toString());
    param.put(
        PqmConstants.PQM_EXECUTE_OBJECT,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_OBJECT).toString());
    param.put(
        PqmConstants.PQM_COMMAND,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_COMMAND).toString());
    param.put(
        PqmConstants.PQM_PARAMETER_NAME,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_COMMANDKEY).toString());
    param.put(
        PqmConstants.PQM_PARAMETER_VALUE,
        JsonPath.read(jsonData, PqmConstants.JSONPATH_COMMANDVALUE).toString());
    Integer uuid = topicInfo.getTopicId();
    param.put(PqmConstants.PQM_UUID, uuid.toString());

    return param.toString();
  }
}
