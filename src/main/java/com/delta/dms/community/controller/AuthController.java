package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.AuthService;
import com.delta.dms.community.service.DeviceService;
import com.delta.dms.community.swagger.controller.AuthApi;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import com.delta.dms.community.swagger.model.DeviceInfo;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.swagger.model.AppInfo;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Auth",
    })
@RestController
public class AuthController implements AuthApi {

  private ObjectMapper mapper = new ObjectMapper();

  private AuthService authService;
  private DeviceService deviceService;
  private HttpServletRequest request;

  @Autowired
  public AuthController(
      AuthService authService, DeviceService deviceService, HttpServletRequest request) {
    this.authService = authService;
    this.deviceService = deviceService;
    this.request = request;
  }

  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return Optional.ofNullable(mapper);
  }

  @Override
  public Optional<HttpServletRequest> getRequest() {
    return Optional.ofNullable(request);
  }

  @Override
  public ResponseBean<UserSession> login(@ApiParam(value = "Username and Password" ,required=true ) @Valid @RequestBody AuthenticationToken json,
                                         @ApiParam(value = "") @RequestHeader(value="Source-OS", required=false) String sourceOS) 
                                         throws Exception {
    ResponseBean<UserSession> res = new ResponseBean<>();
    UserSession userSession = authService.login(json, sourceOS);
    res.setData(userSession);
    res.setMessage(Constants.LOGIN_SUCCESS);
    return res;
  }

  @Override
  public ResponseBean<Void> logout(
      @ApiParam(value = "Device token", defaultValue = "")
          @Valid
          @RequestParam(value = "token", required = false, defaultValue = "")
          String token)
      throws Exception {
    HttpSession session = request.getSession(false);
    Utility.setResponseCookie(Utility.COOKIE_NAME_DMS_JWT, "", 0);
    if (session != null) {
      session.invalidate();
    }
    deviceService.deleteBadDeviceToken(token);
    request.logout();
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<UserSession> checkLogin(
      @NotNull
          @ApiParam(value = "Refresh", required = true)
          @Valid
          @RequestParam(value = "refresh", required = true)
          Boolean refresh)
      throws Exception {
    ResponseBean<UserSession> res = new ResponseBean<>();
    if (refresh.booleanValue()) {
      res.setData(authService.getCurrentUserInfo());
    }
    return res;
  }

  @Override
  public ResponseBean<Void> registerDeviceToken(
      @ApiParam(value = "Device token", required = true) @Valid @RequestBody DeviceInfo json)
      throws Exception {
    authService.registerDeviceToken(json);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<AppInfo> appInfo() throws Exception {
    ResponseBean<AppInfo> res = new ResponseBean<>();
    res.setData(authService.getCurrentUserAppInfo());
    return res;
  }

}
