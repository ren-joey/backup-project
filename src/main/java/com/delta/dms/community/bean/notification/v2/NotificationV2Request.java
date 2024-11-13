package com.delta.dms.community.bean.notification.v2;

import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NotificationV2Request {
  private String triggerUserId;
  private String iconId = "";
  private NotificationV2Link link;
  private NotificationV2Entities name;
  private Set<String> recipient;
  private Map<NotificationV2Variable, Object> variable;
}
