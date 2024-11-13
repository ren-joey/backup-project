package com.delta.dms.community.adapter;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.filter.SourceOsParamFilter;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.model.SourceOsParam;
import com.delta.dms.community.model.search.SearchRequest;
import com.delta.dms.community.swagger.model.CommunityResultList;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JarvisAdapter {

  private static final String FILTER_QUERY_DELIMITER = "=";
  private static final String SEARCH_QUERY_PARAM_Q = "q";
  private static final String SEARCH_QUERY_PARAM_PITEM = "pitem";
  private static final String SEARCH_QUERY_PARAM_PAGE = "page";
  private static final String SEARCH_QUERY_PARAM_FROM = "from";
  private static final String SEARCH_QUERY_PARAM_SORT = "sort";
  private static final String SEARCH_QUERY_PARAM_FILTER = "filter";
  private static final String SEARCH_QUERY_PARAM_SEARCHACTIVITY = "searchActivity";

  private static final String JARVIS_CONTEXT_ROOT = "/dmsjarvis";
  private static final String JARVIS_SEARCH_COMMUNITY_API_URI = "/v2/search/community/listsearch";
  private static final String JARVIS_SEARCH_COMMUNITY_CATEGORY_API_URI =
      "/search/community/categorysearch";
  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private AdapterUtil adapterUtil;
  private YamlConfig yamlConfig;

  @Autowired
  public JarvisAdapter(AdapterUtil adapterUtil, YamlConfig yamlConfig) {
    this.adapterUtil = adapterUtil;
    this.yamlConfig = yamlConfig;
  }

  public CommunityResultList searchCommunity(SearchRequest searchRequest) {
    MultiValueMap<String, String> uriVariables = buildUriVariables(searchRequest);
    String url = getSearchCommunityApiUrl(JARVIS_SEARCH_COMMUNITY_API_URI);
    HttpHeaders headers = adapterUtil.generateHeaderWithCookies();
    headers.set(SourceOsParamFilter.SOURCE_OS_PARAM_FIELD, SourceOsParam.get().toString());
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(url, HttpMethod.GET, headers, null, uriVariables, JsonNode.class);
    return getResponseData(
        response,
        new CommunityResultList().result(Collections.emptyList()),
        CommunityResultList.class);
  }

  private <T> T getResponseData(
      ResponseEntity<JsonNode> response, T defaultValue, Class<T> responseType) {
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (Objects.isNull(responseBody)) {
      return defaultValue;
    }
    JsonNode responseData = responseBody.path(Constants.RESPONSE_DATA);
    if (responseData.isMissingNode() || responseData.isNull()) {
      return defaultValue;
    }
    try {
      return mapper.treeToValue(responseData, responseType);
    } catch (JsonProcessingException e) {
      log.error(e);
      return defaultValue;
    }
  }

  private MultiValueMap<String, String> buildUriVariables(SearchRequest searchRequest) {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    Optional.ofNullable(searchRequest.getQuery())
        .ifPresent(q -> uriVariables.add(SEARCH_QUERY_PARAM_Q, q));
    Optional.ofNullable(searchRequest.getPage())
        .ifPresent(page -> uriVariables.add(SEARCH_QUERY_PARAM_PAGE, String.valueOf(page)));
    Optional.ofNullable(searchRequest.getPitem())
        .ifPresent(pitem -> uriVariables.add(SEARCH_QUERY_PARAM_PITEM, String.valueOf(pitem)));
    Optional.ofNullable(searchRequest.getSort())
        .ifPresent(sort -> uriVariables.add(SEARCH_QUERY_PARAM_SORT, buildSortParam(sort)));
    Optional.ofNullable(searchRequest.getSearchActivity())
        .ifPresent(activity -> uriVariables.add(SEARCH_QUERY_PARAM_SEARCHACTIVITY, activity));
    Optional.ofNullable(searchRequest.getFrom())
        .ifPresent(from -> uriVariables.add(SEARCH_QUERY_PARAM_FROM, from));
    Optional.ofNullable(searchRequest.getFilterMap())
        .ifPresent(
            map ->
                map.entrySet()
                    .forEach(
                        entry ->
                            entry
                                .getValue()
                                .stream()
                                .filter(Objects::nonNull)
                                .filter(v -> !v.isEmpty())
                                .forEach(
                                    v ->
                                        uriVariables.add(
                                            SEARCH_QUERY_PARAM_FILTER,
                                            buildFilterParam(entry.getKey(), v)))));
    return uriVariables;
  }

  private String buildSortParam(Order order) {
    return new StringBuilder(order.isAscending() ? SortParam.SORT_ASC : SortParam.SORT_DESC)
        .append(order.getProperty())
        .toString();
  }

  private String buildFilterParam(String key, String value) {
    return new StringBuilder(key).append(FILTER_QUERY_DELIMITER).append(value).toString();
  }

  private String getSearchCommunityApiUrl(String apiUri) {
    return new StringBuilder(yamlConfig.getHost())
        .append(JARVIS_CONTEXT_ROOT)
        .append(apiUri)
        .toString();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Integer> searchCommunityCategory(SearchRequest searchRequest) {
    MultiValueMap<String, String> uriVariables = buildUriVariables(searchRequest);
    String url = getSearchCommunityApiUrl(JARVIS_SEARCH_COMMUNITY_CATEGORY_API_URI);
    HttpHeaders headers = adapterUtil.generateHeaderWithCookies();
    headers.set(SourceOsParamFilter.SOURCE_OS_PARAM_FIELD, SourceOsParam.get().toString());
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(url, HttpMethod.GET, headers, null, uriVariables, JsonNode.class);
    return getResponseData(response, Collections.emptyMap(), Map.class);
  }
}
