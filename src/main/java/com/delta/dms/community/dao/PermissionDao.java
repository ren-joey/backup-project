package com.delta.dms.community.dao;

import org.apache.ibatis.annotations.Param;

@FunctionalInterface
public interface PermissionDao {

  /**
   * Check user permission of objects
   *
   * @param userId User id
   * @param communityId Community id
   * @param forumId Forum id
   * @param object Object name (community, forum, topic, reply)
   * @param operation Operation name (create, read, update, delete)
   * @return 0 (no permission)/ > 0 (has permission)
   */
  public boolean checkUserPrivilege(
      @Param("userId") String userId,
      @Param("communityId") int communityId,
      @Param("forumId") int forumId,
      @Param("object") String object,
      @Param("operation") String operation);
}
