package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.swagger.model.TopicType;
import org.springframework.context.MessageSource;

public interface BaseConclusion {
  TopicType getType();

  void setJson(String json) throws IOException;

  String getJson() throws IOException;

  String getText();
  String getExcelText(MessageSource messageSource);

  String getRawJson();
}
