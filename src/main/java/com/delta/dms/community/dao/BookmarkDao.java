package com.delta.dms.community.dao;

import org.apache.ibatis.annotations.Param;

public interface BookmarkDao {
  /**
   * Set the book mark of the topic
   *
   * @param userId User id
   * @param object object name
   * @param objectPk object Pk id
   * @param createTime create Time
   * @return Numbers of rows affected
   */
  public int setBookmark(
      @Param("userId") String userId,
      @Param("object") String object,
      @Param("objectPk") int objectPk,
      @Param("createTime") long createTime);

  /**
   * Remove the book mark of the topic
   *
   * @param userId User id
   * @param object object name
   * @param objectPk object Pk id
   * @return Numbers of rows affected
   */
  public int removeBookmark(
      @Param("userId") String userId,
      @Param("object") String object,
      @Param("objectPk") int objectPk);

  /**
   * Check whether user bookmark the object
   *
   * @param userId User id
   * @param object Object id
   * @param objectPk Object key
   * @return Numbers of the result
   */
  public int checkUserBookmark(
      @Param("userId") String userId,
      @Param("object") String object,
      @Param("objectPk") int objectPk);
}
