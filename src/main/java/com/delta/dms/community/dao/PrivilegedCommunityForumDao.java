package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.PrivilegedCommunityForum;

@FunctionalInterface
public interface PrivilegedCommunityForumDao {
  /**
   * Get privileged community and forum by user id
   *
   * @param userId uid
   * @return List of privileged communities and forums
   */
  public List<PrivilegedCommunityForum> getPrivilegedCommunityForumIds(
      @Param("userId") List<String> userId,
      @Param("offset") Integer offset,
      @Param("limit") Integer limit);
}
