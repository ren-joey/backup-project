package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.CommunitiesCreationInfo;
import com.delta.dms.community.dao.entity.CommunityData;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.CommunityMember;
import com.delta.dms.community.dao.entity.CommunityRoleInfo;
import com.delta.dms.community.dao.entity.CommunitySearchRequestEntity;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.UserInfo;
import com.delta.dms.community.swagger.model.ActiveMemberInfo;
import com.delta.dms.community.swagger.model.CommunityReviewDetail;
import com.delta.dms.community.swagger.model.DeleteApplicationDetail;

public interface CommunityDao {
  /**
   * select Community Information from dms_community.communities by communityId
   *
   * @param communityId
   * @return CommunityInfo
   */
  public List<CommunityInfo> getCommunityById(@Param("communityId") List<Integer> communityId);

  /**
   * select all Community Informations from dms_community.communities
   *
   * @return list of CommunityInfo
   */
  public List<CommunityInfo> getAll();

  public List<CommunityInfo> getAllValidDependentCommunity();

  /**
   * insert Community Information to dms_community.communities
   *
   * @param communityInfo
   * @return the number of insertion
   */
  public Integer add(CommunityInfo communityInfo);

  /**
   * update Community Information to dms_community.communities
   *
   * @param communityInfo
   * @return the number of influence of rows
   */
  public Integer update(CommunityInfo communityInfo);

  /**
   * delete Community Information to dms_community.communities
   *
   * @param communityInfo , the value of status change to delete
   * @return the number of influence of rows
   */
  public Integer delete(CommunityInfo communityInfo);

  public List<CommunityInfo> getCommunityByCategory(CommunitySearchRequestEntity request);

  public List<Map<String, Object>> countCommunityCategory(CommunitySearchRequestEntity request);

  /**
   * Add a member into the community
   *
   * @param groupId Group id
   * @param communityId Community id
   * @param roleId Role id
   * @return Numbers of the row affected
   */
  public int addRoleIntoCommunity(
      @Param("groupId") List<String> groupId,
      @Param("communityId") int communityId,
      @Param("roleId") int roleId,
      @Param("isGeneratedFromApp") boolean isGeneratedFromApp);

  /**
   * Add an application of joining the community from user
   *
   * @param communityId Community id
   * @param applicationId Application id
   * @param applicationDesc Application description
   * @param applicationTime Application time
   * @return
   */
  public int addUserIntoCommunityJoinReview(
      @Param("communityId") int communityId,
      @Param("applicationId") String applicationId,
      @Param("applicationDesc") String applicationDesc,
      @Param("applicationTime") long applicationTime);

  /**
   * Check whether the user is the manager of the community
   *
   * @param userId User id
   * @param communityId Community id
   * @param roleId Role id
   * @return 0 (Not the manager)/ 1 (Manager)
   */
  public int checkUserRoleOfCommunity(
      @Param("userId") String userId,
      @Param("communityId") int communityId,
      @Param("roleId") int roleId);

  /**
   * Get the un-approve user list
   *
   * @param communityId Community id
   * @return User list
   */
  public List<Map<String, String>> getApplicantList(@Param("communityId") int communityId);

  /**
   * Check whether the application exists
   *
   * @param communityId Community id
   * @param applicantId Applicant id
   * @return 0 (Doesn't exist)/ 1 (Exist)
   */
  public int checkApplicationExists(
      @Param("communityId") int communityId, @Param("applicantId") String applicantId);

  /**
   * Set the status into the application
   *
   * @param communityId Community id
   * @param applicantId Applicant id
   * @param reviewerId Reviewer id
   * @param reviewTime Review time
   * @return Numbers of the row affected
   */
  public int reviewTheMemberApplicationOfCommunity(
      @Param("communityId") int communityId,
      @Param("applicantId") String applicantId,
      @Param("reviewerId") String reviewerId,
      @Param("reviewTime") long reviewTime,
      @Param("status") String status);

  /**
   * Get the member list with sort and limit
   *
   * @param communityId Community id
   * @param offset Offset
   * @param limit Limit
   * @param sortField SortField (role/ name)
   * @return UserInfo list
   */
  public List<CommunityMember> getMemberListWithSortAndLimit(
      @Param("toGetCount") Boolean toGetCount,
      @Param("communityId") int communityId,
      @Param("toGetDmsMember") Boolean toGetDmsMember,
      @Param("roleId") int roleId, // negative = not specific
      @Param("name") String filterName,
      @Param("userIdList") List<String> userIdList,
      @Param("offset") Integer offset, // negative = no offset
      @Param("limit") Integer limit, // negative = no limit
      @Param("sortField") String sortField);

  /**
   * find all member in community db by community id
   *
   * @param communityId community id
   * @param filterName username
   * @return Community Member List
   */
  List<CommunityMember> findAllMembers(
      @Param("communityId") int communityId, @Param("name") String filterName);

  /**
   * insert Community Role Information to dms_community.community_role
   *
   * @param communityRoleInfo
   * @return the number of insertion
   */
  public Integer addCommunityRole(CommunityRoleInfo communityRoleInfo);

  /**
   * insert Community creation of member Information to dms_community.member
   *
   * @param batchId
   * @param type
   * @param userIds
   * @return the number of insertion
   */
  public Integer addMember(
      @Param("batchId") Integer batchId,
      @Param("type") String type,
      @Param("userIds") List<String> userIds);

  /**
   * insert Community creation Information to dms_community.communities_create_review
   *
   * @param communitiesCreationInfo
   * @return the number of insertion
   */
  public Integer addCommunitiesCreationInfo(CommunitiesCreationInfo communitiesCreationInfo);

  /**
   * update Community creation Information to dms_community.communities_create_review
   *
   * @param communitiesCreationInfo
   * @return the number of update
   */
  public Integer updateCommunitiesCreationInfo(CommunitiesCreationInfo communitiesCreationInfo);

  /**
   * update rejected message to communities_create_review
   *
   * @param batchId
   * @param rejectedMessage
   * @return the number of update
   */
  public Integer updateRejectedMessage(
      @Param("batchId") Integer batchId, @Param("rejectedMessage") String rejectedMessage);

  /**
   * transfer communities_create_review into communities
   *
   * @param communityData
   * @return the number of insert
   */
  public Integer transferCommunityData(CommunityData communityData);

  /**
   * transfer member into community_role
   *
   * @param adminGroupId
   * @param memberGroupId
   * @param batchId
   * @param communityId
   * @return the number of insert
   */
  public Integer transferCommunityRole(
      @Param("adminGroupId") String adminGroupId,
      @Param("memberGroupId") String memberGroupId,
      @Param("batchId") Integer batchId,
      @Param("communityId") Integer communityId);

  /**
   * get applied members for a community
   *
   * @param batchId
   * @return member list applied for this community
   */
  public List<UserInfo> getCommunityAppliedMember(
          @Param("batchId") Integer batchId);

  /**
   * get dependent group list (from MyDms) for a community
   *
   * @param communityId
   * @return dependent group list (from MyDms) for this community
   */
  public List<CommunityRoleInfo> getCommunityRoleList(
          @Param("roleId") int roleId, // negative = not specific
          @Param("isApplicationGroup") Boolean isApplicationGroup, // null = not specific
          @Param("communityId") Integer communityId);

  /**
   * get not from group list dependent group list (from MyDms) for a community
   *
   * @param communityId
   * @return not from group list dependent group list (from MyDms) for this community
   */
  public List<CommunityRoleInfo> getMainGroupCommunityRoleList(
          @Param("roleId") int roleId, // negative = not specific
          @Param("isApplicationGroup") Boolean isApplicationGroup, // null = not specific
          @Param("communityId") Integer communityId);

  /**
   * check Duplicate CommunityName from dms_community.communities
   *
   * @param communityName
   * @return the number of Duplicate CommunityName
   */
  public Integer checkDuplicateCommunityName(
      @Param("originalName") String originalName, @Param("communityName") String communityName);

  /**
   * check Duplicate CommunityName from dms_community.communities_create_review,
   * dms_community.communities
   *
   * @param batchId
   * @return the number of Duplicate CommunityName
   */
  public Integer checkDuplicateCommunityNameWithCommunities(@Param("batchId") int batchId);

  /**
   * select member's user_id from community_role
   *
   * @param communityId
   * @return the list of user_id
   */
  public List<String> getApplicantIdWithUncheck(@Param("communityId") Integer communityId);

  /**
   * select Community Information of community_img_banner from dms_community.communities by
   * communityId
   *
   * @param communityId
   * @return CommunityInfo(include community_img_banner info)
   */
  public CommunityInfo getCommunityImgBannerById(@Param("communityId") Integer communityId);

  /**
   * update Community Information of community_img_banner to dms_community.communities
   *
   * @param communityInfo
   * @return the number of influence of rows
   */
  public Integer updateCommunityImgBanner(CommunityInfo communityInfo);

  /**
   * get Community avatar
   *
   * @param communityId
   * @return
   */
  public String getCommunityAvatarById(@Param("communityId") Integer communityId);

  /**
   * update Community Information of community_img_avatar to dms_community.communities
   *
   * @param communityId
   * @param avatar
   * @return the number of influence of rows
   */
  public Integer updateCommunityImgAvatar(
      @Param("communityId") int communityId, @Param("avatar") String avatar);

  /**
   * get review Informations for Creating Community from dms_community.communities_create_review
   *
   * @param status status
   * @param offset offset
   * @param limit limit
   * @return list of CommunityReviewDetail
   */
  public List<CommunityReviewDetail> getAllCommunityCreationList(
      @Param("status") List<String> status,
      @Param("offset") Integer offset,
      @Param("limit") Integer limit);

  /**
   * select review Informations for Creating Community from dms_community.communities_create_review
   * by batch_id
   *
   * @param batchId batchId
   * @return CommunityReviewDetail
   */
  public CommunitiesCreationInfo getCommunityCreationById(@Param("batchId") Integer batchId);

  /**
   * Get all members from dms_community.community_role, dms_community.communities, but without
   * ApplicantId
   *
   * @param communityId Community id
   * @return UserID list
   */
  public List<String> getAllMemberWithoutApplicantIdById(@Param("communityId") int communityId);

  /**
   * update Community Information to dms_community.communities
   *
   * @param communityId community Id
   * @param memberId community_last_modified_user_id
   * @param milliseconds community_last_modified_time
   * @return the number of influence of rows
   */
  public Integer updateLastModifiedOfCommunity(
      @Param("communityId") Integer communityId,
      @Param("memberId") String memberId,
      @Param("milliseconds") long milliseconds);

  /**
   * check Duplicate communityGroupId from dms_community.communities
   *
   * @param communityGroupId
   * @return the number of Duplicate communityGroupId
   */
  public Integer checkcheckDuplicateCommunityGroupId(
      @Param("communityGroupId") String communityGroupId);

  /**
   * select Community Information from dms_community.communities by community_group_id
   *
   * @param groupId
   * @return CommunityInfo
   */
  public CommunityInfo getCommunityInfoByGroupId(@Param("communityGroupId") String groupId);

  /**
   * Delete the user from forum_join_review of the community
   *
   * @param communityId Community id
   * @param userId User id
   * @return Numbers of deleted item
   */
  public int deleteUserFromForumJoinReviewOfCommunity(
      @Param("communityId") int communityId, @Param("userId") List<String> userId);

  /**
   * Add application of deleting the community
   *
   * @param communityId Community id
   * @param communityName Community name
   * @param applicationId Applicant id
   * @param applicationDesc Application description
   * @param applicationTime Application time
   * @return Numbers of item inserted
   */
  public int addDeleteCommunityApplication(
      @Param("communityId") int communityId,
      @Param("communityName") String communityName,
      @Param("applicantId") String applicationId,
      @Param("applicationSubject") String applicationSubject,
      @Param("applicationDesc") String applicationDesc,
      @Param("applicationTime") long applicationTime);

  /**
   * Delete all join application of the community
   *
   * @param communityId Community id
   * @return Numbers of application deleted
   */
  public int deleteAllMemberJoinApplicationOfCommunity(@Param("communityId") int communityId);

  /**
   * Get the delete application of the community
   *
   * @param communityId Community id
   * @return Delete application detail
   */
  public DeleteApplicationDetail getDeleteApplication(@Param("communityId") int communityId);

  /**
   * Review the deleting application
   *
   * @param communityId Community id
   * @param userId Delete user id
   * @param time Delete time
   * @param status approved/rejected
   * @return Numbers of the application reviewed
   */
  public int reviewDeleteApplication(
      @Param("communityId") int communityId,
      @Param("userId") String userId,
      @Param("time") long time,
      @Param("status") String status,
      @Param("rejectedMessage") String rejectedMessage);

  /**
   * Get the application of deletion
   *
   * @param status Application status list
   * @return Result list
   */
  public List<DeleteApplicationDetail> getDeleteApplicationList(
      @Param("status") List<String> status,
      @Param("offset") Integer offset,
      @Param("limit") Integer limit);

  /**
   * Get all member group id of the community
   *
   * @param communityId Community id
   * @return Id list
   */
  public List<String> getMemberUserAndGroupOfCommunity(@Param("communityId") int communityId);

  /**
   * Get all admin group id of the community
   *
   * @param communityId Community id
   * @return Id list
   */
  public List<String> getAdminUserAndGroupOfCommunity(@Param("communityId") int communityId);

  /**
   * Get the total amounts of the applications
   *
   * @param status Status
   * @return Result number
   */
  public int countDeleteApplicationList(@Param("status") List<String> status);

  /**
   * Get the total amounts of the applications of community creation
   *
   * @param status Status
   * @return Result number
   */
  public int countCommunityCreationApplicationList(@Param("status") List<String> status);

  /**
   * Get the member list without gid
   *
   * @param communityId Community id
   * @return userId list
   */
  public List<String> getAllMemberWithoutGroupId(@Param("communityId") int communityId);

  /**
   * Get the member list without gid
   *
   * @param communityId Community id
   * @return userId list
   */
  public List<String> getAdminGroupOfCommunity(@Param("communityId") int communityId);

  /**
   * insert announcement text to community_announcement table
   *
   * @param communityId community id
   * @param text announcement text
   * @param userId user Id
   * @param time time
   * @return the number of insert
   */
  public Integer insertCommunityAnnouncementText(
      @Param("communityId") Integer communityId,
      @Param("text") String text,
      @Param("userId") String userId,
      @Param("time") long time);

  /**
   * delete announcement text to community_announcement table
   *
   * @param communityId community id
   * @return the number of delete
   */
  public Integer deleteCommunityAnnouncementText(@Param("communityId") Integer communityId);

  /**
   * get announcement text to community_announcement table
   *
   * @param communityId community id
   * @return text
   */
  public String getCommunityAnnouncement(@Param("communityId") Integer communityId);

  /**
   * get ActiveMember from replies table
   *
   * @param communityId community id
   * @return List of ActiveMemberInfo
   */
  public List<ActiveMemberInfo> getCommunityOfActiveMembers(
      @Param("communityId") int communityId,
      @Param("limit") int limit,
      @Param("start") int start,
      @Param("end") int end);

  /**
   * get language by batchId
   *
   * @param batchId
   * @return language
   */
  public String getLangByBatchId(@Param("batchId") Integer batchId);

  /**
   * insert Community notification type to dms_community.communities_create_review
   *
   * @param notificationType
   * @param batchId
   * @return the number of insertion
   */
  public Integer addCommunityNotificationType(
      @Param("batchId") Integer batchId, @Param("notificationType") String notificationType);

  /**
   * transfer member into community_role
   *
   * @param batchId
   * @param communityId
   * @return the number of insert
   */
  public Integer transferCommunitySetting(
      @Param("batchId") Integer batchId, @Param("communityId") Integer communityId);

  /**
   * update notification type to communities_setting
   *
   * @param communityId
   * @param notificationType
   * @return the number of update
   */
  public Integer updateCommunitySeting(
      @Param("communityId") Integer communityId,
      @Param("notificationType") String notificationType);

  /**
   * get notification type from communities_setting
   *
   * @param communityId
   * @return notificationType value
   */
  public String getCommunityNotificationType(@Param("communityId") Integer communityId);

  /**
   * insert Community notification type to dms_community.communities_setting
   *
   * @param notificationType
   * @param communityId
   * @param communityLanguage
   * @return the number of insertion
   */
  public Integer insertCommunityNotificationType(
      @Param("communityId") int communityId,
      @Param("notificationType") String notificationType,
      @Param("communityLanguage") String communityLanguage);

  /**
   * List the Community list with admin privilege
   *
   * @param userId
   * @return list of Community
   */
  public List<IdNameEntity> getPrivilegedAllCommunity(@Param("userId") String userId);

  /**
   * get community Id by CommunityName from dms_community.communities
   *
   * @param communityName
   * @return community Id
   */
  public int getCommunityIdByCommunityName(@Param("communityName") String communityName);
  
  public int getCommunityIdByForumId(@Param("forumId") int forumId);

  public int deleteGroupMemberInCommunity(@Param("communityId") int communityId);

  public List<CommunityRoleInfo> getUnintegratedUid();

  public List<CommunityInfo> getCommunityWithNoAppGroup(@Param("roleId") int roleId);

  public Integer updateCommunityGroupId(
          @Param("groupId") String groupId,
          @Param("communityId") Integer communityId,
          @Param("userId") String userId,
          @Param("roleId") Integer roleId);

  public List<CommunityInfo> getCommunityWithOrgId(
          @Param("startSyncTime") long startSyncTime,
          @Param("endSyncTime") long endSyncTime);

  public List<CommunityRoleInfo> getUserGroupMissingInfo(
          @Param("communityId") int communityId);

  public Integer updateCommunityLastOrgIdSyncTime(
          @Param("communityId") Integer communityId,
          @Param("lastSyncTime") long lastSyncTime);

  public long getLastSyncTimeByCommunityId(
          @Param("communityId") int communityId);

  public long getMinLastSyncTime(
          @Param("startSyncTime") long startSyncTime,
          @Param("endSyncTime") long endSyncTime);
}
