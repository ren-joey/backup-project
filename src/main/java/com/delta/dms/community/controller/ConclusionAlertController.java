package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.ConclusionAlertService;
import com.delta.dms.community.swagger.controller.ConclusionAlertApi;
import com.delta.dms.community.swagger.model.ConclusionAlertDetail;
import com.delta.dms.community.swagger.model.ConclusionAlertGroupDetail;
import com.delta.dms.community.swagger.model.ConclusionAlertMember;
import com.delta.dms.community.swagger.model.ConclusionAlertRuleDetail;
import com.delta.dms.community.swagger.model.AlertRuleType;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "ConclusionAlert",
    })
@RestController
public class ConclusionAlertController implements ConclusionAlertApi {

  private ObjectMapper mapper = new ObjectMapper();

  private ConclusionAlertService conclusionAlertService;
  private HttpServletRequest request;

  @Autowired
  public ConclusionAlertController(
      ConclusionAlertService conclusionAlertService, HttpServletRequest request) {
    this.conclusionAlertService = conclusionAlertService;
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
  public ResponseBean<ConclusionAlertDetail> getForumConclusionAlert(
      @ApiParam(value = "", required = true) @PathVariable("forumId") Integer forumId)
      throws Exception {
    return new ResponseBean<>(conclusionAlertService.getForumConclusionAlert(forumId));
  }

  @Override
  public ResponseBean<Void> upsertForumConclusionAlertGroup(
      @ApiParam(value = "", required = true) @Valid @RequestBody ConclusionAlertGroupDetail body,
      @ApiParam(value = "", required = true) @PathVariable("forumId") Integer forumId)
      throws Exception {
    conclusionAlertService.upsertForumConclusionAlertGroup(forumId, body);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> upsertForumConclusionAlertRule(
      @ApiParam(value = "", required = true) @Valid @RequestBody ConclusionAlertRuleDetail body,
      @ApiParam(value = "", required = true) @PathVariable("forumId") Integer forumId,
      @ApiParam(value = "", required = true) @PathVariable("ruleType")
      AlertRuleType ruleType)
      throws Exception {
    conclusionAlertService.upsertForumConclusionAlertRule(forumId, ruleType, body);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<List<ConclusionAlertMember>> searchMemberByName(
      @ApiParam(value = "", required = true) @PathVariable("forumId") Integer forumId,
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
          @ApiParam(value = "", required = true)
          @Valid
          @RequestParam(value = "withGroup", required = true)
          Boolean withGroup)
      throws Exception {
    return new ResponseBean<>(
        conclusionAlertService.searchMemberByName(forumId, q, limit, withGroup));
  }

  @Override
  public ResponseBean<Void> alertAllUnconcludedTopics() throws Exception {
    conclusionAlertService.alertAllUnconcludedTopics();
    return new ResponseBean<>();
  }
}
