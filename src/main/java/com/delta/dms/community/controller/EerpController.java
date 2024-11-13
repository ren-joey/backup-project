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
import com.delta.dms.community.service.eerp.EerpService;
import com.delta.dms.community.service.eerp.report.EerpReportFactory;
import com.delta.dms.community.service.eerp.topic.EerpTopicService;
import com.delta.dms.community.swagger.controller.EerpApi;
import com.delta.dms.community.swagger.model.EerpTopicCreationData;
import com.delta.dms.community.swagger.model.EerpTopicData;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.swagger.model.EerpmErrorCauseDto;
import com.delta.dms.community.swagger.model.EerpmErrorCodeDto;
import com.delta.dms.community.swagger.model.EerpmErrorSolutionDto;
import com.delta.dms.community.swagger.model.EerpqCodeDto;
import com.delta.dms.community.swagger.model.EerpqCodeType;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Eerp",
    })
@RestController
public class EerpController implements EerpApi {

  private ObjectMapper mapper = new ObjectMapper();
  private EerpService eerpService;
  private EerpTopicService eerpTopicService;
  private EerpReportFactory reportFactory;
  private HttpServletRequest request;

  @Autowired
  public EerpController(
      EerpService eerpService,
      EerpTopicService eerpTopicService,
      EerpReportFactory reportFactory,
      HttpServletRequest request) {
    this.eerpService = eerpService;
    this.eerpTopicService = eerpTopicService;
    this.reportFactory = reportFactory;
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
  public ResponseBean<Integer> createEerpTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody EerpTopicCreationData body)
      throws Exception {
    return new ResponseBean<>(eerpTopicService.createTopic(body));
  }

  @Override
  public ResponseBean<Void> updateEerpTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody EerpTopicData body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    eerpTopicService.updateTopic(topicId, body);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<List<EerpmErrorCodeDto>> getEerpmErrorCode(
      @NotNull
          @ApiParam(value = "Device Model", required = true)
          @Valid
          @RequestParam(value = "deviceModel", required = true)
          String deviceModel)
      throws Exception {
    return new ResponseBean<>(eerpService.getEerpmErrorCode(deviceModel));
  }

  @Override
  public ResponseBean<List<EerpmErrorCauseDto>> getEerpmErrorCause(
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
    return new ResponseBean<>(eerpService.getEerpmErrorCause(deviceModel, errorCode));
  }

  @Override
  public ResponseBean<List<EerpmErrorSolutionDto>> getEerpmErrorSolution(
      @NotNull
          @ApiParam(value = "Device Model", required = true)
          @Valid
          @RequestParam(value = "deviceModel", required = true)
          String deviceModel,
      @NotNull
          @ApiParam(value = "Error Code", required = true)
          @Valid
          @RequestParam(value = "errorCode", required = true)
          String errorCode,
      @NotNull
          @ApiParam(value = "Error Cause", required = true)
          @Valid
          @RequestParam(value = "errorCause", required = true)
          String errorCause)
      throws Exception {
    return new ResponseBean<>(
        eerpService.getEerpmErrorSolution(deviceModel, errorCode, errorCause));
  }

  @Override
  public ResponseBean<EerpqCodeDto> getEerpqCode(
      @NotNull
          @ApiParam(value = "", required = true)
          @Valid
          @RequestParam(value = "type", required = true)
          EerpqCodeType type,
      @NotNull
          @ApiParam(value = "", required = true)
          @Valid
          @RequestParam(value = "factory", required = true)
          String factory,
      @ApiParam(value = "", defaultValue = "-1")
          @Valid
          @RequestParam(value = "pageNum", required = false, defaultValue = "-1")
          Integer pageNum,
      @ApiParam(value = "", defaultValue = "-1")
          @Valid
          @RequestParam(value = "pageSize", required = false, defaultValue = "-1")
          Integer pageSize,
      @ApiParam(value = "") @Valid @RequestParam(value = "phenomenonCode", required = false)
          String phenomenonCode,
      @ApiParam(value = "") @Valid @RequestParam(value = "dutyCode", required = false)
          String dutyCode)
      throws Exception {
    return new ResponseBean<>(
        eerpService.getEerpqCode(type, pageNum, pageSize, factory, phenomenonCode, dutyCode));
  }

  @Override
  public ResponseBean<Void> generateEerpConclusionReport(
      @NotNull
          @ApiParam(value = "", required = true)
          @Valid
          @RequestParam(value = "type", required = true)
          EerpType type,
      @NotNull
          @ApiParam(value = "", required = true)
          @Valid
          @RequestParam(value = "endTime", required = true)
          Long endTime,
      @ApiParam(value = "", defaultValue = "-1")
          @Valid
          @RequestParam(value = "startTime", required = false, defaultValue = "-1")
          Long startTime)
      throws Exception {
    reportFactory.getService(type).generateConclusionReportToMyDms(startTime, endTime);
    return new ResponseBean<>();
  }
}
