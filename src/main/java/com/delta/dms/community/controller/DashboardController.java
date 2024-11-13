package com.delta.dms.community.controller;

import static com.delta.dms.community.utils.Constants.ERR_INVALID_PARAM;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.enums.CommunitySpecialType;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.eerp.dashboard.BaseDashboardService;
import com.delta.dms.community.service.eerp.dashboard.EerpmDashboardService;
import com.delta.dms.community.service.eerp.dashboard.EerppDashboardService;
import com.delta.dms.community.swagger.controller.DashboardApi;
import com.delta.dms.community.swagger.model.ChartDto;
import com.delta.dms.community.swagger.model.DashboardDateDto;
import com.delta.dms.community.swagger.model.EerpDashboardDto;
import com.delta.dms.community.swagger.model.EerpmDashboardDeviceDto;
import com.delta.dms.community.swagger.model.EerpmDashboardTopicDto;
import com.delta.dms.community.swagger.model.EerppDashboardDeviceDto;
import com.delta.dms.community.swagger.model.EerppDashboardTopicDto;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Dashboard",
    })
@RestController
public class DashboardController implements DashboardApi {

  private ObjectMapper mapper = new ObjectMapper();
  private CommunityService communityService;
  private EerpmDashboardService eerpmDashboardService;
  private EerppDashboardService eerppDashboardService;
  private HttpServletRequest request;

  @Autowired
  public DashboardController(
      CommunityService communityService,
      EerpmDashboardService eerpmDashboardService,
      EerppDashboardService eerppDashboardService,
      HttpServletRequest request) {
    this.communityService = communityService;
    this.eerpmDashboardService = eerpmDashboardService;
    this.eerppDashboardService = eerppDashboardService;
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
  public ResponseBean<DashboardDateDto> getDashboardDate(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    CommunityInfo community = communityService.getCommunityInfoById(communityId);
    return new ResponseBean<>(
        getServiceByType(community.getSpecialType()).getEerpDashboardDate(community));
  }

  @Override
  public ResponseBean<EerpDashboardDto> getEerpDashboard(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date)
      throws Exception {
    CommunityInfo community = communityService.getCommunityInfoById(communityId);
    return new ResponseBean<>(
        getServiceByType(community.getSpecialType()).getEerpDashboard(community, date));
  }

  @Override
  public ResponseBean<EerpmDashboardTopicDto> searchEerpmDashboardTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody Map<String, List<Object>> body,
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @ApiParam(value = "Offset", defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = false, defaultValue = "-1")
          Integer offset,
      @ApiParam(value = "Limit", defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = false, defaultValue = "-1")
          Integer limit)
      throws Exception {
    return new ResponseBean<>(
        eerpmDashboardService.searchEerpDashboardTopic(communityId, date, body, offset, limit));
  }

  @Override
  public ResponseBean<EerpmDashboardDeviceDto> searchEerpmDashboardDevice(
      @ApiParam(value = "", required = true) @Valid @RequestBody Map<String, List<Object>> body,
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @ApiParam(value = "Offset", defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = false, defaultValue = "-1")
          Integer offset,
      @ApiParam(value = "Limit", defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = false, defaultValue = "-1")
          Integer limit)
      throws Exception {
    return new ResponseBean<>(
        eerpmDashboardService.searchEerpDashboardDevice(communityId, date, body, offset, limit));
  }

  @Override
  public ResponseBean<List<ChartDto>> getEerpmDashboardDeviceHistory(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @NotNull
          @ApiParam(value = "Factory", required = true)
          @Valid
          @RequestParam(value = "factory", required = true)
          String factory,
      @NotNull
          @ApiParam(value = "Forum name", required = true)
          @Valid
          @RequestParam(value = "forum", required = true)
          String forum,
      @NotNull
          @ApiParam(value = "Device Model", required = true)
          @Valid
          @RequestParam(value = "deviceModel", required = true)
          String deviceModel,
      @NotNull
          @ApiParam(value = "Error Code", required = true)
          @Valid
          @RequestParam(value = "errorCode", required = true)
          String errorCode)
      throws Exception {
    return new ResponseBean<>(
        eerpmDashboardService.getDashboardDeviceHistory(
            communityId, date, factory, forum, deviceModel, errorCode));
  }

  @Override
  public ResponseBean<EerppDashboardTopicDto> searchEerppDashboardTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody Map<String, List<Object>> body,
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit)
      throws Exception {
    return new ResponseBean<>(
        eerppDashboardService.searchEerpDashboardTopic(communityId, date, body, offset, limit));
  }

  @Override
  public ResponseBean<EerppDashboardDeviceDto> searchEerppDashboardDevice(
      @ApiParam(value = "", required = true) @Valid @RequestBody Map<String, List<Object>> body,
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit)
      throws Exception {
    return new ResponseBean<>(
        eerppDashboardService.searchEerpDashboardDevice(communityId, date, body, offset, limit));
  }

  @Override
  public ResponseBean<List<ChartDto>> getEerppDashboardDeviceHistory(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId,
      @ApiParam(value = "", required = true) @PathVariable("date") Long date,
      @NotNull
          @ApiParam(value = "Factory", required = true)
          @Valid
          @RequestParam(value = "factory", required = true)
          String factory,
      @NotNull
          @ApiParam(value = "Forum name", required = true)
          @Valid
          @RequestParam(value = "forum", required = true)
          String forum,
      @NotNull
          @ApiParam(value = "Loss Code", required = true)
          @Valid
          @RequestParam(value = "lossCode", required = true)
          String lossCode)
      throws Exception {
    return new ResponseBean<>(
        eerppDashboardService.getDashboardDeviceHistory(
            communityId, date, factory, forum, lossCode));
  }

  @SuppressWarnings("rawtypes")
  private BaseDashboardService getServiceByType(String specialType) {
    CommunitySpecialType type = CommunitySpecialType.fromValue(specialType);
    switch (type) {
      case EERPM:
        return eerpmDashboardService;
      case EERPP:
        return eerppDashboardService;
      default:
        throw new IllegalArgumentException(ERR_INVALID_PARAM);
    }
  }
}
