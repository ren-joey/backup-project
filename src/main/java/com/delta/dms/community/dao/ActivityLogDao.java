package com.delta.dms.community.dao;

import com.delta.dms.community.dao.entity.ActivityLogInfo;

@FunctionalInterface
public interface ActivityLogDao {
  /**
   * insert data to dms_community.activity_log
   *
   * @param activityLogInfo ActivityLogInfo object
   * @return the number of insertion
   */
  public Integer insertActivityLogData(ActivityLogInfo activityLogInfo);
}
