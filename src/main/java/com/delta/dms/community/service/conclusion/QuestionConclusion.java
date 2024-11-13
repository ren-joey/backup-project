package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import java.util.Locale;

import com.delta.dms.community.dao.entity.QuestionConclusionBean;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import static com.delta.dms.community.enums.I18nEnum.*;
import static com.delta.dms.community.utils.Constants.REPLY_QUESTION_CONCLUSION_TEXT_FORMAT;

public class QuestionConclusion implements BaseConclusion {
  private QuestionConclusionBean bean;
  private String rawJson;

  public QuestionConclusion(String json) throws IOException {
    setJson(json);
  }

  @Override
  public void setJson(String json) throws IOException {
    this.rawJson = json;
    this.bean = new ObjectMapper().readValue(json, QuestionConclusionBean.class);
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
    return TopicType.PROBLEM;
  }

  public QuestionConclusionBean getBean() {
    return bean;
  }

  public void setBean(QuestionConclusionBean bean) {
    this.bean = bean;
  }

  @Override
  public String getText() {
    return "問題描述 (Problem Description) :"
        + bean.getDescription()
        + "<br/>解決方案 (Solution Description) :"
        + bean.getSolution()
        + "<br/>補充說明 (Additional Information) :"
        + bean.getNote();
  }

  // 這邊要套用i18n
  @Override
  public String getExcelText(MessageSource messageSource) {
    Locale locale = LocaleContextHolder.getLocale();
    return String.format(REPLY_QUESTION_CONCLUSION_TEXT_FORMAT,
            messageSource.getMessage(String.valueOf(QUESTION_CONCLUSION_PROBLEM_DESCRIPTION), null, locale),
            bean.getDescription(),
            messageSource.getMessage(String.valueOf(QUESTION_CONCLUSION_SOLUTION_DESCRIPTION), null, locale),
            bean.getSolution(),
            messageSource.getMessage(String.valueOf(QUESTION_CONCLUSION_ADDITIONAL_INFORMATION), null, locale),
            bean.getNote()
    );
  }


  @Override
  public String getRawJson() {
    return rawJson;
  }
}
