package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.DiaAttachmentDetailEntity;
import com.delta.dms.community.dao.entity.DiaEntity;
import com.delta.dms.community.dao.entity.DiaMemberEntity;
import com.delta.dms.community.enums.DiaAttachmentPathStatus;
import com.delta.dms.community.enums.DiaStatus;
import com.delta.dms.community.swagger.model.GeneralStatus;

public interface DiaDao {
  public int countDiaAttachmentPath(@Param("attachmentPath") List<String> attachmentPath);

  public int insertDia(DiaEntity entity);

  public int insertDiaAttachmentPath(@Param("diaId") int diaId, @Param("path") List<String> path);

  public int insertDiaMember(
      @Param("diaId") int diaId, @Param("member") List<DiaMemberEntity> member);

  public List<DiaEntity> getDiaByStatus(@Param("status") DiaStatus status);

  public DiaEntity getDiaById(@Param("id") int id);

  public Integer updateDiaStatus(@Param("id") List<Integer> id, @Param("status") DiaStatus status);

  public List<String> getDiaAttachmentPathByStatus(@Param("status") DiaAttachmentPathStatus status);

  public Integer updateDiaAttachmentPathStatus(
      @Param("pathList") List<String> pathList, @Param("status") DiaAttachmentPathStatus status);

  public List<DiaAttachmentDetailEntity> getDiaAttachmentWithoutDdf();

  public Integer insertDiaAttachment(
      @Param("path") String path, @Param("fileMap") Map<String, Long> fileMap);

  public Integer updateDiaAttachmentDdfId(
      @Param("path") String path, @Param("fileName") String fileName, @Param("ddfId") String ddfId);

  public List<String> getDiaAttachmentDdfIdByPath(@Param("path") List<String> path);

  public Integer insertSyncDiaAttachmentLog(
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("status") DiaStatus status,
      @Param("message") String message);

  public List<String> getDiaLogRecipient();

  public int updateDiaCreationResult(
      @Param("id") int id,
      @Param("status") GeneralStatus status,
      @Param("msg") String msg,
      @Param("time") long time);
}
