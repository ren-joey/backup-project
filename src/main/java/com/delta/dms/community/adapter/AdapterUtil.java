package com.delta.dms.community.adapter;

import java.net.URI;
import java.util.Objects;
import javax.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.delta.set.utils.RequestId;

@Component
public class AdapterUtil {

  private static final int TIMEOUT = 60000;
  private static final String PLUS_SIGN = "+";
  private static final String PLUS_SIGN_ENCODED = "%2B";

  private LogUtil log = LogUtil.getInstance();
  private RestTemplate defaultRestTemplate;
  private RestTemplate longTimeoutRestTemplate;

  @Autowired
  public AdapterUtil(RestTemplateBuilder restTemplateBuilder) {
    this.defaultRestTemplate =
        restTemplateBuilder.setConnectTimeout(TIMEOUT).setReadTimeout(TIMEOUT).build();
    this.longTimeoutRestTemplate =
        restTemplateBuilder.setConnectTimeout(TIMEOUT).setReadTimeout(TIMEOUT * 2).build();
  }

  public RestTemplate restTemplate() {
    return this.defaultRestTemplate;
  }

  public <T, R> ResponseEntity<T> sendRequest(
      String url,
      HttpMethod method,
      HttpHeaders httpHeaders,
      R request,
      MultiValueMap<String, String> uriVariables,
      Class<T> responseType) {
    return this.sendRequest(
        url, method, httpHeaders, request, uriVariables, responseType, defaultRestTemplate);
  }

  public <T, R> ResponseEntity<T> sendRequestWithLongTimeout(
      String url,
      HttpMethod method,
      HttpHeaders httpHeaders,
      R request,
      MultiValueMap<String, String> uriVariables,
      Class<T> responseType) {
    return this.sendRequest(
        url, method, httpHeaders, request, uriVariables, responseType, longTimeoutRestTemplate);
  }

  /**
   * send request for json response
   *
   * @param url - the URL
   * @param method - the HTTP method (GET, POST, etc)
   * @param httpHeaders - header data
   * @param request - request body
   * @param uriVariables - the variables to expand in the template, null if no parameter needed
   * @param responseType - expected response type
   * @return ResponseEntity - response contains body as expected type, return null if fail
   */
  @Retryable(include = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
  private <T, R> ResponseEntity<T> sendRequest(
      String url,
      HttpMethod method,
      HttpHeaders httpHeaders,
      R request,
      MultiValueMap<String, String> uriVariables,
      Class<T> responseType,
      RestTemplate restTemplate) {
    try {
      URI uri = buildUri(url, uriVariables);
      httpHeaders = getHttpHeaders(httpHeaders);
      HttpEntity<R> entity = new HttpEntity<>(request, httpHeaders);
      setErrorHandler(restTemplate, new IgnoreResponseErrorHandler());
      return restTemplate.exchange(uri, method, entity, responseType);
    } catch (RestClientException e) {
      log.error(e);
      return null;
    }
  }

  @Retryable(include = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
  public <T, R> ResponseEntity<T> sendRequestWithCustomHeader(
          String url,
          HttpMethod method,
          R requestBody,
          MultiValueMap<String, String> uriVariables,
          HttpHeaders headers,
          Class<T> responseType) {
    try {
      URI uri = buildUri(url, uriVariables);
      HttpEntity<R> entity = new HttpEntity<>(requestBody, headers);
      setErrorHandler(defaultRestTemplate, new IgnoreResponseErrorHandler());
      return defaultRestTemplate.exchange(uri, method, entity, responseType);
    } catch (RestClientException e) {
      log.error(e);
      return null;
    }
  }

  @Recover
  public <T> ResponseEntity<T> recover(Exception e) {
    log.error(e);
    return null;
  }

  private HttpHeaders getHttpHeaders(HttpHeaders httpHeaders) {
    if (Objects.isNull(httpHeaders)) {
      httpHeaders = generateHeader(MediaType.APPLICATION_JSON_UTF8);
    }
    httpHeaders.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    if (Jwt.get().matches(Constants.LINEBREAK_TAB_REGEX)) {
      throw new RestClientException(Constants.LINEBREAK_TAB_REGEX);
    } else {
      httpHeaders.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    }
    return httpHeaders;
  }

  /**
   * generate request header
   *
   * @param mediaType Media type
   * @return HttpHeaders - header for request
   */
  public HttpHeaders generateHeader(MediaType mediaType) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(mediaType);
    return httpHeaders;
  }

  public HttpHeaders generateHeaderWithCookies() {
    return generateHeaderWithCookies(MediaType.APPLICATION_JSON_UTF8);
  }

  public HttpHeaders generateHeaderWithCookies(MediaType mediaType) {
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    Cookie[] cookies = attr.getRequest().getCookies();

    HttpHeaders headers = generateHeader(mediaType);
    if (Objects.nonNull(cookies)) {
      StringBuilder sb = new StringBuilder();
      for (Cookie cookie : cookies) {
        sb.append(cookie.getName())
            .append(Constants.EQUAL)
            .append(cookie.getValue())
            .append(Constants.SEMICOLON);
      }
      headers.add(HttpHeaders.COOKIE, sb.toString());
    }
    headers.set(HttpHeaders.ACCEPT_LANGUAGE, AcceptLanguage.get());
    return headers;
  }
  
  public HttpHeaders generateHeaderWithJwt(String jwt) {
    HttpHeaders headers = generateHeader(MediaType.APPLICATION_JSON_UTF8);
    StringBuilder sb = new StringBuilder();
    sb.append(Utility.COOKIE_NAME_DMS_JWT).append("=").append(jwt).append(Constants.SEMICOLON);
    headers.add(Constants.HEADER_COOKIE, sb.toString());
    return headers;
  }

  /**
   * build uri path
   *
   * @param url - the URL
   * @param uriVariables - the variables to expand in the template, null if no parameter needed
   * @return URI - uri path for request
   */
  public URI buildUri(String url, MultiValueMap<String, String> uriVariables) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
    if (!CollectionUtils.isEmpty(uriVariables)) {
      builder.queryParams(uriVariables);
    }
    URI uri = builder.build().encode().toUri();
    String plusSignEscapeQuery =
        StringUtils.replace(uri.getRawQuery(), PLUS_SIGN, PLUS_SIGN_ENCODED);
    return UriComponentsBuilder.fromUri(uri).replaceQuery(plusSignEscapeQuery).build(true).toUri();
  }

  public <T> T getResponseBody(ResponseEntity<T> response) {
    if (null == response || response.getStatusCode() != HttpStatus.OK) {
      return null;
    }
    return response.getBody();
  }

  private void setErrorHandler(
      RestTemplate restTemplate, ResponseErrorHandler responseErrorHandler) {
    if (Objects.nonNull(responseErrorHandler)) {
      restTemplate.setErrorHandler(responseErrorHandler);
    } else {
      restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    }
  }
}
