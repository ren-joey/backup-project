package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.Tag;

@FunctionalInterface
public interface TagDao {
  /**
   * get the range of tag Informations from dms_community.forum_tag
   *
   * @param q query
   * @limit the number of limit
   * @exclude exclude some tag
   * @return list of Tag
   */
  public List<Tag> getTags(
      @Param("q") String q, @Param("limit") Integer limit, @Param("exclude") List<String> exclude);
}
