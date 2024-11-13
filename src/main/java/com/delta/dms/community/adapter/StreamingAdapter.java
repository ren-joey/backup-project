package com.delta.dms.community.adapter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.config.StreamingConfig;
import com.delta.dms.community.swagger.model.VideoMappingDto;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class StreamingAdapter {
  private static final String QUERY_KEY = "queryKey";
  private static final String QUERY_KEY_VALUE = "ddfId";
  private static final String ID = "id";
  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private AdapterUtil adapterUtil;
  private StreamingConfig streamingConfig;

  @Autowired
  public StreamingAdapter(AdapterUtil adapterUtil, StreamingConfig streamingConfig) {
    this.adapterUtil = adapterUtil;
    this.streamingConfig = streamingConfig;
  }

  public void upsertMapping(VideoMappingDto data) throws IOException {
    ResponseEntity<String> response =
        adapterUtil.sendRequest(
            streamingConfig.getMappingUrl(),
            HttpMethod.POST,
            adapterUtil.generateHeaderWithCookies(),
            mapper.writeValueAsString(data),
            null,
            String.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (HttpStatus.OK != response.getStatusCode()
        && HttpStatus.CREATED != response.getStatusCode()) {
      throw new HttpClientErrorException(response.getStatusCode());
    }
  }

  public void deleteMapping(String ddfId) {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(QUERY_KEY, QUERY_KEY_VALUE);
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            streamingConfig.getVideoIdUrl(ddfId),
            HttpMethod.DELETE,
            adapterUtil.generateHeaderWithCookies(),
            null,
            uriVariables,
            JsonNode.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (HttpStatus.OK != response.getStatusCode()
        && HttpStatus.NOT_FOUND != response.getStatusCode()) {
      throw new HttpClientErrorException(response.getStatusCode());
    }
  }

  public List<VideoMappingDto> getVideoInfo(List<String> videoIdList) {
    if (CollectionUtils.isEmpty(videoIdList)) {
      return Collections.emptyList();
    }
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(QUERY_KEY, QUERY_KEY_VALUE);
    uriVariables.addAll(ID, videoIdList);
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            streamingConfig.getVideoIdUrl(),
            HttpMethod.GET,
            adapterUtil.generateHeaderWithCookies(),
            null,
            uriVariables,
            JsonNode.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    log.debug(response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      throw new HttpClientErrorException(response.getStatusCode());
    }
    return StreamSupport.stream(responseBody.path(Constants.RESPONSE_DATA).spliterator(), false)
        .map(
            item -> {
              try {
                return mapper.treeToValue(item, VideoMappingDto.class);
              } catch (JsonProcessingException e) {
                log.error(e);
                return new VideoMappingDto();
              }
            })
        .filter(item -> StringUtils.isNotEmpty(item.getDdfId()))
        .collect(Collectors.toList());
  }
}
