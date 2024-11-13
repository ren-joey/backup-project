package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.TopicEerpmEntity;
import com.delta.dms.community.dao.entity.TopicEerppEntity;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.swagger.model.GeneralStatus;

public interface EerpDao {
  public Integer insertEerpReportLog(
      @Param("processStartTime") String processStartTime,
      @Param("processEndTime") String processEndTime,
      @Param("type") EerpType type,
      @Param("status") GeneralStatus status,
      @Param("message") String message,
      @Param("reportStartTimestamp") long reportStartTimestamp,
      @Param("reportEndTimestamp") long reportEndTimestamp);

  public List<String> getEerpReportLogRecipient();

  public Long getLastReportTime(@Param("type") EerpType type);

  public List<TopicEerpmEntity> getEerpmTopic(
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime,
      @Param("filterMap") Map<String, List<Object>> filterMap,
      @Param("offset") int offset,
      @Param("limit") int limit);

  public Integer countEerpmTopic(
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime,
      @Param("filterMap") Map<String, List<Object>> filterMap);

  public List<IdNameEntity> getAllConclusionState(@Param("lang") String lang);

  public List<Object> getEerpmDistinctColumn(
      @Param("column") String column,
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime);

  public List<TopicEerppEntity> getEerppTopic(
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime,
      @Param("filterMap") Map<String, List<Object>> filterMap,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("lang") String lang);

  public Integer countEerppTopic(
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime,
      @Param("filterMap") Map<String, List<Object>> filterMap);

  public List<Object> getEerppDistinctColumn(
      @Param("column") String column,
      @Param("communityId") int communityId,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime);
}
