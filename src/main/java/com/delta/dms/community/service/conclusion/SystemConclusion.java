package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.dao.entity.SystemConclusionBean;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;

public class SystemConclusion implements BaseConclusion {
  private SystemConclusionBean bean;
  private String rawJson;

  private static final String SYSTEM_REASON = "預警原因(Reason of Alert): ";
  private static final String SYSTEM_DESCRIPTION =
      "對策內容 / 步驟與描述(Solution Description / Execution Steps): ";
  private static final String SYSTEM_RESULT = "對策結果(Execution Result): ";
  private static final String SYSTEM_ROLE = "通知人員角色(Role to notify): ";
  private static final String SYSTEM_CLASSIFICATION = "對策措施分類(Countermeasure Type): ";
  private static final String SYSTEM_TYPE = "對策類型(Solution Type): ";
  private static final String SYSTEM_OBJECT = "執行對象(Execution Subject): ";
  private static final String SYSTEM_COMMAND = "指令(Command): ";
  private static final String SYSTEM_COMMANDKEY = "參數名稱(Parameter): ";
  private static final String SYSTEM_COMMANDVALUE = "參數值(Parameter Value): ";
  private static final String SYSTEM_ECN = "ECN: ";
  private static final String SYSTEM_PCN = "PCN: ";

  public SystemConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = new ObjectMapper().readValue(json, SystemConclusionBean.class);
  }

  @Override
  public String getJson() throws IOException {
    if (this.bean != null) {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(bean);
    } else {
      throw new IOException(Constants.JSON_EMPTY);
    }
  }

  @Override
  public TopicType getType() {
    return TopicType.SYSTEM;
  }

  public SystemConclusionBean getBean() {
    return bean;
  }

  public void setBean(SystemConclusionBean bean) {
    this.bean = bean;
  }

  @Override
  public String getText() {
    StringBuilder sb = new StringBuilder();
    sb.append(SYSTEM_REASON + bean.getReason() + Constants.HTML_BR);
    sb.append(SYSTEM_DESCRIPTION + bean.getDescription() + Constants.HTML_BR);
    sb.append(SYSTEM_RESULT + bean.getResult() + Constants.HTML_BR);
    sb.append(SYSTEM_ROLE + bean.getRole() + Constants.HTML_BR);
    sb.append(SYSTEM_CLASSIFICATION + bean.getClassification() + Constants.HTML_BR);
    sb.append(SYSTEM_TYPE + bean.getType() + Constants.HTML_BR);
    sb.append(SYSTEM_OBJECT + bean.getObject() + Constants.HTML_BR);
    sb.append(SYSTEM_COMMAND + bean.getCommand() + Constants.HTML_BR);
    sb.append(SYSTEM_COMMANDKEY + bean.getCommandKey() + Constants.HTML_BR);
    sb.append(SYSTEM_COMMANDVALUE + bean.getCommandValue() + Constants.HTML_BR);
    sb.append(SYSTEM_ECN + bean.getEcn() + Constants.HTML_BR);
    sb.append(SYSTEM_PCN + bean.getPcn() + Constants.HTML_BR);
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
