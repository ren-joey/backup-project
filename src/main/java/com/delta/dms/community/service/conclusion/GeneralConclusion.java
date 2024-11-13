package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.dao.entity.GeneralConclusionBean;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;

public class GeneralConclusion implements BaseConclusion {
  private GeneralConclusionBean bean;
  private String rawJson;

  public GeneralConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = new ObjectMapper().readValue(json, GeneralConclusionBean.class);
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
    return TopicType.GENERAL;
  }

  public GeneralConclusionBean getBean() {
    return bean;
  }

  public void setBean(GeneralConclusionBean bean) {
    this.bean = bean;
  }

  @Override
  public String getText() {
    return bean.getText();
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
