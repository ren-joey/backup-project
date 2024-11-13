package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.InfoService;
import com.delta.dms.community.swagger.controller.InfoApi;
import com.delta.dms.community.swagger.model.Info;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;

@Api(
    tags = {
      "Info",
    })
@RestController
public class InfoController implements InfoApi {

  private ObjectMapper mapper = new ObjectMapper();

  private InfoService infoService;
  private HttpServletRequest request;

  @Autowired
  public InfoController(InfoService infoService, HttpServletRequest request) {
    this.infoService = infoService;
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
  public ResponseBean<Info> getInfo() throws Exception {
    return new ResponseBean<>(infoService.getInfo());
  }
}
