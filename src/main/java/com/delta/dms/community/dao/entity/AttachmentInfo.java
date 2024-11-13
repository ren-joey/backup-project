package com.delta.dms.community.dao.entity;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AttachmentInfo {
  private String attachmentId;
  private String fileName;
  private String fileExt;
  private String recordType;
  private String topicTitle;
  private String createUserId;
  private String createUserName;

  private List<String> appFieldList;
}
