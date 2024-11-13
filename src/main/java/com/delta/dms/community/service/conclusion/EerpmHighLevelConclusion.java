package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.swagger.model.TopicType;

public class EerpmHighLevelConclusion extends EerpmGeneralConclusion {

  public EerpmHighLevelConclusion(String json) throws IOException {
    super(json);
  }

  @Override
  public TopicType getType() {
    return TopicType.EERPMHIGHLEVEL;
  }
}
