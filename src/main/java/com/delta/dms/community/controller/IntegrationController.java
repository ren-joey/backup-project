package com.delta.dms.community.controller;

import com.delta.dms.community.config.DrcSyncConfig;
import com.delta.dms.community.config.IntegrationConfig;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.IntegrationService;
import com.delta.dms.community.swagger.controller.IntegrationApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Api(
    tags = {
      "Integration",
    })
@RestController
public class IntegrationController implements IntegrationApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private IntegrationService integrationService;
  private final HttpServletResponse response;
  private final IntegrationConfig integrationConfig;

  @Autowired
  public IntegrationController(
          IntegrationService integrationService,
          HttpServletRequest request,
          HttpServletResponse response,
          IntegrationConfig integrationConfig) {
    this.integrationService = integrationService;
    this.request = request;
    this.response = response;
    this.integrationConfig = integrationConfig;
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
  public ResponseBean<Integer> createCommunityAppGroup() throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.createCommunityAppGroup());
  }

  @Override
  public ResponseBean<Integer> integrateCommunityRole() throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.integrateCommunityRole());
  }

  @Override
  public ResponseBean<Integer> createForumAppGroup() throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.createForumAppGroup());
  }

  @Override
  public ResponseBean<Integer> integrateForumRole() throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.integrateForumRole());
  }

  @Override
  public ResponseBean<Integer> integrateDdf() throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.integrateDdf());
  }

  @Override
  public ResponseBean<Integer> integrateOrgCommunity(
          @NotNull @ApiParam(value = "", required = true, defaultValue = "-1")
          @Valid @RequestParam(value = "startTime", required = true, defaultValue="-1") Long startTime,
          @ApiParam(value = "") @Valid @RequestParam(value = "endTime", required = false) Long endTime)
          throws Exception {
    validatePrivilege();
    return new ResponseBean<>(integrationService.integrateOrgCommunity(startTime, endTime));
  }

  private void validatePrivilege() {
    List<String> validUserIdList = Collections.singletonList(integrationConfig.getAuthorizedUid());
    if (!validUserIdList.contains(Utility.getUserIdFromSession())) {
      throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }
  }
}
