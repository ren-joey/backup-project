package com.delta.dms.community.adapter;

import java.io.IOException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class IgnoreResponseErrorHandler implements ResponseErrorHandler {
  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    return false;
  }

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    // don't do anything when calling API failed.
  }
}
