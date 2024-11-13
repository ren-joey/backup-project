package com.delta.dms.community.interceptor;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.delta.dms.community.model.BasicAuthToken;

@Component
public class BasicAuthInterceptor extends HandlerInterceptorAdapter {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String authorization =
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
            .orElseGet(() -> StringUtils.EMPTY);
    if (!authorization
        .substring(NumberUtils.INTEGER_ZERO, HttpServletRequest.BASIC_AUTH.length())
        .equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
      throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }
    BasicAuthToken.set(authorization.substring(HttpServletRequest.BASIC_AUTH.length()).trim());
    return true;
  }
}
