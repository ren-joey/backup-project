/***********************************************************
 *  Created: 2024/01
 *  Author: Yvonne.Zheng
 *  Goal: 爲了將 uid 搬移到 userGroup, community 只記錄 gid，
 *        需要將數個項目的舊有資料重新整合。
 */
package com.delta.dms.community.service;

import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.CommunityDao;
import com.delta.dms.community.dao.ForumDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.CommunityRoleInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.RoleDetailEntity;
import com.delta.dms.community.enums.Role;
import com.delta.dms.community.swagger.model.UpdateAttachedCommunityData;
import com.delta.dms.community.utils.Utility;
import com.esotericsoftware.minlog.Log;
import lombok.RequiredArgsConstructor;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationService {
  private final CommunityDao communityDao;
  private final CommunityService communityService;
  private final ForumDao forumDao;
  private final UserGroupAdapter userGroupAdapter;
  private final YamlConfig yamlConfig;
  private final EventPublishService eventPublishService;

  /**
   * 將所有 community 逐一檢查（主要針對舊有 community），
   *  針對經理人及成員，如果沒有對應的 userGroup gid，
   *  則分別建立對應 group 並存下 gid
   *
   * @return      成功建立的縂數量
   */
  public int createCommunityAppGroup() {
    int updatedRowCount = 0;
    updatedRowCount += createCommunityAppGroupByRole(Role.COMMUNITY_ADMIN);
    updatedRowCount += createCommunityAppGroupByRole(Role.COMMUNITY_MEMBER);
    return updatedRowCount;
  }

  /**
   * Create userGroup gid of specified role for community.
   * Return num of successfully created & saved gid.
   */
  private int createCommunityAppGroupByRole(Role role) {
    if(!role.equals(Role.COMMUNITY_ADMIN) && !role.equals(Role.COMMUNITY_MEMBER)) {
      return 0; // not valid role
    }
    int updatedRowCount = 0;
    List<CommunityInfo> noGroupRows = null;
    do {
      noGroupRows = this.communityDao.getCommunityWithNoAppGroup(role.getId());
      for(CommunityInfo row : noGroupRows) {
        String groupId = this.userGroupAdapter.createUserGroup(
                role, row.getCommunityId(), this.yamlConfig.getAppGroupRootId(),null);
        if(!groupId.isEmpty()) {
          CommunityRoleInfo communityRoleInfo = new CommunityRoleInfo();
          communityRoleInfo.setCommunityId(row.getCommunityId());
          communityRoleInfo.setGroupId(groupId);
          communityRoleInfo.setRoleId(role.getId());
          communityRoleInfo.setIsGeneratedFromApp(true);
          updatedRowCount += this.communityDao.addCommunityRole(communityRoleInfo);
        }
      }
      System.gc();
    } while(!noGroupRows.isEmpty());
    return updatedRowCount;
  }

  /**
   * 將所有 forum 逐一檢查（主要針對舊有 forum），
   *  針對經理人及成員，如果沒有對應的 userGroup gid，
   *  則分別建立對應 group 並存下 gid
   *
   * @return      成功建立的縂數量
   */
  public int createForumAppGroup() {
    int updatedRowCount = 0;
    updatedRowCount += createForumAppGroupByRole(Role.FORUM_ADMIN);
    updatedRowCount += createForumAppGroupByRole(Role.FORUM_MEMBER);
    return updatedRowCount;
  }

  /**
   * Create userGroup gid of specified role for forum.
   * Return num of successfully created & saved gid.
   */
  private int createForumAppGroupByRole(Role role) {
    if(!role.equals(Role.FORUM_ADMIN) && !role.equals(Role.FORUM_MEMBER)) {
      return 0; // not valid role
    }
    int updatedRowCount = 0;
    List<ForumInfo> noGroupRows = null;
    do {
      noGroupRows = this.forumDao.getForumWithNoAppGroup(role.getId());
      for(ForumInfo row : noGroupRows) {
        String groupId = this.userGroupAdapter.createUserGroup(
                role, row.getForumId(), this.yamlConfig.getAppGroupRootId(),null);
        if(!groupId.isEmpty()) {
          updatedRowCount += this.forumDao.addRoleIntoForum(
                  Collections.singletonList(groupId), row.getForumId(), role.getId());
        }
      }
      System.gc();
    } while(!noGroupRows.isEmpty());
    return updatedRowCount;
  }

  /**
   * 將所有 community 舊有會員 (restore data) 跟新 userGroup 内會員做比對，
   *  針對經理人及成員，如果沒有對應的 userGroup gid，則分別建立對應 group 並存下 gid，
   *  將每一 community 找出是舊經理人及成員，但尚未存在 userGroup 的 uid 查缺補漏
   *
   * @return      成功更新的縂 community 數量
   */
  public int integrateCommunityRole() {
    int updatedRowCount = 0;
    List<CommunityInfo> allCommunities = this.communityDao.getAll();
    for(CommunityInfo item : allCommunities) {
      List<CommunityRoleInfo> unintegratedRows = this.communityDao.getUserGroupMissingInfo(item.getCommunityId());
      Map<String, List<CommunityRoleInfo>> groupedUserList =
              unintegratedRows.stream().collect(Collectors.groupingBy(CommunityRoleInfo::getGroupId));

      for (String groupId : groupedUserList.keySet()){
        List<String> userIds  = groupedUserList.get(groupId)
                .parallelStream()
                .map(CommunityRoleInfo::getUserId)
                .collect(Collectors.toList()) ;
        this.userGroupAdapter.appendUserGroupMembers(
                groupId,
                userIds);
      }
      updatedRowCount += 1;
    }
    System.gc();
    return updatedRowCount;
  }

  /**
   * 將所有 forum 舊有會員 (restore data) 跟新 userGroup 内會員做比對，
   *  針對經理人及成員，如果沒有對應的 userGroup gid，則分別建立對應 group 並存下 gid，
   *  將每一 forum 找出是舊經理人及成員，但尚未存在 userGroup 的 uid 查缺補漏
   *
   * @return      成功更新的縂 forum 數量
   */
  public int integrateForumRole() {
    int updatedRowCount = 0;
    List<ForumInfo> forumInfos = this.forumDao.getAll();
    for(ForumInfo item : forumInfos) {
      List<RoleDetailEntity> unintegratedRows = this.forumDao.getUserGroupMissingInfo(item.getForumId());
      Map<String, List<RoleDetailEntity>> userlistGrouped =
              unintegratedRows.stream().collect(Collectors.groupingBy(RoleDetailEntity::getGroupId));

      for (String groupId : userlistGrouped.keySet()){
        List<String> userIds  = userlistGrouped.get(groupId)
                .parallelStream()
                .map(RoleDetailEntity::getUserId)
                .collect(Collectors.toList()) ;
        this.userGroupAdapter.appendUserGroupMembers(
                groupId,
                userIds);
      }
      updatedRowCount += 1;
      System.gc();
    }
    return updatedRowCount;
  }

  /**
   * Create or retrieve userGroup gid of specified role for community.
   * Return num of successfully updated & saved gid for uid.
   */
  public int updateCommunityGid(CommunityRoleInfo row) {
    if(Objects.isNull(row) || row.getRoleId() == 0 || StringUtil.isBlank(row.getUserId())
            || row.getCommunityId() == 0 ) {
      return -1; // not valid community info
    }
    String groupId = generateGroupId(row.getRoleId(), row.getUserId(), row.getCommunityId());
    return this.communityDao.updateCommunityGroupId(groupId, row.getCommunityId(), row.getUserId(), row.getRoleId());
  }

  /**
   * Create or retrieve userGroup gid of specified role for forum.
   * Return num of successfully updated & saved gid for uid.
   */
  public int updateForumGid(RoleDetailEntity row) {
    if(Objects.isNull(row) || row.getRoleId() == 0 || StringUtil.isBlank(row.getUserId())
            || row.getForumId() == 0 ) {
      return -1; // not valid forum info
    }
    String groupId = generateGroupId(row.getRoleId(), row.getUserId(), row.getForumId());
    return this.forumDao.updateForumGroupId(groupId, row.getForumId(), row.getUserId(), row.getRoleId());
  }

  /**
   * Create or retrieve userGroup gid of specified role for community/forum.
   */
  private String generateGroupId(int roleId, String userId, int associatedId) {
    if(StringUtil.isBlank(userId) || associatedId == 0) {
      return null; // not valid combination to generate a gid
    }

    Role role = Role.getRoleByValue(roleId);
    if(roleId == 0 || role == null) {
      return null; // not valid role
    }

    String groupId = getCurrentGroupId(role, associatedId);
    if (!Objects.equals(groupId, "")) {
      this.userGroupAdapter.appendUserGroupMembers(
              groupId,
              Collections.singletonList(userId));
    } else {
      groupId = this.userGroupAdapter.createUserGroup(
              role, associatedId, this.yamlConfig.getAppGroupRootId(),
              Collections.singletonList(userId));
    }
    return groupId;
  }

  /**
   * Retrieve userGroup gid of specified role for community/forum.
   */
  private String getCurrentGroupId(Role role, int associatedId) {
    if(Objects.isNull(role) || associatedId == 0 ) {
      return ""; // not valid combination to retrieve a gid
    }

    String groupId = "";
    if (role.equals(Role.COMMUNITY_ADMIN) || role.equals(Role.COMMUNITY_MEMBER))  {
      Optional<CommunityRoleInfo> optional = this.communityDao.getCommunityRoleList(
              role.getId(), true, associatedId)
              .stream()
              .findFirst();
      if (optional.isPresent()) {
        groupId = optional.get().getGroupId();
      }
    } else {
      Optional<RoleDetailEntity> optional = this.forumDao.getForumRole(
                      associatedId, Collections.singletonList(role.getId()))
              .stream()
              .findFirst();
      if (optional.isPresent()) {
        groupId = optional.get().getGroupId();
      }
    }
    return groupId;
  }

  /**
   * 將所有 ddf 舊有會員權限資料刷新，套用新的 gid
   *  以 community 為單位，更新全部内含 forum 及 topic 及附件
   *
   * @return      成功更新的縂 community 數量
   */
  public int integrateDdf() {
    int updatedRowCount = 0;
    List<CommunityInfo> allCommunityInfo = this.communityDao.getAll();
    for(CommunityInfo item : allCommunityInfo) {
      this.eventPublishService.publishCommunityChangingEvent(item.getCommunityId());
      updatedRowCount += 1;
    }
    System.gc();
    return updatedRowCount;
  }

  /**
   * 將所有依附型群組内含會員以群組方式存放
   *
   * @return      成功更新的縂依附型 community 數量
   */
  public int integrateOrgCommunity(Long startTime, Long endTime) {
    int updatedRowCount = 0;
    UpdateAttachedCommunityData user = new UpdateAttachedCommunityData();
    user.setUpdateUserId(Utility.getUserIdFromSession());

    // retrieve and process old derivative communities
    List<CommunityInfo> unintegratedRows = null;
    do {
      endTime = (null == endTime) ? System.currentTimeMillis() : endTime;
      unintegratedRows = this.communityDao.getCommunityWithOrgId(startTime, endTime);
      if (!unintegratedRows.isEmpty()) {
        for(CommunityInfo item : unintegratedRows) {
          if(!Objects.equals(item.getCommunityGroupId(), "")) {
            int status = this.communityService.updateAttachedCommunity(item.getCommunityGroupId(), user);
            if(HttpStatus.OK.value() == status) {
              updatedRowCount += 1;
            } else if(HttpStatus.NO_CONTENT.value() == status){
              Log.warn(item.getCommunityGroupId());
            } else {
              Log.error(item.getCommunityGroupId());
            }
          }
        }
        startTime = this.communityDao.getMinLastSyncTime(startTime-1, endTime+1) -1; // get min sync time in range
        System.gc();
      }
    } while(!unintegratedRows.isEmpty());
    return updatedRowCount;
  }
}
