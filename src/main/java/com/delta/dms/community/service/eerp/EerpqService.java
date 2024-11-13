package com.delta.dms.community.service.eerp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.EerpAdapter;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.eerpq.EerpqDutyCode;
import com.delta.dms.community.model.eerpq.EerpqErrorDto;
import com.delta.dms.community.model.eerpq.EerpqFailureCode;
import com.delta.dms.community.model.eerpq.EerpqPhenomenonCode;
import com.delta.dms.community.model.eerpq.EerpqReasonCode;
import com.delta.dms.community.model.eerpq.EerpqSolutionCode;
import com.delta.dms.community.swagger.model.EerpqCodeDto;
import com.delta.dms.community.swagger.model.EerpqCodeType;
import com.delta.dms.community.swagger.model.KeyLabelDto;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.EerpConstants;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class EerpqService {

  private final EerpAdapter eerpAdapter;

  public EerpqCodeDto getEerpqCode(
      EerpqCodeType type,
      int pageNum,
      int pageSize,
      String factory,
      String phenomenonyCode,
      String dutyCode)
      throws IOException {
    if (StringUtils.isEmpty(StringUtils.trimToEmpty(factory))) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    EerpqErrorDto request =
        new EerpqErrorDto()
            .setPageNum(pageNum)
            .setPageSize(pageSize)
            .setFactory(StringUtils.trimToEmpty(factory))
            .setPhenomenonyCode(StringUtils.defaultString(phenomenonyCode))
            .setDutyCode(StringUtils.defaultString(dutyCode));
    switch (type) {
      case PHENOMENONCODE:
        return convertToEerpqCodeDto(eerpAdapter.getEerpqPhenomenonCode(request));
      case FAILURECODE:
        if (StringUtils.isEmpty(request.getPhenomenonyCode())) {
          return new EerpqCodeDto().count(NumberUtils.INTEGER_ZERO).data(new ArrayList<>());
        }
        return convertToEerpqCodeDto(eerpAdapter.getEerpqFailureCode(request));
      case DUTYCODE:
        return convertToEerpqCodeDto(eerpAdapter.getEerpqDutyCode(request));
      case REASONCODE:
        if (StringUtils.isEmpty(request.getDutyCode())) {
          return new EerpqCodeDto().count(NumberUtils.INTEGER_ZERO).data(new ArrayList<>());
        }
        return convertToEerpqCodeDto(eerpAdapter.getEerpqReasonCode(request));
      case SOLUTIONCODE:
        return convertToEerpqCodeDto(eerpAdapter.getEerpqSolutionCode(request));
      default:
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
  }

  private EerpqCodeDto convertToEerpqCodeDto(EerpqFailureCode data) {
    if (StringUtils.equalsIgnoreCase(EerpConstants.EERPQ_LANG_EN, AcceptLanguage.get())) {
      return new EerpqCodeDto()
          .count(data.getTotoalSize())
          .data(
              data.getDataList()
                  .stream()
                  .map(
                      item ->
                          new KeyLabelDto()
                              .key(item.getFailureCode())
                              .label(item.getFailureDescEn()))
                  .collect(Collectors.toList()));
    }
    return new EerpqCodeDto()
        .count(data.getTotoalSize())
        .data(
            data.getDataList()
                .stream()
                .map(
                    item ->
                        new KeyLabelDto().key(item.getFailureCode()).label(item.getFailureDesc()))
                .collect(Collectors.toList()));
  }

  private EerpqCodeDto convertToEerpqCodeDto(EerpqPhenomenonCode data) {
    return new EerpqCodeDto()
        .count(data.getTotoalSize())
        .data(
            data.getDataList()
                .stream()
                .map(
                    item ->
                        new KeyLabelDto()
                            .key(item.getPhenomenonCode())
                            .label(item.getPhenomenonCode()))
                .collect(Collectors.toList()));
  }

  private EerpqCodeDto convertToEerpqCodeDto(EerpqDutyCode data) {
    if (StringUtils.equalsIgnoreCase(EerpConstants.EERPQ_LANG_EN, AcceptLanguage.get())) {
      return new EerpqCodeDto()
          .count(data.getTotoalSize())
          .data(
              data.getDataList()
                  .stream()
                  .map(
                      item -> new KeyLabelDto().key(item.getDutyCode()).label(item.getDutyDescEn()))
                  .collect(Collectors.toList()));
    }
    return new EerpqCodeDto()
        .count(data.getTotoalSize())
        .data(
            data.getDataList()
                .stream()
                .map(item -> new KeyLabelDto().key(item.getDutyCode()).label(item.getDutyDesc()))
                .collect(Collectors.toList()));
  }

  private EerpqCodeDto convertToEerpqCodeDto(EerpqReasonCode data) {
    if (StringUtils.equalsIgnoreCase(EerpConstants.EERPQ_LANG_EN, AcceptLanguage.get())) {
      return new EerpqCodeDto()
          .count(data.getTotoalSize())
          .data(
              data.getDataList()
                  .stream()
                  .map(
                      item -> new KeyLabelDto().key(item.getReason()).label(item.getReasonDescEn()))
                  .collect(Collectors.toList()));
    }
    return new EerpqCodeDto()
        .count(data.getTotoalSize())
        .data(
            data.getDataList()
                .stream()
                .map(item -> new KeyLabelDto().key(item.getReason()).label(item.getReasonDesc()))
                .collect(Collectors.toList()));
  }

  private EerpqCodeDto convertToEerpqCodeDto(EerpqSolutionCode data) {
    return new EerpqCodeDto()
        .count(data.getTotoalSize())
        .data(
            data.getDataList()
                .stream()
                .map(
                    item ->
                        new KeyLabelDto().key(item.getSolutionCode()).label(item.getSolutionCode()))
                .collect(Collectors.toList()));
  }
}
