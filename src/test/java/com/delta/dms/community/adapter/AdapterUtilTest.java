package com.delta.dms.community.adapter;

import java.net.URI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.delta.dms.community.model.Jwt;
import com.delta.set.utils.RequestId;

public class AdapterUtilTest {

  private static final int TIMEOUT = 60000;

  @Mock private RestTemplate restTemplate;
  @Mock private RestTemplateBuilder restTemplateBuilder;
  private AdapterUtil adapterUtil;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(restTemplateBuilder.setConnectTimeout(TIMEOUT)).thenReturn(restTemplateBuilder);
    Mockito.when(restTemplateBuilder.setReadTimeout(TIMEOUT)).thenReturn(restTemplateBuilder);
    Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    Mockito.when(restTemplateBuilder.setConnectTimeout(TIMEOUT * 2))
        .thenReturn(restTemplateBuilder);
    Mockito.when(restTemplateBuilder.setReadTimeout(TIMEOUT * 2)).thenReturn(restTemplateBuilder);
    Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    adapterUtil = new AdapterUtil(restTemplateBuilder);
  }

  @Test
  public void whenSendRequest_thenRestTemplateExchangeOnce() {
    Jwt.set("");
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add("TEST", "TEST");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    httpHeaders.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    URI uri = adapterUtil.buildUri("TESTURL", uriVariables);
    adapterUtil.sendRequest("TESTURL", HttpMethod.GET, null, null, uriVariables, String.class);
    Mockito.verify(restTemplate)
        .exchange(uri, HttpMethod.GET, new HttpEntity<>(null, httpHeaders), String.class);
  }

  @Test
  public void givenInvalidJWT_whenSendRequest_thenReturnNull() {
    Jwt.set("\n");
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add("TEST", "TEST");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    httpHeaders.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    URI uri = adapterUtil.buildUri("TESTURL", uriVariables);
    ResponseEntity<String> result =
        adapterUtil.sendRequest("TESTURL", HttpMethod.GET, null, null, uriVariables, String.class);
    Mockito.verify(restTemplate, Mockito.times(0))
        .exchange(uri, HttpMethod.GET, new HttpEntity<>(null, httpHeaders), String.class);
    Assert.assertNull(result);
  }

  @Test
  public void givenRestTemplateExecuteWithException_whenSendRequest_thenReturnNull() {
    Jwt.set("");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    httpHeaders.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    URI uri = adapterUtil.buildUri("TESTURL", null);
    Mockito.when(
            restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>("TEST", httpHeaders), String.class))
        .thenThrow(new RestClientException("ERROR"));
    Assert.assertNull(
        adapterUtil.sendRequest(
            "TESTURL", HttpMethod.GET, httpHeaders, "TEST", null, String.class));
  }

  @Test
  public void givenWithNullUriVariables_whenbuildUri_thenReturnOnlyUrl() {
    URI result = adapterUtil.buildUri("TESTURL", null);
    Assert.assertEquals(UriComponentsBuilder.fromUriString("TESTURL").build().toUri(), result);
  }

  @Test
  public void givenWithEmptyUriVariables_whenbuildUri_thenReturnOnlyUrl() {
    URI result = adapterUtil.buildUri("TESTURL", new LinkedMultiValueMap<>());
    Assert.assertEquals(UriComponentsBuilder.fromUriString("TESTURL").build().toUri(), result);
  }

  @Test
  public void givenWithUriVariables_whenbuildUri_thenReturnUrl() {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add("TESTVAR", "TESTVAR");
    URI result = adapterUtil.buildUri("TESTURL", uriVariables);
    Assert.assertEquals("TESTURL?TESTVAR=TESTVAR", result.toString());
  }

  @Test
  public void givenWithUriVariables_whenbuildUri_thenReturnEncodedUrl() {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add("TESTVAR", "+TESTVAR");
    URI result = adapterUtil.buildUri("TESTURL", uriVariables);
    Assert.assertEquals("TESTURL?TESTVAR=%2BTESTVAR", result.toString());
  }

  @Test
  public void
      givenNullResponseErrorHandler_whenSendRequest_thenAddIgnoreResponseErrorHandlerAndExecute() {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add("TEST", "TEST");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    httpHeaders.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    URI uri = adapterUtil.buildUri("TESTURL", uriVariables);
    adapterUtil.sendRequest("TESTURL", HttpMethod.GET, null, null, uriVariables, String.class);
    Mockito.verify(restTemplate)
        .setErrorHandler(ArgumentMatchers.any(IgnoreResponseErrorHandler.class));
    Mockito.verify(restTemplate)
        .exchange(uri, HttpMethod.GET, new HttpEntity<>(null, httpHeaders), String.class);
  }

  @Test
  public void givenNullResponse_whenGetResponseBody_thenReturnNull() {
    Assert.assertNull(adapterUtil.getResponseBody(null));
  }

  @Test
  public void givenNon200StatusCode_whenGetResponseBody_thenReturnNull() {
    Assert.assertNull(adapterUtil.getResponseBody(new ResponseEntity<>(HttpStatus.BAD_REQUEST)));
  }

  @Test
  public void whenGetResponseBody_thenReturnResult() {
    Assert.assertEquals(
        "TEST", adapterUtil.getResponseBody(new ResponseEntity<>("TEST", HttpStatus.OK)));
    ;
  }
}
