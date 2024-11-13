package com.delta.dms.community.dao.entity;

import com.delta.dms.community.enums.DiaAttachmentPathStatus;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiaAttachmentPathEntity {
  private String attachmentPath;
  private DiaAttachmentPathStatus attachmentPathStatus;
}
