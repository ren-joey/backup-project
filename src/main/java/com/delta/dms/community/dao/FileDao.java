package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.AttachmentDetail;
import com.delta.dms.community.swagger.model.User;

public interface FileDao {
  /**
   * Get updated file and privilege
   *
   * @return
   */
  public List<Map<String, Object>> getFilePrivilegeUpdateList();

  /**
   * Get all attachments of forums in the community
   *
   * @param communityId Community id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return List of attachment id
   */
  public List<String> getAttachmentOfCommunity(
		  @Param("communityId") int communityId,
	      @Param("isDL") boolean isDL,
	      @Param("allowForumId") String allowForumId);

  /**
   * Get the detail information of the attachment
   *
   * @param attachmentId Attachment id
   * @return Detail information of the attachment
   */
  public AttachmentDetail getAttachmentDetail(@Param("attachmentId") String attachmentId);

  /**
   * Get all attachments that user can read of the community
   *
   * @param userId User id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return List of attachment id
   */
  public List<String> getOwnAttachmentOfCommunity(
      @Param("communityId") int communityId,
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortField") String sortField,
      @Param("sortOrder") String sortOrder,
      @Param("fileExt") String fileExt,
      @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Get the numbers of the attachment user can read in the community
   *
   * @param communityId Community id
   * @param userId User id
   * @param isDL DL check
   * @param allowForumId allow forum id
   * @return
   */
  public int countOwnAttachmentOfCommunity(
      @Param("communityId") int communityId,
      @Param("isSysAdmin") boolean isSysAdmin,
      @Param("userId") String userId,
      @Param("fileExt") String fileExt,
      @Param("isDL") boolean isDL,
      @Param("allowForumId") String allowForumId);

  /**
   * Get topicId by attachmentId
   *
   * @param attachmentId
   * @return topicId
   */
  public Integer getTopicIdbyAttachmentId(@Param("attachmentId") String attachmentId);

  /**
   * Get ForumType by attachmentId
   *
   * @param attachmentId
   * @return forumType
   */
  public String getForumTypeFromTopicbyAttachmentId(@Param("attachmentId") String attachmentId);

  /**
   * Get replyId by attachmentId
   *
   * @param attachmentId
   * @return replyId
   */
  public Integer getReplyIdbyAttachmentId(@Param("attachmentId") String attachmentId);

  /**
   * Get ForumType by attachmentId
   *
   * @param attachmentId
   * @return forumType
   */
  public String getForumTypeFromReplybyAttachmentId(@Param("attachmentId") String attachmentId);

  public List<User> getAttachmentAuthor(@Param("attachmentId") String attachmentId);
}
