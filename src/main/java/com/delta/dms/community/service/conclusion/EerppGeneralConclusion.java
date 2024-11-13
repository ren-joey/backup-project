package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import com.delta.dms.community.dao.entity.EerppGeneralConclusionBean;
import com.delta.dms.community.enums.EerppLanguage;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;

public class EerppGeneralConclusion implements BaseConclusion {
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String AREA = "生產區域(Area): ";
  private static final String LOSS_CODE = "生產延誤代碼(Loss Code): ";
  private static final String LOSS_DESC = "生產延誤描述(Loss Code Description): ";
  private static final String REASON_DESC = "原因描述(Reason Code Description): ";
  private static final String OLD_SOLUTION = "舊對策(Old Solution): ";
  private static final String NEW_SOLUTION = "新增對策(New Solution): ";

  private EerppGeneralConclusionBean bean;
  private String rawJson;

  public EerppGeneralConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public TopicType getType() {
    return TopicType.EERPPGENERAL;
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = mapper.readValue(json, EerppGeneralConclusionBean.class);
  }

  @Override
  public String getJson() throws IOException {
    if (this.bean != null) {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bean);
    } else {
      throw new IOException(Constants.JSON_EMPTY);
    }
  }

  @Override
  public String getText() {
    StringBuilder sb = new StringBuilder();
    sb.append(AREA + bean.getArea() + Constants.HTML_BR);
    sb.append(LOSS_CODE + bean.getLossCode() + Constants.HTML_BR);
    sb.append(LOSS_DESC + getDefaultDesc(bean.getLossCodeDesc()) + Constants.HTML_BR);
    sb.append(REASON_DESC + getDefaultDesc(bean.getReasonCodeDesc()) + Constants.HTML_BR);
    Optional.ofNullable(bean.getOriginSolution())
        .orElseGet(Collections::emptyList)
        .forEach(
            solution ->
                sb.append(
                    OLD_SOLUTION
                        + getDefaultDesc(solution.getSolutionCodeDesc())
                        + Constants.HTML_BR));
    Optional.ofNullable(bean.getNewSolution())
        .orElseGet(Collections::emptyList)
        .forEach(
            solution ->
                sb.append(
                    NEW_SOLUTION
                        + getDefaultDesc(solution.getSolutionCodeDesc())
                        + Constants.HTML_BR));
    return sb.toString();
  }

  @Override
  public String getExcelText(MessageSource messageSource) {
    return this.getText();
  }

  @Override
  public String getRawJson() {
    return rawJson;
  }

  private String getDefaultDesc(Map<String, String> descMap) {
    String targetDesc =
        StringUtils.defaultString(
            descMap.get(AcceptLanguage.convertToLocaleString(AcceptLanguage.get())));
    if (StringUtils.isEmpty(targetDesc)) {
      List<String> descList =
          descMap
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(e -> EerppLanguage.fromLanguage(e.getKey()), Entry::getValue))
              .entrySet()
              .stream()
              .sorted(Comparator.comparing(e -> e.getKey().getOrder()))
              .map(Entry::getValue)
              .collect(Collectors.toList());
      for (String desc : descList) {
        if (!StringUtils.isEmpty(desc)) {
          return desc;
        }
      }
    }
    return targetDesc;
  }
}
