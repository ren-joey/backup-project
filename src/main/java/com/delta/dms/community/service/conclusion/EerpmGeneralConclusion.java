package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import com.delta.dms.community.dao.entity.EerpAmbu;
import com.delta.dms.community.dao.entity.EerpmGeneralConclusionBean;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;

public class EerpmGeneralConclusion implements BaseConclusion {
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String DEVICE_MODEL = "設備型號(Device Model): ";
  private static final String ERROR_CODE = "錯誤代碼(Error Code): ";
  private static final String ERROR_DESC = "問題描述(Problem Description): ";
  private static final String CAUSE = "原因(Cause): ";
  private static final String OLD_QUICK_TROUBLESHOOTING =
      "Error舊快排優化(Error Old Quick troubleshooting Optimization): ";
  private static final String NEW_QUICK_TROUBLESHOOTING =
      "Error新增快排(Error Added Quick Troubleshooting): ";
  private static final String ECN = "ECN-設計結構變更(ECN-Design Structure Change): ";
  private static final String PCN = "PCN-製程結構變更(PCN-Process Structure Change): ";
  private static final String DFAUTO = "DFauto條文修訂(DFauto Amendments): ";
  private static final String AMBU_ELECTRONIC_OPTIMIZATION =
      "AMBU-電控軟體優化(AMBU-Electronic Control Software Optimization): ";
  private static final String AMBU_MECHANISM_OPTIMIZATION =
      "AMBU-機構結構優化(AMBU-Mechanism Structure Optimization): ";

  private EerpmGeneralConclusionBean bean;
  private String rawJson;

  public EerpmGeneralConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public TopicType getType() {
    return TopicType.EERPMGENERAL;
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = mapper.readValue(json, EerpmGeneralConclusionBean.class);
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
    sb.append(DEVICE_MODEL + bean.getDeviceModel() + Constants.HTML_BR);
    sb.append(ERROR_CODE + bean.getErrorCode() + Constants.HTML_BR);
    sb.append(ERROR_DESC + bean.getErrorDesc() + Constants.HTML_BR);
    Optional.ofNullable(bean.getOriginCauseSolution())
        .orElseGet(Collections::emptyList)
        .forEach(
            solution -> {
              sb.append(CAUSE + solution.getCauseDesc() + Constants.HTML_BR);
              sb.append(
                  OLD_QUICK_TROUBLESHOOTING + solution.getOriginSolution() + Constants.HTML_BR);
              sb.append(NEW_QUICK_TROUBLESHOOTING + solution.getNewSolution() + Constants.HTML_BR);
            });
    Optional.ofNullable(bean.getNewCauseSolution())
        .orElseGet(Collections::emptyList)
        .forEach(
            solution -> {
              sb.append(CAUSE + solution.getCauseDesc() + Constants.HTML_BR);
              sb.append(NEW_QUICK_TROUBLESHOOTING + solution.getSolution() + Constants.HTML_BR);
            });
    Optional.ofNullable(bean.getEcn())
        .orElseGet(Collections::emptyList)
        .forEach(ecn -> sb.append(ECN + ecn + Constants.HTML_BR));
    Optional.ofNullable(bean.getPcn())
        .orElseGet(Collections::emptyList)
        .forEach(pcn -> sb.append(PCN + pcn + Constants.HTML_BR));
    sb.append(DFAUTO + bean.getDfauto() + Constants.HTML_BR);
    sb.append(
        AMBU_ELECTRONIC_OPTIMIZATION
            + Optional.ofNullable(bean.getAmbu()).orElseGet(EerpAmbu::new).getSoftware()
            + Constants.HTML_BR);
    sb.append(
        AMBU_MECHANISM_OPTIMIZATION
            + Optional.ofNullable(bean.getAmbu()).orElseGet(EerpAmbu::new).getMechanism()
            + Constants.HTML_BR);
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
}
