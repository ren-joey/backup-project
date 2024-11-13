package com.delta.dms.community.dao;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.enums.FileArchiveType;

public interface FileArchiveQueueDao {

  static final String STATUS_PROCESS = "PROCESSING";
  static final String STATUS_WAIT = "WAIT";
  static final String STATUS_FAIL = "FAIL";

  void upsertQueue(@Param("type") FileArchiveType type, @Param("id") String id);

  List<IdNameEntity> getQueue();

  default void processQueue(FileArchiveType type, List<String> ids) {
    ofNullable(ids)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(list -> updateQueueStatus(type, STATUS_PROCESS, list));
  }

  default void resetQueue(FileArchiveType type, String id) {
    updateQueueStatus(type, STATUS_WAIT, singletonList(id));
  }

  default void updateErrorQueue(FileArchiveType type, String id) {
    updateQueueStatus(type, STATUS_FAIL, singletonList(id));
  }

  void updateQueueStatus(
      @Param("type") FileArchiveType type,
      @Param("status") String status,
      @Param("ids") List<String> ids);

  void deleteQueue(@Param("type") FileArchiveType type, @Param("id") String id);
}
