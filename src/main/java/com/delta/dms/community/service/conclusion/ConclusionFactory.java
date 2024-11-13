package com.delta.dms.community.service.conclusion;

import java.io.IOException;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.utils.Constants;

public class ConclusionFactory {
  public BaseConclusion getConclusion(TopicType type, String json) throws IOException {
    BaseConclusion conclusion = null;
    switch (type) {
      case GENERAL:
        conclusion = new GeneralConclusion(json);
        break;
      case PROBLEM:
        conclusion = new QuestionConclusion(json);
        break;
      case SYSTEM:
        conclusion = new SystemConclusion(json);
        break;
      case EERPMGENERAL:
        conclusion = new EerpmGeneralConclusion(json);
        break;
      case EERPMMANUAL:
        conclusion = new EerpmManualConclusion(json);
        break;
      case EERPQGENERAL:
        conclusion = new EerpqGeneralConclusion(json);
        break;
      case EERPPGENERAL:
        conclusion = new EerppGeneralConclusion(json);
        break;
      case EERPMHIGHLEVEL:
        conclusion = new EerpmHighLevelConclusion(json);
        break;
      default:
        throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return conclusion;
  }
}
