package com.delta.dms.community.dao;

import static com.delta.dms.community.utils.I18nConstants.MSG_TOPIC_NOT_EXIST;
import static java.util.Optional.ofNullable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.delta.dms.community.dao.entity.*;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.LatestTopic;
import com.delta.dms.community.swagger.model.ParticipatedTopic;
import com.delta.dms.community.swagger.model.Tag;
import com.delta.dms.community.swagger.model.TopicInformationOfBookmark;

public interface TopicDao {

  default TopicInfo getTopicInfoById(int topicId) {
    return ofNullable(getTopicById(topicId))
        .orElseThrow(() -> new IllegalArgumentException(MSG_TOPIC_NOT_EXIST));
  }

  default String getForumTypeByTopicId(int topicId) {
    return Optional.ofNullable(selectForumTypeByTopicId(topicId))
        .orElseThrow(NoSuchElementException::new);
  }

  /**
   * select Topic Information from dms_community.topics by topicId
   *
   * @param topicId
   * @return ForumInfo
   */
  public TopicInfo getTopicById(@Param("topicId") Integer topicId);

  /**
   * select Topic Information from dms_community.topics by ForumId
   *
   * @param forumId
   * @param topicStatus
   * @return list of TopicInfo
   */
  public List<TopicInfo> getAllByForumIdAndStatus(
      @Param("forumId") Integer forumId, @Param("topicStatus") String topicStatus);

  /**
   * insert topic Information(except text) to dms_community.topics
   *
   * @param topic
   * @return the number of insertion
   */
  public Integer addInfo(TopicInfo topic);

  /**
   * insert topic text to dms_community.topics
   *
   * @param topic
   * @return the number of insertion
   */
  public Integer addText(TopicInfo topic);

  /**
   * update topic Information(except text) to dms_community.topics
   *
   * @param topic
   * @return the number of influence of rows
   */
  public Integer updateInfo(TopicInfo topic);

  /**
   * update topic text to dms_community.topics
   *
   * @param topic
   * @return the number of influence of rows
   */
  public Integer updateText(TopicInfo topic);

  /**
   * delete forum Information to dms_community.forums
   *
   * @param topic
   * @return the number of influence of rows
   */
  public Integer delete(TopicInfo topic);

  /**
   * Get topics of the community
   *
   * @param communityId Community id
   * @param offset Offset
   * @param limit Limit
   * @param sortField Sort field
   * @param sortOrder Sort order
   * @param userId User id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return TopicInfo list
   */
  public List<TopicInfo> getTopicOfCommunityWithSortAndLimit(
      @Param("communityId") int communityId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder,
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("topicState") List<String> topicState,
      @Param("topicType") String topicType,
      @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Get the numbers of the topics in the community
   *
   * @param communityId Community id
   * @return Numbers of the topics
   */
  public int countTopicOfCommunity(
		  @Param("communityId") int communityId);

  /**
   * Get the numbers of the topics user can read in the community
   *
   * @param communityId Community id
   * @param userId User id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return Numbers of the topics
   */
  public int countUserCanReadTopicOfCommunity(
      @Param("communityId") int communityId,
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("topicState") List<String> topicState,
      @Param("topicType") String topicType,
	  @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Get the numbers of the topics in the forum
   *
   * @param forumId Forum id
   * @return Numbers of the topics
   */
  public int countTopicOfForum(
      @Param("forumId") int forumId,
      @Param("topicState") List<String> topicState,
      @Param("topicType") String topicType);

  /**
   * Get topics of the forum
   *
   * @param forumId Forum id
   * @param offset Offset
   * @param limit Limit
   * @param sortField Sort field
   * @param sortOrder Sort order
   * @return TopicInfo list
   */
  public List<TopicInfo> getTopicOfForumWithSortAndLimit(
      @Param("forumId") int forumId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder,
      @Param("topicState") List<String> topicState,
      @Param("topicType") String topicType);

  /**
   * Get topic information
   *
   * @param topicId Topic id
   * @return TopicInfo
   */
  public TopicInfo getTopicHomePage(@Param("topicId") int topicId, @Param("lang") String lang);

  /**
   * Add topic's view count
   *
   * @param topicId Topic id
   * @return Numbers of affected row
   */
  public int addViewCountOfTopic(@Param("topicId") int topicId);

  /**
   * Add tags of the topic
   *
   * @param topicId Topic id
   * @param tag Tag
   * @return Numbers of tags added
   */
  public int addTagOfTopic(@Param("topicId") int topicId, @Param("tag") List<String> tag);

  /**
   * Delete all tags of the topic
   *
   * @param topicId Topic id
   * @return Numbers of tags deleted
   */
  public int deleteTagOfTopic(@Param("topicId") int topicId);

  /**
   * Add attachments of the topic
   *
   * @param topicId Topic id
   * @param attachmentId Attachment id
   * @param fileName File name
   * @param fileExt File extension
   * @param createTime Upload time
   * @return Numbers of attachments added
   */
  public int addAttachmentOfTopic(
      @Param("topicId") int topicId,
      @Param("attachmentId") String attachmentId,
      @Param("fileName") String fileName,
      @Param("fileExt") String fileExt,
      @Param("createTime") long createTime);

  /**
   * Delete the attachment of the topic
   *
   * @param topicId Topic id
   * @param attachmentId Attachment id
   * @param userId User id
   * @param deleteTime Delete time
   * @return Numbers of attachments deleted
   */
  public int deleteAttachmentOfTopic(
      @Param("topicId") int topicId,
      @Param("attachmentId") String attachmentId,
      @Param("userId") String userId,
      @Param("deleteTime") Long deleteTime);

  /**
   * Get id of attachments of the topic
   *
   * @param topicId Topic id
   * @return Attachment Id list
   */
  public List<String> getAttachmentIdOfTopic(@Param("topicId") int topicId);

  /**
   * Check whether the title is duplicate in the forum
   *
   * @param forumId Forum id
   * @param title Title
   * @return Numbers of the topic with the same title
   */
  public int checkDuplicateTopicOfForum(
      @Param("forumId") int forumId,
      @Param("originalTitle") String originalTitle,
      @Param("title") String title);

  /**
   * Add notification information of the topic
   *
   * @param topicId Topic id
   * @param notificationType Notification type
   * @param recipient Recipient
   * @return Numbers of rows added
   */
  public int addNotificationOfTopic(
      @Param("topicId") int topicId,
      @Param("notificationType") String notificationType,
      @Param("recipient") String recipient);

  /**
   * update topic State to dms_community.topics
   *
   * @param topic
   * @return the number of influence of rows
   */
  public Integer updateState(TopicInfo topic);

  /**
   * Get the tag of the topic
   *
   * @param topicId Topic Id
   * @return List of tags
   */
  public List<Tag> getTagOfTopic(@Param("topicId") int topicId);

  /**
   * Update the last modified time of topic
   *
   * @param topicId Topic id
   * @param userId User id
   * @param time Last modified time
   * @return Numbers of rows updated
   */
  public int updateLastModifiedOfTopic(
      @Param("topicId") int topicId, @Param("userId") String userId, @Param("time") long time);

  /**
   * Get the latest topics of all community
   *
   * @param userId User id
   * @param offset Offset
   * @param limit Limit
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return Result list
   */
  public List<LatestTopic> getLatestTopicOfAllCommunity(
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("offset") int offset,
      @Param("limit") int limit,
	  @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Set the status of the topic and its replies locked
   *
   * @param topicId Topic id
   * @param userId User id
   * @param modifiedTime Modified time
   * @return Numbers of topics and replies modified
   */
  public int lockTopicAndItsReplies(
      @Param("topicId") int topicId,
      @Param("userId") String userId,
      @Param("modifiedTime") long modifiedTime);

  /**
   * Set the status of the topic and its replies open
   *
   * @param topicId Topic id
   * @param userId User id
   * @param modifiedTime Modified time
   * @param status status
   * @return Numbers of topics and replies modified
   */
  public int reopenTopicAndItsReplies(
      @Param("topicId") int topicId,
      @Param("userId") String userId,
      @Param("modifiedTime") long modifiedTime,
      @Param("status") String status);

  /**
   * Get topics of the userId
   *
   * @param userId user id
   * @param offset Offset
   * @param limit Limit
   * @param sortField Sort field
   * @param sortOrder Sort order
   * @return TopicInformationOfBookmark list
   */
  public List<TopicInformationOfBookmark> getTopicOfUserWithSortAndLimit(
      @Param("userId") String userId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder);

  /**
   * Get the numbers of the topics from userId
   *
   * @param userId
   * @return Numbers of the topics
   */
  public int countTopicOfUser(@Param("userId") String userId);

  /**
   * Set the situation of the topic
   *
   * @param topicId Topic id
   * @param topicSituation Topic situation
   * @return Numbers of topics updated
   */
  public int setTopicSituation(
      @Param("topicId") int topicId, @Param("topicSituation") String topicSituation);

  /**
   * Get the latest participated topics of the user
   *
   * @param userId User id
   * @param offset Offset
   * @param limit Limit
   * @return The result topic list
   */
  public List<ParticipatedTopic> getParticipatedTopicOfUser(
      @Param("userId") String userId, @Param("offset") int offset, @Param("limit") int limit);

  /**
   * Count the numbers of topping topic in the forum
   *
   * @param forumId Forum id
   * @return Numbers of topping topic
   */
  public int countToppingTopicOfForum(@Param("forumId") int forumId);

  /**
   * Topping the topic
   *
   * @param topicId Topic id
   * @return Numbers of updated row
   */
  public int toppingTopic(@Param("topicId") int topicId);

  /**
   * Remove the topping topic
   *
   * @param topicId Topic id
   * @return Numbers of updated row
   */
  public int unToppingTopic(@Param("topicId") int topicId);

  /**
   * Swap two topics topping order
   *
   * @param topicId Topic id
   * @param toppingOrder Topping order
   * @return Numbers of updated row
   */
  public int swapToppingOrderOfTopic(
      @Param("topicId") int topicId, @Param("toppingOrder") int toppingOrder);

  /**
   * Get the hot topics
   *
   * @param communityId Community id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return Topics list
   */
  public List<TopicInfo> getHotPublicTopicOfCommunity(
      @Param("communityId") int communityId,
      @Param("offset") int offset,
      @Param("limit") int limit,
	  @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId,
      @Param("hotLastingSeconds") Integer hotLastingSeconds);

  /**
   * Get topping topics in the forum
   *
   * @param forumId Forum id
   * @return Topic list
   */
  public List<TopicInfo> getToppingTopicByForumId(
      @Param("forumId") int forumId, @Param("offset") int offset, @Param("limit") int limit);

  /**
   * update ForumId to dms_community.topics and dms_communityreplies by forumId and TopicId
   *
   * @param topicId
   * @param orignForumId
   * @param newForumId
   * @return the number of influence of rows
   */
  public Integer updateForumIdForTopicAndRepliesById(
      @Param("topicId") int topicId,
      @Param("orignForumId") int orignForumId,
      @Param("newForumId") int newForumId);

  public List<ConclusionAlertTopicInfo> getAllNeedConclusionAlertTopic(
      @Param("forumId") List<Integer> forumId,
      @Param("startDay") int startDay,
      @Param("endDay") int endDay);

  public boolean getConclusionAlertByTopicType(@Param("topicType") String topicType);

  public List<TopicTypeEntity> getTopicTypeByCommunityId(
      @Param("communityId") int communityId, @Param("lang") String lang);

  public List<TopicTypeEntity> getTopicTypeByCommunityIdAndForumId(
      @Param("communityId") int communityId,
      @Param("forumId") int forumId,
      @Param("lang") String lang);

  public List<ConclusionStateEntity> getConclusionState(
      @Param("topicTypeId") List<Integer> topicTypeId, @Param("lang") String lang);

  public List<TopicInfo> getConcludedTopicByTopicType(
      @Param("topicType") List<String> topicType,
      @Param("startTime") long startTime,
      @Param("endTime") long endTime);

  int upsertOrgMemberNotificationOfTopic(
      @Param("topicId") int topicId, @Param("orgId") String orgId, @Param("users") String users);

  int upsertBgbuNotificationOfTopic(
      @Param("topicId") int topicId, @Param("orgId") String orgId, @Param("users") String users);

  List<OrgIdWithUsers> getOrgMemberNotificationOfTopic(@Param("topicId") int topicId);

  List<OrgIdWithUsers> getBgbuNotificationOfTopic(@Param("topicId") int topicId);

  int deleteOrgMemberNotificationOfTopic(@Param("topicId") int topicId);

  int deleteBgbuNotificationOfTopic(@Param("topicId") int topicId);

  int insertTopicAppField(
      @Param("topicId") int topicId, @Param("appFieldIdList") List<String> appFieldIdList);

  int deleteTopicAppField(@Param("topicId") int topicId);

  List<IdNameEntity> getTopicAppField(@Param("topicId") int topicId, @Param("lang") String lang);

  int insertTopicAttachmentAppField(
      @Param("attachmentId") String attachmentId,
      @Param("appFieldIdList") List<String> appFieldIdList);

  int deleteTopicAttachmentAppField(@Param("attachmentId") String attachmentId);

  List<IdNameEntity> getTopicAttachmentAppField(
      @Param("attachmentId") String attachmentId, @Param("lang") String lang);

  List<AttachmentAppFieldEntity> getTopicAllAttachmentAppField(
      @Param("attachmentIdList") List<String> attachmentIdList, @Param("lang") String lang);

  String selectForumTypeByTopicId(@Param("topicId") int topicId);

  List<AttachmentInfo> getTopicAttachments(@Param("topicId") int topicId);

  List<CustomTopicInfoForExcel> getTopicsByForumIds(@Param("forumIds") List<Integer> forumIds);
}
