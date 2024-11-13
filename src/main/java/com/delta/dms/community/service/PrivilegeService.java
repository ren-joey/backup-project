package com.delta.dms.community.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.PermissionDao;

@Service
public class PrivilegeService {

  private UserService userService;
  private PermissionDao permissionDao;

  @Autowired
  public PrivilegeService(UserService userService, PermissionDao permissionDao) {
    this.userService = userService;
    this.permissionDao = permissionDao;
  }

  public boolean checkUserPrivilege(
      String userId, int communityId, int forumId, String object, String operation) {
    return userService.isSysAdmin(userId)
        || permissionDao.checkUserPrivilege(userId, communityId, forumId, object, operation);
  }
}
