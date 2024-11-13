package com.delta.dms.community.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.EmojiDetail;

public interface EmojiDao {
  /**
   * Get the emoji of the topic
   *
   * @param topicId Topic id
   * @return Emoji map
   */
  public List<Map<String, Object>> getEmojiOfTopic(@Param("topicId") int topicId);

  /**
   * Get the emoji of the topic by user
   *
   * @param topicId Topic id
   * @param userId User id
   * @return Emoji desc
   */
  public String getEmojiOfTopicByUser(
      @Param("topicId") int topicId, @Param("userId") String userId);

  /**
   * Get the emoji of the reply
   *
   * @param replyId Reply id
   * @return Emoji map
   */
  public List<Map<String, Object>> getEmojiOfReply(@Param("replyId") int replyId);

  /**
   * Get the emoji of the topic by user
   *
   * @param replyId Reply id
   * @param userId User id
   * @return Emoji desc
   */
  public String getEmojiOfReplyByUser(
      @Param("replyId") int replyId, @Param("userId") String userId);

  /**
   * Set the emoji of the topic
   *
   * @param userId User id
   * @param topicId Topic id
   * @param emojiId Emoji id
   * @param operationTime operation time
   * @return Numbers of rows affected
   */
  public int setEmojiOfTopic(
      @Param("userId") String userId,
      @Param("topicId") int topicId,
      @Param("emojiId") int emojiId,
      @Param("operationTime") long operationTime);

  /**
   * Remove the emoji of the topic
   *
   * @param userId User id
   * @param topicId Topic id
   * @return Numbers of rows affected
   */
  public int removeEmojiOfTopic(@Param("userId") String userId, @Param("topicId") int topicId);

  /**
   * Set the emoji of the reply
   *
   * @param userId User id
   * @param replyId Reply id
   * @param emojiId Emoji id
   * @param operationTime operation time
   * @return Numbers of rows affected
   */
  public int setEmojiOfReply(
      @Param("userId") String userId,
      @Param("replyId") int replyId,
      @Param("emojiId") int emojiId,
      @Param("operationTime") long operationTime);

  /**
   * Remove the emoji of the reply
   *
   * @param userId User id
   * @param replyId Reply id
   * @return Numbers of rows affected
   */
  public int removeEmojiOfReply(@Param("userId") String userId, @Param("replyId") int replyId);

  public List<EmojiDetail> getEmojiDetailOfTopic(
      @Param("topicId") int topicId,
      @Param("emojiId") int emojiId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortOrder") String sortOrder);

  public List<EmojiDetail> getEmojiDetailOfReply(
      @Param("replyId") int replyId,
      @Param("emojiId") int emojiId,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("sortOrder") String sortOrder);
}
