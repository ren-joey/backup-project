package com.delta.dms.community.dao.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AttachmentAppFieldEntity {
  private String attachmentId;
  private String appFieldId;
  private String appFieldName;
}
