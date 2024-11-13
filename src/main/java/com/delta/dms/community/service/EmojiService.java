package com.delta.dms.community.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.dao.EmojiDao;
import com.delta.dms.community.swagger.model.Emoji;
import com.delta.dms.community.swagger.model.EmojiDetail;
import com.delta.dms.community.swagger.model.EmojiResultUser;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;

@Service
@Transactional
public class EmojiService {

  private UserService userService;
  private EmojiDao emojiDao;

  public EmojiService(UserService userService, EmojiDao emojiDao) {
    this.userService = userService;
    this.emojiDao = emojiDao;
  }

  public List<Map<String, Object>> getEmojiOfTopic(int topicId) {
    return emojiDao.getEmojiOfTopic(topicId);
  }

  public String getEmojiOfTopicByUser(int topicId, String userId) {
    return emojiDao.getEmojiOfTopicByUser(topicId, userId);
  }

  public List<Map<String, Object>> getEmojiOfReply(int replyId) {
    return emojiDao.getEmojiOfReply(replyId);
  }

  public String getEmojiOfReplyByUser(int replyId, String userId) {
    return emojiDao.getEmojiOfReplyByUser(replyId, userId);
  }

  public int setEmojiOfTopic(int topicId, Emoji emoji) {
    return emojiDao.setEmojiOfTopic(
        Utility.getUserIdFromSession(), topicId, emoji.ordinal() + 1, new Date().getTime());
  }

  private List<EmojiResultUser> transferToEmojiResultUser(List<EmojiDetail> emojiDetailList) {
    Map<String, UserSession> internalTalentUserMap =
        userService
            .getUserById(
                emojiDetailList
                    .parallelStream()
                    .map(EmojiDetail::getUserId)
                    .collect(Collectors.toList()),
                new ArrayList<>())
            .stream()
            .collect(
                LinkedHashMap::new,
                (map, item) -> map.put(item.getCommonUUID(), item),
                Map::putAll);
    return emojiDetailList
        .parallelStream()
        .map(
            emojiDetail ->
                new EmojiResultUser()
                    .user(internalTalentUserMap.get(emojiDetail.getUserId()))
                    .operationTime(emojiDetail.getOperationTime()))
        .collect(Collectors.toList());
  }

  public List<EmojiResultUser> getEmojiDetailOfTopic(
      Integer topicId, Emoji emoji, Integer offset, Integer limit, Direction direction) {
    List<EmojiDetail> emojiDetailList =
        emojiDao.getEmojiDetailOfTopic(
            topicId, emoji.ordinal() + 1, offset, limit, direction.toString());
    return transferToEmojiResultUser(emojiDetailList);
  }

  public List<EmojiResultUser> getEmojiDetailOfReply(
      Integer replyId, Emoji emoji, Integer offset, Integer limit, Direction direction) {
    List<EmojiDetail> emojiDetailList =
        emojiDao.getEmojiDetailOfReply(
            replyId, emoji.ordinal() + 1, offset, limit, direction.toString());
    return transferToEmojiResultUser(emojiDetailList);
  }

  public int removeEmojiOfTopic(int topicId) {
    return emojiDao.removeEmojiOfTopic(Utility.getUserIdFromSession(), topicId);
  }

  public int setEmojiOfReply(int replyId, Emoji emoji) {
    return emojiDao.setEmojiOfReply(
        Utility.getUserIdFromSession(), replyId, emoji.ordinal() + 1, new Date().getTime());
  }

  public int removeEmojiOfReply(int replyId) {
    return emojiDao.removeEmojiOfReply(Utility.getUserIdFromSession(), replyId);
  }

  public Map<String, Integer> transferEmojiMap(List<Map<String, Object>> map) {
    Map<String, Integer> allEmojiMap = new HashMap<>();
    for (Emoji item : Emoji.values()) {
      allEmojiMap.put(item.toString(), 0);
    }
    Map<String, Integer> emojiMap =
        map.stream()
            .collect(
                Collectors.toMap(
                    x -> x.get(Constants.SQL_DESC).toString(),
                    x -> Integer.parseInt(x.get(Constants.SQL_COUNT).toString())));
    return Stream.concat(allEmojiMap.entrySet().stream(), emojiMap.entrySet().stream())
        .collect(
            Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
  }
}
