package com.delta.dms.community.interceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.service.AuthService;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.JwtParser;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;

@Component
public class AuthenticateInterceptor extends HandlerInterceptorAdapter {

  private AuthService authService;
  private JwtParser parser;
  private LogUtil log = LogUtil.getInstance();

  @Autowired
  public AuthenticateInterceptor(AuthService authService) {
    this.authService = authService;
    parser = JwtParser.instance;
  }

  public void setParser(JwtParser parser) {
    this.parser = parser;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    Cookie cookie = Utility.getRequestCookie(Utility.COOKIE_NAME_DMS_JWT);
    if (null == cookie) {
      throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }
    Jwt.set(cookie.getValue());
    try {
      String userId = parser.parseUserId(cookie.getValue());
      authService.renewJwtCookiebyToken(cookie.getValue());
      UserSession userSession = Utility.getUserFromSession();
      log.debug("intercept: " + userSession.getCommonUUID() + " " + userId);
      if (!userId.equals(userSession.getCommonUUID())) {
        authService.setSessionInfoByUserId(userId);
      }
      return true;
    } catch (Exception e) {
      log.error(e);
      throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }
  }
}
