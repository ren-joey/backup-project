package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AttachmentDao {

  public int insertAttachmentKeyman(
      @Param("userId") List<String> userId, @Param("attachmentId") String attachmentId);
}
