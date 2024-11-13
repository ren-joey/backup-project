package com.delta.dms.community.service.eerp;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.EerpAdapter;
import com.delta.dms.community.adapter.PqmAdapter;
import com.delta.dms.community.dao.entity.EerpAmbu;
import com.delta.dms.community.dao.entity.EerpmGeneralConclusionBean;
import com.delta.dms.community.dao.entity.EerpmManualConclusionBean;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.enums.ConclusionState;
import com.delta.dms.community.model.eerpm.EerpmCodeStatus;
import com.delta.dms.community.model.eerpm.EerpmConclusionDetail;
import com.delta.dms.community.model.eerpm.EerpmDeviceRawData;
import com.delta.dms.community.model.eerpm.EerpmErrorRawData;
import com.delta.dms.community.model.eerpm.EerpmOriginCauseSolution;
import com.delta.dms.community.model.eerpm.EerpmTopicRawData;
import com.delta.dms.community.model.eerpp.EerppConclusionDetail;
import com.delta.dms.community.model.eerpp.EerppTopicRawData;
import com.delta.dms.community.swagger.model.EerpmErrorCauseDto;
import com.delta.dms.community.swagger.model.EerpmErrorCodeDto;
import com.delta.dms.community.swagger.model.EerpmErrorSolutionDto;
import com.delta.dms.community.swagger.model.EerpqCodeDto;
import com.delta.dms.community.swagger.model.EerpqCodeType;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.EerpConstants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

@Service
public class EerpService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final EerpAdapter eerpAdapter;
  private final PqmAdapter pqmAdapter;
  private final EerpmService eerpmService;
  private final EerpqService eerpqService;

  @Autowired
  public EerpService(
      EerpAdapter eerpAdapter,
      PqmAdapter pqmAdapter,
      EerpmService eerpmService,
      EerpqService eerpqService) {
    this.eerpAdapter = eerpAdapter;
    this.pqmAdapter = pqmAdapter;
    this.eerpmService = eerpmService;
    this.eerpqService = eerpqService;
  }

  public List<EerpmErrorCodeDto> getEerpmErrorCode(String deviceModel) throws IOException {
    return eerpmService.getEerpmErrorCode(deviceModel);
  }

  public List<EerpmErrorCauseDto> getEerpmErrorCause(String deviceModel, String errorCode)
      throws IOException {
    return eerpmService.getEerpmErrorCause(deviceModel, errorCode);
  }

  public List<EerpmErrorSolutionDto> getEerpmErrorSolution(
      String deviceModel, String errorCode, String errorCause) throws IOException {
    return eerpmService.getEerpmErrorSolution(deviceModel, errorCode, errorCause);
  }

  public EerpqCodeDto getEerpqCode(
      EerpqCodeType type,
      int pageNum,
      int pageSize,
      String factory,
      String phenomenonyCode,
      String dutyCode)
      throws IOException {
    return eerpqService.getEerpqCode(type, pageNum, pageSize, factory, phenomenonyCode, dutyCode);
  }

  public void sendConclusionToPqm(TopicInfo topicInfo, String json) {
    pqmAdapter.sendMesDataInPqm(topicInfo, json);
  }

  public void sendConclusionToEerpm(TopicInfo topicInfo, String json) throws IOException {
    eerpAdapter.pushEerpmConclusion(convertToEerpSolutionDetail(topicInfo, json));
  }

  public void sendConclusionToEerpp(TopicInfo topicInfo) throws IOException {
    eerpAdapter.pushEerppConclusion(convertToEerppGeneralConclusionDetail(topicInfo));
  }

  public boolean isEerpType(TopicType topicType) {
    switch (topicType) {
      case EERPMSUMMARY:
      case EERPMGENERAL:
      case EERPMMANUAL:
      case EERPQGENERAL:
      case EERPPGENERAL:
      case EERPMHIGHLEVEL:
        return true;
      default:
        return false;
    }
  }

  public boolean isEerpmType(TopicType topicType) {
    switch (topicType) {
      case EERPMSUMMARY:
      case EERPMGENERAL:
      case EERPMMANUAL:
      case EERPMHIGHLEVEL:
        return true;
      default:
        return false;
    }
  }

  public boolean isEerpConcluded(String jsonData) {
    try {
      return ConclusionState.CONCLUDED
          .toString()
          .equals(JsonPath.read(jsonData, EerpConstants.JSONPATH_CONCLUSION_STATE));
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  EerpmConclusionDetail convertToEerpSolutionDetail(TopicInfo topicInfo, String jsonData)
      throws IOException {
    return (TopicType.EERPMMANUAL == TopicType.fromValue(topicInfo.getTopicType()))
        ? convertToEerpmManualConclusionDetail(topicInfo, jsonData)
        : convertToEerpmGeneralConclusionDetail(topicInfo, jsonData);
  }

  private EerpmConclusionDetail convertToEerpmGeneralConclusionDetail(
      TopicInfo topicInfo, String jsonData) throws IOException {
    EerpmDeviceRawData deviceRawData =
        mapper
            .readValue(topicInfo.getTopicText(), EerpmTopicRawData.class)
            .getDeviceDatas()
            .get(NumberUtils.INTEGER_ZERO);
    EerpmGeneralConclusionBean conclusionText =
        mapper.readValue(jsonData, EerpmGeneralConclusionBean.class);
    String originErrorDesc =
        deviceRawData.getMethods().get(NumberUtils.INTEGER_ZERO).getDescription();

    return new EerpmConclusionDetail()
        .setTopicId(topicInfo.getTopicId())
        .setPushStatus(EerpConstants.EERP_PUSHSTATUS_VALUE)
        .setTypeCode(null) // IT required
        .setError(
            new EerpmCodeStatus()
                .setCode(deviceRawData.getErrorCode())
                .setDesc(conclusionText.getErrorDesc())
                .setStatus(getEerpChangedStatus(originErrorDesc, conclusionText.getErrorDesc())))
        .setOriginCauseSolution(
            Optional.ofNullable(conclusionText.getOriginCauseSolution())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(
                    solution -> {
                      EerpmErrorRawData errorRawData =
                          getEerpmErrorRawData(deviceRawData, solution.getCauseCode());
                      return new EerpmOriginCauseSolution()
                          .setCause(
                              new EerpmCodeStatus()
                                  .setCode(solution.getCauseCode())
                                  .setDesc(solution.getCauseDesc())
                                  .setStatus(
                                      getEerpChangedStatus(
                                          errorRawData.getCause(), solution.getCauseDesc())))
                          .setImproveSolution(
                              new EerpmCodeStatus()
                                  .setCode(solution.getOriginSolutionId())
                                  .setDesc(solution.getOriginSolution())
                                  .setStatus(
                                      getEerpChangedStatus(
                                          errorRawData.getQuickCountermeasure(),
                                          solution.getOriginSolution())))
                          .setNewSolution(
                              StringUtils.isEmpty(solution.getNewSolution())
                                  ? null
                                  : new EerpmCodeStatus()
                                      .setCode(solution.getOriginSolutionId())
                                      .setDesc(solution.getNewSolution())
                                      .setStatus(EerpConstants.EERP_STATUS_UPDATE));
                    })
                .collect(Collectors.toList()))
        .setNewCauseSolution(
            Optional.ofNullable(conclusionText.getNewCauseSolution())
                .orElseGet(Collections::emptyList))
        .setEcn(conclusionText.getEcn())
        .setPcn(conclusionText.getPcn())
        .setDfauto(conclusionText.getDfauto())
        .setAmbu(Optional.ofNullable(conclusionText.getAmbu()).orElseGet(EerpAmbu::new));
  }

  private EerpmConclusionDetail convertToEerpmManualConclusionDetail(
      TopicInfo topicInfo, String jsonData) throws IOException {
    EerpmManualConclusionBean conclusionText =
        mapper.readValue(jsonData, EerpmManualConclusionBean.class);

    return new EerpmConclusionDetail()
        .setTopicId(topicInfo.getTopicId())
        .setPushStatus(null)
        .setTypeCode(conclusionText.getDeviceModel())
        .setError(
            new EerpmCodeStatus()
                .setCode(conclusionText.getErrorCode())
                .setDesc(conclusionText.getErrorDesc())
                .setStatus(EerpConstants.EERP_STATUS_UPDATE))
        .setOriginCauseSolution(
            Optional.ofNullable(conclusionText.getOriginCauseSolution())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(
                    solution ->
                        new EerpmOriginCauseSolution()
                            .setCause(
                                new EerpmCodeStatus()
                                    .setCode(conclusionText.getCauseCode())
                                    .setDesc(solution.getCauseDesc())
                                    .setStatus(EerpConstants.EERP_STATUS_UPDATE))
                            .setImproveSolution(
                                new EerpmCodeStatus()
                                    .setCode(conclusionText.getSolutionCode())
                                    .setDesc(solution.getOriginSolution())
                                    .setStatus(EerpConstants.EERP_STATUS_UPDATE))
                            .setNewSolution(
                                StringUtils.isEmpty(solution.getNewSolution())
                                    ? null
                                    : new EerpmCodeStatus()
                                        .setCode(conclusionText.getSolutionCode())
                                        .setDesc(solution.getNewSolution())
                                        .setStatus(EerpConstants.EERP_STATUS_UPDATE)))
                .collect(Collectors.toList()))
        .setNewCauseSolution(
            Optional.ofNullable(conclusionText.getNewCauseSolution())
                .orElseGet(Collections::emptyList))
        .setEcn(conclusionText.getEcn())
        .setPcn(conclusionText.getPcn())
        .setDfauto(conclusionText.getDfauto())
        .setAmbu(Optional.ofNullable(conclusionText.getAmbu()).orElseGet(EerpAmbu::new));
  }

  private EerppConclusionDetail convertToEerppGeneralConclusionDetail(TopicInfo topicInfo)
      throws IOException {
    EerppTopicRawData topicRawData =
        mapper.readValue(topicInfo.getTopicText(), EerppTopicRawData.class);
    return new EerppConclusionDetail()
        .setTopicId(topicInfo.getTopicId())
        .setFactory(topicRawData.getFactory());
  }

  private int getEerpChangedStatus(String origenString, String newString) {
    return StringUtils.equals(origenString, newString)
        ? EerpConstants.EERP_STATUS_NONE
        : EerpConstants.EERP_STATUS_UPDATE;
  }

  private EerpmErrorRawData getEerpmErrorRawData(
      EerpmDeviceRawData deviceRawData, String causeCode) {
    return deviceRawData
        .getMethods()
        .stream()
        .filter(item -> StringUtils.equals(item.getCauseId(), causeCode))
        .findFirst()
        .orElseGet(EerpmErrorRawData::new);
  }
}
