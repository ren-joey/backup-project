package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.swagger.controller.UserApi;
import com.delta.dms.community.swagger.model.AutoCompleteUser;
import com.delta.dms.community.swagger.model.OutlookQueryData;
import com.delta.dms.community.swagger.model.OutlookQueryResult;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.UserAutocompleteQuery;
import com.delta.dms.community.swagger.model.UserQueryDto;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "User",
    })
@RestController
public class UserController implements UserApi {

  private ObjectMapper mapper = new ObjectMapper();
  private UserService userService;
  private HttpServletRequest request;

  @Autowired
  public UserController(UserService userService, HttpServletRequest request) {
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
  public ResponseBean<List<AutoCompleteUser>> searchUserByName(
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
          @ApiParam(
              value = "Whether there is avatar in the response",
              required = true,
              defaultValue = "false")
          @Valid
          @RequestParam(value = "withAvatar", required = true, defaultValue = "false")
          Boolean withAvatar,
      @ApiParam(value = "Exclusion") @Valid @RequestParam(value = "exclude", required = false)
          List<String> exclude)
      throws Exception {
    if (limit < 0) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(userService.searchUserByName(q, limit, exclude, withAvatar));
  }

  @Override
  public ResponseBean<OutlookQueryResult> searchOutlookUser(
      @ApiParam(value = "Outlook query data", required = true) @Valid @RequestBody
          OutlookQueryData body)
      throws Exception {
    return new ResponseBean<>(userService.searchOutlookUser(body));
  }

  @Override
  public ResponseBean<String> transferToOutlookUser(
      @ApiParam(value = "", required = true) @Valid @RequestBody List<String> uidList)
      throws Exception {
    return new ResponseBean<>(userService.getOutlookUserByUserId(uidList));
  }

  @Override
  public ResponseBean<List<AutoCompleteUser>> getAutocompleteUserList(
      @ApiParam(value = "") @Valid @RequestBody UserAutocompleteQuery body) throws Exception {
    if (body.getLimit() < 0) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        userService.searchUserByName(
            body.getQ(), body.getLimit(), body.getExclude(), body.isWithAvatar()));
  }

  @Override
  public ResponseBean<List<UserSession>> getUser(
      @ApiParam(value = "") @Valid @RequestBody UserQueryDto body) throws Exception {
    return new ResponseBean<>(userService.getUserByUserQuery(body));
  }
}
