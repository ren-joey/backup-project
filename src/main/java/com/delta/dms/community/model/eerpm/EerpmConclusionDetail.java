package com.delta.dms.community.model.eerpm;

import java.util.List;
import com.delta.dms.community.dao.entity.EerpAmbu;
import com.delta.dms.community.dao.entity.EerpNewCauseSolution;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpmConclusionDetail {
  private int topicId;
  private String pushStatus;
  private EerpmCodeStatus error;
  private List<EerpmOriginCauseSolution> originCauseSolution;
  private List<EerpNewCauseSolution> newCauseSolution;
  private List<String> ecn;
  private List<String> pcn;
  private String dfauto;
  private EerpAmbu ambu;
  private String typeCode;
}
