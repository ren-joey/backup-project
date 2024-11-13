package com.delta.dms.community.dao.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.delta.dms.community.enums.EerpmErrorLevel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TopicEerpmEntity {
  private int topicId;
  private String topicTitle;
  private long topicCreateTime;
  private String forumName;
  private int conclusionStateId;
  private long conclusionCreateTime;
  private String factory;
  private String deviceModel;
  private String errorCode;
  private long errorCount;
  private String worstDeviceId;
  private String detail;
  private String errorDesc;
  private String conclusion;

  private String id;
  private Map<Long, List<Long>> history = new LinkedHashMap<>();
  private EerpmErrorLevel level = EerpmErrorLevel.LEVEL_1;
  private List<String> worstDevices = new ArrayList<>();
}
