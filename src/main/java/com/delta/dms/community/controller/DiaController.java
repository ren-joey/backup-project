package com.delta.dms.community.controller;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.DiaService;
import com.delta.dms.community.swagger.controller.DiaApi;
import com.delta.dms.community.swagger.model.DiaDto;
import com.delta.dms.community.swagger.model.DiaResultDto;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(
    tags = {
      "Dia",
    })
@RestController
public class DiaController implements DiaApi {
  private ObjectMapper mapper = new ObjectMapper();

  private DiaService diaService;
  private HttpServletRequest request;

  @Autowired
  public DiaController(DiaService diaService, HttpServletRequest request) {
    this.diaService = diaService;
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

  @ApiOperation(
      value = "to post innovation award data into community system",
      nickname = "createDia",
      notes = "",
      response = DiaResultDto.class)
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Success", response = DiaResultDto.class)})
  @PostMapping(
      value = "/innovation/award",
      produces = {"application/json"},
      consumes = {"application/json"})
  public DiaResultDto createDia(
      @ApiParam(value = "", required = true) @Valid @RequestBody DiaDto body) throws IOException {
    return diaService.createDia(body);
  }

  @Override
  public ResponseBean<Void> createAllDiaAttachment() throws Exception {
    diaService.createAllDiaAttachment();
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> createAllDiaTopic() throws Exception {
    diaService.createAllDiaTopic();
    return new ResponseBean<>();
  }
}
