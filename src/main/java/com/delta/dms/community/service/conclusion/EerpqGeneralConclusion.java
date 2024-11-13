package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.dao.entity.EerpqGeneralConclusionBean;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;

public class EerpqGeneralConclusion implements BaseConclusion {
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String PHENOMENON_CODE = "修護不良代碼(Phenomenon Code): ";
  private static final String FAILURE_CODE = "不良原因代碼(Failure Code): ";
  private static final String DUTY_CODE = "責任分類(Duty Code): ";
  private static final String REASON_CODE = "責任類別(Reason Code): ";
  private static final String SOLUTION_CODE = "處理對策(Solution Code): ";
  private static final String LOCATION = "零件位置(Location): ";

  private EerpqGeneralConclusionBean bean;
  private String rawJson;

  public EerpqGeneralConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public TopicType getType() {
    return TopicType.EERPQGENERAL;
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = mapper.readValue(json, EerpqGeneralConclusionBean.class);
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
    return new StringBuilder()
        .append(PHENOMENON_CODE + bean.getPhenomenonCode() + Constants.HTML_BR)
        .append(FAILURE_CODE + bean.getFailureCode() + Constants.HTML_BR)
        .append(DUTY_CODE + bean.getDutyCode() + Constants.HTML_BR)
        .append(REASON_CODE + bean.getReasonCode() + Constants.HTML_BR)
        .append(SOLUTION_CODE + bean.getSolutionCode() + Constants.HTML_BR)
        .append(LOCATION + bean.getLocation() + Constants.HTML_BR)
        .toString();
  }

  @Override
  public String getExcelText(MessageSource messageSource) {
    return this.getText();
  }

  @Override
  public String getRawJson() {
    return rawJson;
  }
}
