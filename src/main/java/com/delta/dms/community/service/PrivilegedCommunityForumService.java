package com.delta.dms.community.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.dao.PrivilegedCommunityForumDao;
import com.delta.dms.community.swagger.model.PrivilegedCommunityForum;
import com.delta.dms.community.utils.Utility;

@Service
@Transactional
public class PrivilegedCommunityForumService {

  private UserGroupAdapter userGroupAdapter;
  private PrivilegedCommunityForumDao privilegeCommunityForumDao;

  @Autowired
  public PrivilegedCommunityForumService(
      UserGroupAdapter userGroupAdapter, PrivilegedCommunityForumDao privilegeCommunityForumDao) {
    this.userGroupAdapter = userGroupAdapter;
    this.privilegeCommunityForumDao = privilegeCommunityForumDao;
  }

  /*
   * This returned forumName is customized for PQM requirement to display in the drop-down list on
   * 2019-04-12.
   */
  public List<PrivilegedCommunityForum> getPrivilegeCommunityForumIds(int offset, int limit) {
    List<String> userIdList =
        userGroupAdapter.getPrivilegedUserGroupIdByUserIdAndFilter(
            Utility.getUserIdFromSession(), true);
    userIdList.add(Utility.getUserIdFromSession());
    return privilegeCommunityForumDao.getPrivilegedCommunityForumIds(userIdList, offset, limit);
  }
}
