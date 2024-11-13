package com.delta.dms.community.adapter;

import java.io.IOException;
import java.util.Objects;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.exception.EerpException;
import com.delta.dms.community.model.EerpGeneralResult;
import com.delta.dms.community.model.eerpm.EerpmConclusionDetail;
import com.delta.dms.community.model.eerpm.EerpmErrorCause;
import com.delta.dms.community.model.eerpm.EerpmErrorCode;
import com.delta.dms.community.model.eerpm.EerpmErrorDetail;
import com.delta.dms.community.model.eerpm.EerpmErrorSolution;
import com.delta.dms.community.model.eerpp.EerppConclusionDetail;
import com.delta.dms.community.model.eerpq.EerpqDutyCode;
import com.delta.dms.community.model.eerpq.EerpqErrorDto;
import com.delta.dms.community.model.eerpq.EerpqFailureCode;
import com.delta.dms.community.model.eerpq.EerpqPhenomenonCode;
import com.delta.dms.community.model.eerpq.EerpqReasonCode;
import com.delta.dms.community.model.eerpq.EerpqSolutionCode;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EerpConstants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EerpAdapter {
  private static final String LOG_RESPONSE = "response: ";
  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private EerpConfig eerpConfig;
  private AdapterUtil adapterUtil;

  @Autowired
  public EerpAdapter(EerpConfig eerpConfig, AdapterUtil adapterUtil) {
    this.eerpConfig = eerpConfig;
    this.adapterUtil = adapterUtil;
  }

  public EerpmErrorCode getEerpmErrorCode(EerpmErrorDetail error) throws IOException {
    final String url = eerpConfig.getEerpmErrorCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpmErrorCode.class);
  }

  public EerpmErrorCause getEerpmCause(EerpmErrorDetail error) throws IOException {
    final String url = eerpConfig.getEerpmCausesUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpmErrorCause.class);
  }

  public EerpmErrorSolution getEerpmSolution(EerpmErrorDetail error) throws IOException {
    final String url = eerpConfig.getEerpmSolutionUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpmErrorSolution.class);
  }

  public void pushEerpmConclusion(EerpmConclusionDetail conclusion) throws IOException {
    final String url = eerpConfig.getEerpmPushConclusionUrl();
    log.debug("url: " + url);
    log.debug("request: " + mapper.writeValueAsString(conclusion));
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(conclusion),
            null,
            JsonNode.class);
    validateResponse(response, url);
  }

  public void pushEerppConclusion(EerppConclusionDetail conclusion) throws IOException {
    final String url = eerpConfig.getEerppPushConclusionUrl();
    log.debug("request: " + mapper.writeValueAsString(conclusion));
    log.debug("url: " + url);
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(conclusion),
            null,
            JsonNode.class);
    validateResponse(response, url);
  }

  public EerpqPhenomenonCode getEerpqPhenomenonCode(EerpqErrorDto error) throws IOException {
    final String url = eerpConfig.getEerpqPhenomenonCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpqPhenomenonCode.class);
  }

  public EerpqFailureCode getEerpqFailureCode(EerpqErrorDto error) throws IOException {
    final String url = eerpConfig.getEerpqFailureCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpqFailureCode.class);
  }

  public EerpqDutyCode getEerpqDutyCode(EerpqErrorDto error) throws IOException {
    final String url = eerpConfig.getEerpqDutyCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpqDutyCode.class);
  }

  public EerpqReasonCode getEerpqReasonCode(EerpqErrorDto error) throws IOException {
    final String url = eerpConfig.getEerpqReasonCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpqReasonCode.class);
  }

  public EerpqSolutionCode getEerpqSolutionCode(EerpqErrorDto error) throws IOException {
    final String url = eerpConfig.getEerpqSolutionCodeUrl();
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            url,
            HttpMethod.POST,
            getHeaderWithToken(),
            mapper.writeValueAsString(error),
            null,
            JsonNode.class);
    log.debug(LOG_RESPONSE + response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    return mapper.treeToValue(responseBody, EerpqSolutionCode.class);
  }

  private HttpHeaders getHeaderWithToken() {
    HttpHeaders headers = adapterUtil.generateHeader(MediaType.APPLICATION_JSON_UTF8);
    headers.set(EerpConstants.EERP_TOKENID, eerpConfig.getToken());
    return headers;
  }

  private void validateResponse(ResponseEntity<JsonNode> response, final String url)
      throws IOException {
    if (Objects.isNull(response)) {
      log.error(Constants.CONNECT + url + Constants.ERROR);
      throw new EerpException(Constants.CONNECT + url + Constants.ERROR);
    }
    if (HttpStatus.OK != response.getStatusCode()) {
      if (HttpStatus.INTERNAL_SERVER_ERROR == response.getStatusCode()) {
        EerpGeneralResult result = mapper.treeToValue(response.getBody(), EerpGeneralResult.class);
        throw new EerpException(result.getMessage());
      } else {
        throw new EerpException(EerpConstants.EERP_UNKNOWN);
      }
    }

    EerpGeneralResult result = mapper.treeToValue(response.getBody(), EerpGeneralResult.class);
    if (NumberUtils.INTEGER_ZERO != result.getResult()) {
      throw new EerpException(result.getMessage());
    }
  }
}
