package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.service.ddf.DdfQueueService;
import com.delta.dms.community.swagger.controller.DdfApi;
import com.delta.dms.community.swagger.model.DdfQueueAction;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Ddf",
    })
@RestController
public class DdfController implements DdfApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private DdfQueueService ddfQueueService;
  private UserService userService;

  @Autowired
  public DdfController(
      DdfQueueService ddfQueueService, UserService userService, HttpServletRequest request) {
    this.ddfQueueService = ddfQueueService;
    this.userService = userService;
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
  public ResponseBean<Void> syncDdf(
      @NotNull
          @ApiParam(value = "Action", required = true)
          @Valid
          @RequestParam(value = "action", required = true)
          DdfQueueAction action)
      throws Exception {
    validatePrivilege();
    switch (action) {
      case UPDATE:
        ddfQueueService.upsertDdfs();
        break;
      case DELETE:
        ddfQueueService.deleteDdfs();
        break;
      default:
        break;
    }
    return new ResponseBean<>();
  }

  private void validatePrivilege() {
    if (!userService.isSysAdmin()) {
      throw new UnauthorizedException("only admin can generate ddf");
    }
  }
}
