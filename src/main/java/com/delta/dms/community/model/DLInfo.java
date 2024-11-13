package com.delta.dms.community.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DLInfo {
  public boolean isDL;
  public String allowCommunityId;
  public String allowForumId;
}
