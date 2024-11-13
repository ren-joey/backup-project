package com.delta.dms.community.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.DeltaPointServiceVer2;
import com.delta.dms.community.swagger.controller.DeltaPointApi;
import com.delta.dms.community.swagger.model.DeltaPointInfo;
import com.delta.dms.community.swagger.model.DeltaPointLineChart;
import com.delta.dms.community.swagger.model.DeltaPointType;
import com.delta.dms.community.swagger.model.ResponseBean;
import io.swagger.annotations.Api;

@Api(
    tags = {
      "DeltaPoint",
    })
@RestController
public class DeltaPointController implements DeltaPointApi {
  private DeltaPointServiceVer2 deltaPointService;

  @Autowired
  public DeltaPointController(DeltaPointServiceVer2 deltaPointService) {
    this.deltaPointService = deltaPointService;
  }

  @Override
  public ResponseBean<List<DeltaPointInfo>> getDeltaPointResult(
      @PathVariable("userId") String userId) throws Exception {
    return new ResponseBean<>(
        deltaPointService.getDeltaPointByTimePeriod(userId, 0, System.currentTimeMillis(), false));
  }

  @Override
  public ResponseBean<DeltaPointLineChart> getDeltaPointLineChart(
      @PathVariable("userId") String userId) throws Exception {
    return new ResponseBean<>(deltaPointService.getLineChartDP(userId));
  }

  @Override
  public ResponseBean<List<DeltaPointInfo>> getDeltaPointDetail(
      @PathVariable("userId") String userId,
      @RequestParam(value = "type", required = true, defaultValue = "threeMonths") String type)
      throws Exception {
    return new ResponseBean<>(
        deltaPointService.getDeltaPointDetail(userId, DeltaPointType.fromValue(type)));
  }
}
