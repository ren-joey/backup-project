package com.delta.dms.community.dao.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.delta.dms.community.enums.EerpErrorLevel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TopicEerppEntity {
  private int topicId;
  private String topicTitle;
  private long topicCreateTime;
  private String forumName;
  private int conclusionStateId;
  private long conclusionCreateTime;
  private String factory;
  private String department;
  private String area;
  private String lossCode;
  private String lossCodeDesc;
  private String duration;
  private String line;

  private String id;
  private List<Double> durations = new ArrayList<>();
  private List<String> lines = new ArrayList<>();

  private List<String> areas = new ArrayList<>();
  private List<String> lossCodeDescriptions = new ArrayList<>();
  private Map<Long, List<Double>> history = new LinkedHashMap<>();
  private long durationCount;
  private EerpErrorLevel level = EerpErrorLevel.LEVEL_1;
}
