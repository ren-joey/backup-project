package com.delta.dms.community.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.delta.dms.community.swagger.model.TemplateType;
import com.delta.dms.community.utils.AugmentedDateTool;

@Service
public class TemplateService {

  private VelocityEngine velocityEngine;
  private TemplateEngine textTemplateEngine;
  private static final String TEMPLATE_PATH_FORMAT = "template/%s.vm";
  private static final String TEMPLATE_DATETOOL = "dateTool";

  @Autowired
  public TemplateService(VelocityEngine velocityEngine, TemplateEngine textTemplateEngine) {
    this.velocityEngine = velocityEngine;
    this.textTemplateEngine = textTemplateEngine;
  }

  public String getTemplate(TemplateType templateType, VelocityContext velocityContext) {
    velocityContext.put(TEMPLATE_DATETOOL, new AugmentedDateTool());
    StringWriter stringWriter = new StringWriter();
    velocityEngine.mergeTemplate(
        String.format(TEMPLATE_PATH_FORMAT, templateType),
        StandardCharsets.UTF_8.toString(),
        velocityContext,
        stringWriter);
    return stringWriter.toString();
  }

  public String getThymeleafTemplate(TemplateType templateType, Context context) {
    return textTemplateEngine.process(templateType.toString(), context);
  }
}
