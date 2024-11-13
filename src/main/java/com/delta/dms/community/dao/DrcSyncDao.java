package com.delta.dms.community.dao;

import com.delta.dms.community.model.DrcSyncLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface DrcSyncDao {
    public void updateFileStatus(
            @Param("database") String atabase,
            @Param("action") String action,
            @Param("topicId") int topicId,
            @Param("communityId") int communityId,
            @Param("forumId") int forumId,
            @Param("responseCode") int responseCode,
            @Param("responseBody") String responseBody);

    List<DrcSyncLog> findDrcSyncLogsForSpecificActions(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );
}

