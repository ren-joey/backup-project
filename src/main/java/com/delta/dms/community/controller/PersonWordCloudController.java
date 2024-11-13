package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.TextService;
import com.delta.dms.community.swagger.controller.PersonwordcloudApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "PersonWordCloud",
    })
@RestController
public class PersonWordCloudController implements PersonwordcloudApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private TextService textService;

  @Autowired
  public PersonWordCloudController(TextService textService, HttpServletRequest request) {
    this.textService = textService;
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
  public ResponseBean<String> getPersonWordCloud(
      @NotNull
          @ApiParam(value = "userId", required = true)
          @Valid
          @RequestParam(value = "id", required = true)
          String id,
      @NotNull
          @ApiParam(value = "topn", required = true)
          @Valid
          @RequestParam(value = "topn", required = true)
          Integer topn,
      @NotNull
          @ApiParam(value = "system language", required = true)
          @Valid
          @RequestParam(value = "lang", required = true)
          String lang)
      throws Exception {
    return new ResponseBean<>(textService.getPersonWordCloud(id, topn, lang));
  }
}
