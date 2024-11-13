package com.delta.dms.community.dao.entity;

import java.util.List;
import org.springframework.data.domain.Sort.Order;
import com.delta.dms.community.swagger.model.SearchScope;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CommunitySearchRequestEntity {
  private String category;
  private List<String> excludeStatusList;
  private boolean checkRole;
  private String userIdWithGid;
  private int roleId;
  private String sortField;
  private String sortOrder;
  private int offset;
  private int limit;

  private SearchScope scope;
  private Order sort;
  private String userId;
  
  private boolean isDL;
  private String allowCommunityId;
}
