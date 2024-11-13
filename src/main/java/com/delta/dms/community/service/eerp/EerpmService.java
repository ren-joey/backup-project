package com.delta.dms.community.service.eerp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.EerpAdapter;
import com.delta.dms.community.model.eerpm.EerpmErrorDetail;
import com.delta.dms.community.swagger.model.EerpmErrorCauseDto;
import com.delta.dms.community.swagger.model.EerpmErrorCodeDto;
import com.delta.dms.community.swagger.model.EerpmErrorSolutionDto;

@Service
public class EerpmService {

  private final EerpAdapter eerpAdapter;

  @Autowired
  public EerpmService(EerpAdapter eerpAdapter) {
    this.eerpAdapter = eerpAdapter;
  }

  public List<EerpmErrorCodeDto> getEerpmErrorCode(String deviceModel) throws IOException {
    return Optional.ofNullable(
            eerpAdapter
                .getEerpmErrorCode(new EerpmErrorDetail().setTypeCode(deviceModel))
                .getErrorCodeList())
        .orElseGet(ArrayList::new)
        .stream()
        .map(
            item ->
                new EerpmErrorCodeDto()
                    .errorCode(item.getErrorCode())
                    .errorDesc(item.getDescription()))
        .collect(Collectors.toList());
  }

  public List<EerpmErrorCauseDto> getEerpmErrorCause(String deviceModel, String errorCode)
      throws IOException {
    return Optional.ofNullable(
            eerpAdapter
                .getEerpmCause(
                    new EerpmErrorDetail().setTypeCode(deviceModel).setErrorCode(errorCode))
                .getCauseList())
        .orElseGet(ArrayList::new)
        .stream()
        .map(
            item ->
                new EerpmErrorCauseDto()
                    .causeCode(item.getCauseCode())
                    .causeDesc(item.getDescription()))
        .collect(Collectors.toList());
  }

  public List<EerpmErrorSolutionDto> getEerpmErrorSolution(
      String deviceModel, String errorCode, String errorCause) throws IOException {
    return Optional.ofNullable(
            eerpAdapter
                .getEerpmSolution(
                    new EerpmErrorDetail()
                        .setTypeCode(deviceModel)
                        .setErrorCode(errorCode)
                        .setCauseCode(errorCause))
                .getSolutionList())
        .orElseGet(ArrayList::new)
        .stream()
        .map(
            item ->
                new EerpmErrorSolutionDto()
                    .solutionCode(item.getSolutionCode())
                    .originSolution(item.getDescription()))
        .collect(Collectors.toList());
  }
}
