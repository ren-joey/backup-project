package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.PrivilegedCommunityForumService;
import com.delta.dms.community.swagger.controller.PrivilegedCommunityForumApi;
import com.delta.dms.community.swagger.model.PrivilegedCommunityForum;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "PrivilegedCommunityForum",
    })
@RestController
public class PrivilegeCommunityForumController implements PrivilegedCommunityForumApi {

  private HttpServletRequest request;
  private ObjectMapper mapper = new ObjectMapper();
  private PrivilegedCommunityForumService privilegedCommunityForumService;

  @Autowired
  public PrivilegeCommunityForumController(
      PrivilegedCommunityForumService privilegeCommunityForumService, HttpServletRequest request) {
    this.privilegedCommunityForumService = privilegeCommunityForumService;
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

  /*
   * This returned forumName is customized for PQM requirement to display in the drop-down list on
   * 2019-04-12.
   */
  @Override
  public ResponseBean<List<PrivilegedCommunityForum>> getPrivilegedCommunityForumIds(
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit)
      throws Exception {
    return new ResponseBean<>(
        privilegedCommunityForumService.getPrivilegeCommunityForumIds(offset, limit));
  }
}
