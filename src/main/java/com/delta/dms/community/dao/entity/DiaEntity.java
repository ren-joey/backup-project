package com.delta.dms.community.dao.entity;

import java.util.List;
import com.delta.dms.community.enums.DiaStatus;
import com.delta.dms.community.swagger.model.DiaClassification;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiaEntity {
  private Integer innovationAwardId;
  private DiaClassification classificationName;
  private String projectItemName;
  private String oaInstanceCode;
  private String projectExecutiveSummary;
  private DiaStatus status;
  private String message;
  private long applyTime;
  private long createTopicTime;
  private List<DiaMemberEntity> memberList;
  private List<DiaAttachmentPathEntity> attachmentPathList;
}
