package com.delta.dms.community.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.dao.TagDao;
import com.delta.dms.community.swagger.model.Tag;

@Service
@Transactional
public class TagService {
  private TagDao tagDao;

  public TagService(TagDao tagDao) {
    this.tagDao = tagDao;
  }

  public List<Tag> getTags(String q, Integer limit, List<String> exclude) {
    return tagDao.getTags(q, limit, exclude);
  }
}
