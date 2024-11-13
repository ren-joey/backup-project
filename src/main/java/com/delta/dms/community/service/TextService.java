package com.delta.dms.community.service;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.delta.dms.community.adapter.AdapterUtil;
import com.delta.dms.community.config.PersonWordCloudConfig;
import com.delta.dms.community.exception.CommunityException;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.PersonWordCloudConstants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class TextService {

  private static final LogUtil log = LogUtil.getInstance();

  private AdapterUtil adapterUtil;
  private PersonWordCloudConfig personWordCloudConfig;

  @Autowired
  public TextService(AdapterUtil adapterUtil, PersonWordCloudConfig personWordCloudConfig) {
    this.adapterUtil = adapterUtil;
    this.personWordCloudConfig = personWordCloudConfig;
  }

  public String getPersonWordCloud(String id, Integer topn, String lang) {
    String url = personWordCloudConfig.getBaseUrl();
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(PersonWordCloudConstants.PERSON_WORD_CLOUD_ID, id);
    uriVariables.add(PersonWordCloudConstants.PERSON_WORD_CLOUD_TOPN, topn.toString());
    uriVariables.add(PersonWordCloudConstants.PERSON_WORD_CLOUD_LANG, lang);

    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(url, HttpMethod.GET, null, null, uriVariables, JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (Objects.isNull(responseBody)) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new CommunityException(Constants.CONNECT + url + Constants.ERROR);
    }
    return responseBody.toString();
  }
}
