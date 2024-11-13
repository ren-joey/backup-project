package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.TagService;
import com.delta.dms.community.swagger.controller.TagApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Tag",
    })
@RestController
public class TagController implements TagApi {
  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private TagService tagService;

  @Autowired
  public TagController(TagService tagService, HttpServletRequest request) {
    this.tagService = tagService;
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
  public ResponseBean<List<Tag>> getTags(
      @NotNull
          @ApiParam(value = "query name", required = true)
          @Valid
          @RequestParam(value = "q", required = true)
          String q,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(value = "Exclusion", required = true)
          @Valid
          @RequestParam(value = "exclude", required = true)
          List<String> exclude) {
    return new ResponseBean<>(tagService.getTags(q, limit, exclude));
  }
}
