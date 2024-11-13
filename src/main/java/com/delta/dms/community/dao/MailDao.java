package com.delta.dms.community.dao;

import org.apache.ibatis.annotations.Param;

public interface MailDao {

  /**
   * Store mail
   *
   * @param creator
   * @param sender
   * @param recipient
   * @param subject
   * @param content
   * @param priority
   * @return Numbers of the row affected
   */
  public int insertMail(
      @Param("creator") String creator,
      @Param("sender") String sender,
      @Param("recipient") String recipient,
      @Param("subject") String subject,
      @Param("content") String content,
      @Param("priority") int priority);
}
