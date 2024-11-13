package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.DdfQueue;
import com.delta.dms.community.swagger.model.AllInformation;

public interface DdfDao {

  public void upsertDdfQueue(
      @Param("type") String type,
      @Param("id") int id,
      @Param("status") String status,
      @Param("message") String message);

  public void upsertDdfDeleteQueue(
      @Param("id") String id,
      @Param("status") String status,
      @Param("message") String message);

  public AllInformation getAllInformation(@Param("type") String type, @Param("id") int id);

  public void storeDdfId(
      @Param("type") String type, @Param("id") int id, @Param("uuid") String uuid);

  public List<DdfQueue> getDdfQueueByStatus(@Param("status") String status);

  public List<String> getDdfDeleteQueueByStatus(@Param("status") String status);

  public void updateDdfQueueStatus(
      @Param("type") String type, @Param("ids") List<Integer> ids, @Param("status") String status);

  public void updateDdfDeleteQueueStatus(
      @Param("ids") List<String> ids, @Param("status") String status);

  public void deleteDdfQueue(@Param("type") String type, @Param("id") int id);

  public void deleteDdfDeleteQueue(@Param("id") String id);

  public List<String> getDdfDeleteQueueByStatusAndAssociatedId(
          @Param("status") String status, @Param("associatedId") int associatedId, @Param("fileType") String fileType);


}
