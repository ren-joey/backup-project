package com.delta.dms.community.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.BasicAuthToken;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.model.SourceOsParam;

@Component
public class ThreadLocalInterceptor extends HandlerInterceptorAdapter {

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      @Nullable Exception ex)
      throws Exception {
    AcceptLanguage.unset();
    Jwt.unset();
    SortParam.unset();
    SourceOsParam.unset();
    BasicAuthToken.unset();
  }
}
