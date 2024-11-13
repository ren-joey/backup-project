package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.delta.dms.community.dao.entity.*;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.Tag;
import com.delta.dms.community.swagger.model.User;

public interface ForumDao {
  /**
   * select forum Information from dms_community.forums by forumId
   *
   * @param forumId
   * @return ForumInfo
   */
  public ForumInfo getForumById(@Param("forumId") Integer forumId, @Param("lang") String lang);

  public List<TopicTypeEntity> getForumSupportTopicType(
      @Param("forumId") Integer forumId,
      @Param("communityId") Integer communityId,
      @Param("lang") String lang);

  /**
   * select all forum Informations from dms_community.forums by communityId
   *
   * @param communityId
   * @param forumStatus
   * @return list of ForumInfo
   */
  public List<ForumInfo> getAllByCommunityIdAndStatus(
      @Param("communityId") Integer communityId, @Param("forumStatus") String forumStatus);

  /**
   * insert forum Information to dms_community.forums
   *
   * @param forum
   * @return the number of insertion
   */
  public Integer add(ForumInfo forum);

  /**
   * update forum Information to dms_community.forums
   *
   * @param forum
   * @return the number of influence of rows
   */
  public Integer update(ForumInfo forum);

  /**
   * delete forum Information to dms_community.forums
   *
   * @param forum forum information
   * @return the number of influence of rows
   */
  public Integer delete(ForumInfo forum);

  /**
   * Get the Open forums of community
   *
   * @param userId User id
   * @param forumType Forum type
   * @param offset Offset
   * @param limit Limit
   * @param sortField Sort field
   * @param sortOrder Sort order
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return ForumInfo list
   */
  public List<ForumInfo> getForumOfCommunityByTypeWithSortAndLimit(
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("communityId") int communityId,
      @Param("forumType") List<String> forumType,
      @Param("offset") Integer offset,
      @Param("limit") Integer limit,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder,
      @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Get the Open toping forums of community
   *
   * @param communityId community id
   * @param forumType Forum type
   * @return ForumInfo list
   */
  public List<ForumInfo> getTopingForumOfCommunityByTypeWithSortAndLimit(
      @Param("communityId") int communityId, @Param("forumType") List<String> forumType);

  /**
   * Count the number of all forums in the community
   *
   * @param communityId Community id
   * @param forumType Forum type
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return the number of the forums
   */
  public Integer countForumOfCommunity(
      @Param("communityId") int communityId, 
      @Param("forumType") List<String> forumType,
	  @Param("isDL") boolean isDL,
	  @Param("allowForumId") String allowForumId);

  /**
   * Check whether the application exists
   *
   * @param forumId Forum id
   * @param applicantId Applicant id
   * @return 0 (Doesn't exist)/ 1 (Exist)
   */
  public int checkApplicationExists(
      @Param("forumId") int forumId, @Param("applicantId") String applicantId);

  /**
   * Add an application of joining the forum from user
   *
   * @param forumId Forum id
   * @param applicationId Application id
   * @param applicationDesc Application description
   * @param applicationTime Application time
   * @return
   */
  public int addUserIntoForumJoinReview(
      @Param("forumId") int forumId,
      @Param("applicationId") String applicationId,
      @Param("applicationDesc") String applicationDesc,
      @Param("applicationTime") long applicationTime);

  /**
   * Get the un-approve user list
   *
   * @param forumId Forum id
   * @return User list
   */
  public List<Map<String, String>> getApplicantList(@Param("forumId") int forumId);

  /**
   * Set the status into the application
   *
   * @param forumId Forum id
   * @param applicantId Applicant id
   * @param reviewerId Reviewer id
   * @param reviewTime Review time
   * @return Numbers of the row affected
   */
  public int reviewTheMemberApplicationOfForum(
      @Param("forumId") int forumId,
      @Param("applicantId") String applicantId,
      @Param("reviewerId") String reviewerId,
      @Param("reviewTime") long reviewTime,
      @Param("status") String status);

  /**
   * Add a role into the forum
   *
   * @param groupId Group id
   * @param forumId Forum id
   * @return Numbers of the row affected
   */
  public int addRoleIntoForum(
      @Param("groupId") List<String> groupId,
      @Param("forumId") int forumId,
      @Param("roleId") int roleId);

  /**
   * Get the member list of the forum
   *
   * @param forumId Forum id
   * @return Member list
   */
  public List<User> getMemberListWithSortAndLimit(
          @Param("toGetCount") Boolean toGetCount,
          @Param("forumId") int forumId,
          @Param("toIncludeDmsMember") Boolean toIncludeDmsMember,
          @Param("roleIdList") List<Integer> roleIdList,
          @Param("name") String filterName,
          @Param("userIdList") List<String> userIdList,
          @Param("excludeUserIdList") List<String> excludeUserIdList,
          @Param("offset") Integer offset, // negative = no offset
          @Param("limit") Integer limit, // negative = no limit
          @Param("sortField") String sortField);

  public List<User> getNotMemberListWithSortAndLimit(
          @Param("toGetCount") Boolean toGetCount,
          @Param("forumId") int forumId,
          @Param("toIncludeDmsMember") Boolean toIncludeDmsMember,
          @Param("roleIdList") List<Integer> roleIdList,
          @Param("name") String filterName,
          @Param("userIdList") List<String> userIdList,
          @Param("excludeUserIdList") List<String> excludeUserIdList,
          @Param("offset") Integer offset, // negative = no offset
          @Param("limit") Integer limit, // negative = no limit
          @Param("sortField") String sortField);

  /**
   * check duplicated forumName from dms_community.forums
   *
   * @param communityId Community Id
   * @param forumName Forum Name
   * @return Numbers of the row
   */
  public Integer checkDuplicateForumName(
      @Param("communityId") int communityId,
      @Param("originalName") String originalName,
      @Param("forumName") String forumName);

  /**
   * Add a tag into the forum_tag
   *
   * @param forumId Forum id
   * @param forumTags forum tag list
   * @return Numbers of the row affected
   */
  public Integer addTagIntoForumTag(
      @Param("forumId") Integer forumId, @Param("forumTags") List<String> forumTags);

  /**
   * delete all members from forum_role
   *
   * @param forumId Forum id
   * @return Numbers of the row deleted
   */
  public Integer deleteAllMemberByforumId(@Param("forumId") int forumId);

  /**
   * delete tags from forum_tag
   *
   * @param forumId Forum id
   * @return Numbers of the row deleted
   */
  public Integer deleteAllTags(@Param("forumId") Integer forumId);

  /**
   * Update the last modified time of Forum
   *
   * @param forumId Forum id
   * @param userId User id
   * @param time Last modified time
   * @return Numbers of rows updated
   */
  public int updateLastModifiedOfForum(
      @Param("forumId") int forumId, @Param("userId") String userId, @Param("time") long time);

  /**
   * Get the tag of the forum
   *
   * @param forumId Forum Id
   * @return List of tags
   */
  public List<Tag> getTagOfForum(@Param("forumId") int forumId);

  /**
   * Delete all join application of the forum
   *
   * @param forumId Forum id
   * @return Numbers of application deleted
   */
  public int deleteAllMemberJoinApplicationOfForum(@Param("forumId") int forumId);

  /**
   * select top forum
   *
   * @param communityId community Id
   * @return Numbers of TopForum
   */
  public int countMaxNumberOfTopForum(@Param("communityId") int communityId);

  /**
   * update top forum
   *
   * @param communityId community Id
   * @param forumId Forum id
   * @return Numbers of rows updated
   */
  public int updateTopForum(@Param("communityId") int communityId, @Param("forumId") int forumId);

  /**
   * update top forum
   *
   * @param forumId Forum id
   * @return Numbers of rows updated
   */
  public int updateNormalForum(@Param("forumId") int forumId);

  /**
   * update priority of forum
   *
   * @param forumId Forum id
   * @param toppingOrder top Order
   * @return Numbers of rows updated
   */
  public int swapPriorityOfForum(
      @Param("forumId") int forumId, @Param("toppingOrder") int toppingOrder);

  /**
   * Get the Open hot forums of community
   *
   * @param communityId community id
   * @param offset offset
   * @param limit limit
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return ForumInfo list
   */
  public List<ForumInfo> getHotForumOfCommunity(
      @Param("communityId") int communityId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId,
      @Param("hotLastingSeconds") Integer hotLastingSeconds);

  /**
   * Get forum list that user can post the topic in the community
   *
   * @param communityId Community
   * @param userId User id
   * @return Forum list
   */
  public List<IdNameEntity> getPrivilegedForumOfCommunity(
      @Param("communityId") int communityId,
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId);

  /**
   * Get the topping forums of community
   *
   * @param communityId community id
   * @param offset offset
   * @param limit limit
   * @return ForumInfo list
   */
  public List<ForumInfo> getToppingForumOfCommunity(
      @Param("communityId") int communityId,
      @Param("offset") int offset,
      @Param("limit") int limit);

  public Set<Integer> getApplicantForumsByUserId(@Param("userId") String userId);

  public List<RoleDetailEntity> getRolesByForumIds(
      @Param("userId") String userId, @Param("forumIds") List<Integer> forumIds);

  public List<RoleDetailEntity> getForumRole(
          @Param("forumId") int forumId,
          @Param("roleIdList") List<Integer> roleIdList);

  public List<RoleDetailEntity> getMainGroupRoleListOfForum(
          @Param("forumId") int forumId,
          @Param("roleIdList") List<Integer> roleIdList);

  public List<RoleDetailEntity> getUnintegratedUid();

  public List<ForumInfo> getForumWithNoAppGroup(@Param("roleId") int roleId);

  public List<RoleDetailEntity> getAllForumRoleByCommunity(
          @Param("communityId") int communityId
  );

  public int updateForumGroupId(
          @Param("groupId") String groupId,
          @Param("forumId") int forumId,
          @Param("userId") String userId,
          @Param("roleId") int roleId);

  public List<ForumInfo> getAll();

  public List<RoleDetailEntity> getUserGroupMissingInfo(
          @Param("forumId") int forumId
  );

  List<Integer> getForumIdsByCommunityId(@Param("communityId") int communityId);

}
