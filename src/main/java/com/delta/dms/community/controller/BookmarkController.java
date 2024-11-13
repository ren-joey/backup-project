package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.swagger.controller.BookmarkApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.TopicResultOfBookmark;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Bookmark",
    })
@RestController
public class BookmarkController implements BookmarkApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private TopicService topicService;

  @Autowired
  public BookmarkController(TopicService topicService, HttpServletRequest request) {
    this.topicService = topicService;
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
  public ResponseBean<TopicResultOfBookmark> getTopicListOfBookmark(
      @ApiParam(value = "Id of the user", required = true) @PathVariable("userId") String userId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value = "Sort:   * -bookmarkCreateTime - Sort by bookmark Create Time. Descending. ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort)
      throws Exception {
    return new ResponseBean<>(
        topicService.searchTopicListOfUser(userId, offset, limit, SortParam.get()));
  }
}
