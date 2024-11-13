package com.delta.dms.community.bean.notification.v2;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NotificationV2Link {
  private String title = "Read More on DMS";
  private String url;
}
