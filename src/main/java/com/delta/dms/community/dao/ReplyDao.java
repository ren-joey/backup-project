package com.delta.dms.community.dao;

import static com.delta.dms.community.utils.I18nConstants.MSG_REPLY_NOT_EXIST;
import static java.util.Optional.ofNullable;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.AttachmentAppFieldEntity;
import com.delta.dms.community.dao.entity.AttachmentInfo;
import com.delta.dms.community.dao.entity.CustomReplyInfoForExcel;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.OrgIdWithUsers;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.swagger.model.ReplyDetail;

public interface ReplyDao {

  default ReplyInfo getReplyInfoById(int replyId) {
    return ofNullable(getReplyById(replyId))
        .orElseThrow(() -> new IllegalArgumentException(MSG_REPLY_NOT_EXIST));
  }

  /**
   * select Reply Information from dms_community.replies by replyId
   *
   * @param replyId
   * @return ReplyInfo
   */
  public ReplyInfo getReplyById(@Param("replyId") Integer replyId);

  /**
   * insert Reply Information(except text) to dms_community.replies
   *
   * @param reply
   * @return the number of insertion
   */
  public Integer addInfo(ReplyInfo reply);

  /**
   * insert Reply text to dms_community.replies
   *
   * @param reply
   * @return the number of insertion
   */
  public Integer addText(ReplyInfo reply);

  /**
   * update Reply Information(except text) to dms_community.replies
   *
   * @param reply
   * @return the number of influence of rows
   */
  public Integer updateInfo(ReplyInfo reply);

  /**
   * update Reply text to dms_community.replies
   *
   * @param reply
   * @return the number of influence of rows
   */
  public Integer updateText(ReplyInfo reply);

  /**
   * Get the reply list of the topic
   *
   * @param topicId Topic id
   * @param offset Offset
   * @param limit Limit
   * @param sortOrder Sort order of create time
   * @return ReplyInfo list
   */
  public List<ReplyInfo> getReplyListOfTopic(
      @Param("topicId") int topicId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortOrder") String sortOrder);

  /**
   * Add attachments of the reply
   *
   * @param replyId Reply id
   * @param attachmentId Attachment id
   * @param fileName File name
   * @param fileExt File extension
   * @param createTime Upload time
   * @return Numbers of attachments added
   */
  public int addAttachmentOfReply(
      @Param("replyId") int replyId,
      @Param("attachmentId") String attachmentId,
      @Param("fileName") String fileName,
      @Param("fileExt") String fileExt,
      @Param("createTime") long createTime);

  /**
   * get reply index from dms_community.replies by forum_id, follow_topic_id
   *
   * @param forumId forum id
   * @param followTopicId topic id
   * @return the index number
   */
  public Integer getReplyIndexById(
      @Param("forumId") Integer forumId, @Param("followTopicId") Integer followTopicId);

  /**
   * Count the numbers of replies of the topic
   *
   * @param topicId Topic id
   * @return Numbers of replies
   */
  public int countReplyOfTopic(@Param("topicId") int topicId);

  /**
   * Get id of attachments of the reply
   *
   * @param replyId Reply id
   * @return Attachment Id list
   */
  public List<String> getAttachmentIdOfReply(@Param("replyId") int replyId);

  /**
   * Add notification information of the reply
   *
   * @param replyId Reply id
   * @param notificationType Notification type
   * @param recipient Recipient
   * @return Numbers of rows added
   */
  public int addNotificationOfReply(
      @Param("replyId") int replyId,
      @Param("notificationType") String notificationType,
      @Param("recipient") String recipient);

  /**
   * Delete the attachment of the reply
   *
   * @param replyId Reply id
   * @param attachmentId Attachment id
   * @param userId User id
   * @param deleteTime Delete time
   * @return Numbers of attachments deleted
   */
  public int deleteAttachmentOfReply(
      @Param("replyId") int replyId,
      @Param("attachmentId") String attachmentId,
      @Param("userId") String userId,
      @Param("deleteTime") Long deleteTime);

  /**
   * Set the reply status to delete
   *
   * @param replyId Reply id
   * @param userId User id
   * @param deleteTime Delete time
   * @return Numbers of replies deleted
   */
  public int deleteReply(
      @Param("replyId") int replyId,
      @Param("userId") String userId,
      @Param("deleteTime") long deleteTime);

  /**
   * Get the notification type of the reply
   *
   * @param replyId Reply id
   * @return Result map
   */
  public Map<String, String> getNotificationOfReply(@Param("replyId") int replyId);

  /**
   * Get the reply list of the topic
   *
   * @param replyId Reply id
   * @param offset Offset
   * @param limit Limit
   * @param sortOrder Sort order of create time
   * @return ReplyInfo list
   */
  public List<ReplyInfo> getReplyListOfReply(
      @Param("replyId") int replyId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortOrder") String sortOrder);

  /**
   * Count the numbers of replies of the Reply
   *
   * @param replyId reply id
   * @return Numbers of replies
   */
  public int countReplyOfReply(@Param("replyId") int replyId);

  /**
   * Count the numbers of replies of the reply
   *
   * @param replyId Reply id
   * @return Numbers of kid replies
   */
  public int countKidNumOfReply(@Param("replyId") int replyId);

  public List<ReplyDetail> getReplyList(@Param("topicId") int topicId);

  public List<ReplyDetail> getNestedReplyList(@Param("replyId") int replyId);

  int upsertOrgMemberNotificationOfReply(
      @Param("replyId") int replyId, @Param("orgId") String orgId, @Param("users") String users);

  int upsertBgbuNotificationOfReply(
      @Param("replyId") int replyId, @Param("orgId") String orgId, @Param("users") String users);

  List<OrgIdWithUsers> getOrgMemberNotificationOfReply(@Param("replyId") int replyId);

  List<OrgIdWithUsers> getBgbuNotificationOfReply(@Param("replyId") int replyId);

  int deleteOrgMemberNotificationOfReply(@Param("replyId") int replyId);

  int deleteBgbuNotificationOfReply(@Param("replyId") int replyId);

  int insertReplyAttachmentAppField(
      @Param("attachmentId") String attachmentId,
      @Param("appFieldIdList") List<String> appFieldIdList);

  int deleteReplyAttachmentAppField(@Param("attachmentId") String attachmentId);

  List<IdNameEntity> getReplyAttachmentAppField(
      @Param("attachmentId") String attachmentId, @Param("lang") String lang);

  List<AttachmentAppFieldEntity> getReplyAllAttachmentAppField(
      @Param("attachmentIdList") List<String> attachmentIdList, @Param("lang") String lang);

  void insertReplyAttachmentRecordType(@Param("attachmentMap") Map<String, String> attachmentMap);

  List<IdNameEntity> getReplyAllAttachmentRecordType(
      @Param("attachmentIdList") List<String> attachmentIdList);

  List<AttachmentInfo> getReplyAttachments(@Param("replyId") int replyId);

  List<CustomReplyInfoForExcel> getRepliesByTopicIds(@Param("topicIds") List<String> topicIds);
}
