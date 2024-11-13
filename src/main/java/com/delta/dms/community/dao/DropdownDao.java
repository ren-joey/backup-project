package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.IdNameEntity;

public interface DropdownDao {

  public List<IdNameEntity> getAppFieldDropdownList(@Param("lang") String lang);
}
