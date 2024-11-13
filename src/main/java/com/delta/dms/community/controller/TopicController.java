package com.delta.dms.community.controller;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.delta.dms.community.adapter.AdapterUtil;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.swagger.controller.TopicApi;
import com.delta.dms.community.swagger.model.Emoji;
import com.delta.dms.community.swagger.model.EmojiResult;
import com.delta.dms.community.swagger.model.ForumIdWithModifiedTime;
import com.delta.dms.community.swagger.model.ParticipatedTopic;
import com.delta.dms.community.swagger.model.Pin;
import com.delta.dms.community.swagger.model.PinEnum;
import com.delta.dms.community.swagger.model.ReplyConclusionCreationData;
import com.delta.dms.community.swagger.model.ReplyConclusionUpdatedData;
import com.delta.dms.community.swagger.model.ReplyCreationData;
import com.delta.dms.community.swagger.model.ReplyData;
import com.delta.dms.community.swagger.model.ReplyListDetail;
import com.delta.dms.community.swagger.model.ReplyOperation;
import com.delta.dms.community.swagger.model.ReplySearchResult;
import com.delta.dms.community.swagger.model.ReplyUpdatedData;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.swagger.model.TopicCreationData;
import com.delta.dms.community.swagger.model.TopicHomePage;
import com.delta.dms.community.swagger.model.TopicOperation;
import com.delta.dms.community.swagger.model.TopicPqmData;
import com.delta.dms.community.swagger.model.TopicUpdatedData;
import com.delta.dms.community.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Topic",
    })
@RestController
public class TopicController implements TopicApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HomePageService homePageService;
  private TopicService topicService;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ReplyService replyService;
  private BookmarkService bookmarkService;
  private TopicReplyReportService topicReplyReportService;
  @Autowired
  private AdapterUtil adapterUtil;
  @Autowired
  private MessageSource messageSource;

  @Autowired
  public TopicController(
      HomePageService homePageService,
      TopicService topicService,
      HttpServletRequest request,
      HttpServletResponse response,
      ReplyService replyService,
      BookmarkService bookmarkService,
      TopicReplyReportService topicReplyReportService
  ) {
    this.homePageService = homePageService;
    this.topicService = topicService;
    this.request = request;
    this.response = response;
    this.replyService = replyService;
    this.bookmarkService = bookmarkService;
    this.topicReplyReportService = topicReplyReportService;
  }

  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return Optional.ofNullable(mapper);
  }

  @Override
  public Optional<HttpServletRequest> getRequest() {
    return Optional.ofNullable(request);
  }

  @Override
  public ResponseBean<TopicHomePage> getTopicInfo(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @ApiParam(
              value = "Need attachment detail or not (including thumbnail)",
              defaultValue = "true")
          @Valid
          @RequestParam(value = "withAttachmentDetail", required = false, defaultValue = "true")
          Boolean withAttachmentDetail)
      throws Exception {
    return new ResponseBean<>(homePageService.getTopicHomePage(topicId, withAttachmentDetail));
  }

  @Override
  public ResponseBean<ReplySearchResult> searchReplyListOfTopic(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort:   * -createTime - Sort by create time. Descending.   * +createTime - Sort by create time. Ascending. ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "Need attachment detail or not", defaultValue = "true")
          @Valid
          @RequestParam(value = "withAttachmentDetail", required = false, defaultValue = "true")
          Boolean withAttachmentDetail)
      throws Exception {
    return new ResponseBean<>(
        replyService.searchReplyOfTopic(
            topicId, offset, limit, SortParam.get().getDirection(), withAttachmentDetail));
  }

  @Override
  public ResponseBean<Integer> createTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody TopicCreationData json) {
    return new ResponseBean<>(topicService.createTopic(json, false));
  }

  @Override
  public ResponseBean<Integer> createIssueTrackingTopic(
      @ApiParam(value = "", required = true) @Valid @RequestBody TopicCreationData json) {
    return new ResponseBean<>(topicService.createIssueTrackingTopic(json, true));
  }

  @Override
  public ResponseBean<Void> setEmoji(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @NotNull
          @ApiParam(
              value = "Emoji:   * surprised   * laugh   * agree   * wink   * like   * cry ",
              required = true,
              allowableValues = "surprised, laugh, agree, wink, like, cry")
          @Valid
          @RequestParam(value = "emoji", required = true)
          String emoji) {
    Emoji e = Emoji.fromValue(emoji);
    if (e == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    response.setStatus(topicService.setEmojiOfTopic(topicId, e));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<EmojiResult> getTopicEmoji(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort:   * -operationTime - Sort by operation time. Descending.   * +operationTime - Sort by operation time. Ascending. ",
              required = true,
              defaultValue = "-operationTime")
          @Valid
          @RequestParam(value = "sort", required = true, defaultValue = "-operationTime")
          String sort,
      @NotNull
          @ApiParam(
              value = "",
              required = true,
              allowableValues = "surprised, laugh, agree, wink, like, cry",
              defaultValue = "like")
          @Valid
          @RequestParam(value = "emoji", required = true, defaultValue = "like")
          String emoji)
      throws Exception {
    Emoji e = Emoji.fromValue(emoji);
    Assert.notNull(e, Constants.ERR_INVALID_PARAM);
    Assert.isTrue(
        SortField.OPERATIONTIME == SortField.fromValue(SortParam.get().getProperty()),
        Constants.ERR_INVALID_PARAM);
    return new ResponseBean<>(
        topicService.getEmojiDetailOfTopic(topicId, e, offset, limit, SortParam.get()));
  }

  @Override
  public ResponseBean<Void> removeEmoji(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId) {
    response.setStatus(topicService.removeEmojiOfTopic(topicId));
    return new ResponseBean<>();
  }

  /**
    input: topicId、 topicData (update的資料)
    1.	回傳通知討論區的成員(recipient) 到 topicData
    2.	檢查 topicData 的 appField 和 attachment 的 appField, 如果 appField 為空或任何附件的 appField 為空，則拋出錯誤
    3.	透過 topicId 取得目前 topic 的資料, 存到 topicInfo
    4.	如果主題被關閉(Locked) or 鎖定(Sealed) or 時間不正確 or 更新的 title 已經有相同的主題名稱, 則拋出錯誤
    5.	如果主題類別被更改 or 附件檔案大於最大值 拋出錯誤
    6.	檢查目前使用者是否可以 update，如果不行則拋出錯誤
    7.	把 topicInfo 更新為 topicData
    8.	如果更新 table `topics` 的 [topic_title] [topic_type_id] [topic_modified_user_id] [topic_modified_time]
        以及 table `topics_text` 的 [topics_text] 成功 則繼續往下執行 否則拋出錯誤
    9.	update tag: 根據 topicId 找到目前的 tag, 檢查跟 topicData 裡面的 tag 是否相同
            -	如果不是則刪除目前的 tag, 然後新增新的 tag
    10. update appField: 根據 topicId 找到目前的 appField, 檢查跟 topicData 裡面的 appField 是否相同
            - 	如果不是則刪除目前的 appField, 然後新增新的 appField
    11. update Attachment:
        11-1. 對於在 originalAttachment 當中 且不在 newAttachmentIdList 的項目(id), 執行以下步驟:
                - 	deleteAttachmentOfTopic -> 更新 table `topic_attachment` 的 attachment_status 為 'delete'，並設置刪除者跟刪除時間
                - 	deleteTopicAttachmentAppField -> 刪除 table `topic_attachment_app_field` 對應的appField
                - 	把 id 存到 ddf_delete_queue
        11-2. 對於在 newAttachmentIdList 當中 且不在 originalAttachment 的項目(id), 執行以下步驟:
                - 	讀取 DDF id
                - 	insert table `topic_attachment`
                -	insert table `topic_attachment_app_field`
        11-3. 對於在 newAttachmentIdList 當中 而且在 originalAttachment 的項目(id), 執行以下步驟:
                - 	檢查 attachment 的 appField 是否相同
                    - 	如果不同:	從 table `topic_attachment_app_field` 刪除對應的 attachment_id，
                                    在 table `topic_attachment_app_field` insert 新的 appField
    12. 觸發 FileUploadingEvent: 對於 topicData 的每個檔案
            -	根據權限建立一個新的 ddf
            -	將檔案對應到 ddf
            -	刪除 table `attachment_keyman` 當中的 attachment_id
            -	insert table `attachment_keyman` 當中的 attachment_id
    13. 觸發 TopicChangingEvent:
            - 	更新 communities 的最後修改時間 (community_last_modified_time)
            - 	更新 forums 的最後修改時間 (forum_last_modified_time)
            - 	更新 topics 的最後修改時間 (topic_last_modified_time)
    14. 比對原本的 topic_text 以及更新後的 topic_text:
            -	如果有圖片在原本的 topic_text 但不在更新後的 topic_text
            - 	把圖片 id 存到 ddf_delete_queue
    15. 傳送通知: 根據 topicData 裡面 recipient 的名單傳送通知及 Email
    16. 刪除在 table `ddf_delete_queue` 的 message 當中 associated_Id = topicId 且 type = FILE 的資料
  */

  @Override
  public ResponseBean<Void> updateTopicInfo(
      @ApiParam(value = "", required = true) @Valid @RequestBody TopicUpdatedData body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    int status = topicService.updateTopic(topicId, body);
    response.setStatus(status);
    return new ResponseBean<>();
  }

  /** deleteTopic
    input: topicId
    1. 根據 topicId 取得 table `topics` 、 `topics_text`、 `topics_type`、 `topics_conclusion_state` 的資訊
    2. 檢查 topic_status、 topic_situation 是否為可刪除的狀態
    3. 根據 forum_id 取得 forumMemberList 並轉成 idList
    4. 檢查目前使用者是否有權限刪除 topic
    5. 取得目前時間跟user
    6. 使用 applicationEventPublisher 發布刪除事件，在 CustomEventListener 接聽事件
            -   根據 TopicId 取得 reply list id
            -   依序刪除每個 reply_id:
            -   如果有(文字+圖片): 取得 DATAHIVE 對應的圖片id (DATAHIVE_URL_WITH_UUID) 放到 ddf_delete_queue
    7. 取得 `reply_attachment` 的 attachment_id
            -   根據 attachment_id 設置 `reply_attachment` 的 attachment_status 為 'delete'，以及刪除者跟刪除時間
            -   根據 replyId 設置 `replies` 的 reply_status 為 'delete'，以及刪除者跟刪除時間
    8. 取得 `topic_attachment` 的 attachment_id
            -   把 `topic_attachment` 對應 attachmentId 的 attachment_status 設為 'delete' 以及設定刪除者跟刪除時間
    9. topic 如果是(文字 + 圖片) -> 取得 DATAHIVE 對應的圖片id (DATAHIVE_URL_WITH_UUID) 放到 ddf_delete_queue
    10. 把 table `topics` 中對應的 topicId 設為 deleted 以及設定刪除者跟刪除時間
    11. 刪除在 table `ddf_delete_queue` 的 message 當中 associated_Id = topicId 且 type = FILE 的資料
  */

  @Override
  public ResponseBean<Void> deleteTopic(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    topicService.deleteTopic(topicId, false);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Integer> createConclusion(
      @ApiParam(value = "", required = true) @Valid @RequestBody ReplyConclusionCreationData body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    response.setStatus(replyService.createConclusion(body, topicId));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<ReplyData> createReply(
      @ApiParam(value = "", required = true) @Valid @RequestBody ReplyCreationData body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    if (StringUtils.isEmpty(body.getText().trim())) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(replyService.createReply(body, topicId));
  }

  @Override
  public ResponseBean<Void> removeReplyEmoji(@PathVariable("replyId") Integer replyId)
      throws Exception {
    response.setStatus(replyService.removeEmojiOfReply(replyId));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> setReplyEmoji(
      @PathVariable("replyId") Integer replyId,
      @NotNull @Valid @RequestParam(value = "emoji", required = true) String emoji)
      throws Exception {
    Emoji e = Emoji.fromValue(emoji);
    if (e == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    response.setStatus(replyService.setEmojiOfReply(replyId, e));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<EmojiResult> getReplyEmoji(
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort:   * -operationTime - Sort by operation time. Descending.   * +operationTime - Sort by operation time. Ascending. ",
              required = true,
              defaultValue = "-operationTime")
          @Valid
          @RequestParam(value = "sort", required = true, defaultValue = "-operationTime")
          String sort,
      @NotNull
          @ApiParam(
              value = "",
              required = true,
              allowableValues = "surprised, laugh, agree, wink, like, cry",
              defaultValue = "like")
          @Valid
          @RequestParam(value = "emoji", required = true, defaultValue = "like")
          String emoji)
      throws Exception {
    Emoji e = Emoji.fromValue(emoji);
    Assert.notNull(e, Constants.ERR_INVALID_PARAM);
    Assert.isTrue(
        SortField.OPERATIONTIME == SortField.fromValue(SortParam.get().getProperty()),
        Constants.ERR_INVALID_PARAM);
    return new ResponseBean<>(
        replyService.getEmojiDetailOfReply(replyId, e, offset, limit, SortParam.get()));
  }

  @Override
  public ResponseBean<ReplyListDetail> updateConclusion(
      @ApiParam(value = "", required = true) @Valid @RequestBody ReplyConclusionUpdatedData body,
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId)
      throws Exception {
    return new ResponseBean<>(replyService.updateConclusion(replyId, body));
  }

  @Override
  public ResponseBean<ReplyListDetail> getReplyInfo(
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId,
      @ApiParam(
              value = "Need attachment detail or not (including thumbnail)",
              defaultValue = "true")
          @Valid
          @RequestParam(value = "withAttachmentDetail", required = false, defaultValue = "true")
          Boolean withAttachmentDetail)
      throws Exception {
    return new ResponseBean<>(replyService.getReplyInfo(replyId, withAttachmentDetail));
  }

  /** updateReply
    input: replyId、 replyData (update的資料)
    1. 透過 replyId 取得目前 reply 的資料, 存到 replyInfo
    2. 回傳通知討論區的成員(recipient) 到 replyData
    3. 檢查 table `reply_attachment_app_field` 的 app_field_id, 如果有空值: 拋出錯誤
    4. 如果 ReplyStatus 被關閉(Locked) or 時間不正確 or 該主題沒有回覆, 拋出錯誤
    5. 透過 replyInfo 取得對應的 topicId, 檢查 topicId 是否被鎖定(Sealed), 如果是: 拋出錯誤
    6. 透過 replyInfo 取得對應的 forumId, 並根據 forumId 取得 forumMemberList
    7. 根據 forumMemberList 檢查目前使用者是否可以 update，如果不行: 拋出錯誤
    8. 檢查附件檔案是否大於最大值, 如果是: 拋出錯誤
    9. 把 replyInfo 更新為 replyData
    10.如果更新 table `replies` 的 [reply_modified_user_id] [reply_modified_time] [reply_respondee]
    以及 table `replies_text` 的 [reply_text] [reply_conclusion_text] 成功, 則繼續往下執行 否則拋出錯誤
    11. update Attachment:
    11-1. 對於在 originalAttachment 當中 且不在 newAttachmentIdList 的項目(id), 執行以下步驟:
            - 	deleteAttachmentOfTopic -> 更新 table `reply_attachment` 的 attachment_status 為 'delete'，並設置刪除者跟刪除時間
            - 	deleteTopicAttachmentAppField -> 刪除 table `reply_attachment_app_field` 對應的appField
            - 	把 id 存到 ddf_delete_queue
    11-2. 對於在 newAttachmentIdList 當中 且不在 originalAttachment 的項目(id), 執行以下步驟:
            - 	讀取 DDF id
            - 	insert table `reply_attachment`
            -	insert table `reply_attachment_app_field`
    11-3. 對於在 newAttachmentIdList 當中 而且在 originalAttachment 的項目(id), 執行以下步驟:
            - 	檢查 attachment 的 appField 是否相同
                - 	如果不同:	從 table `reply_attachment_app_field` 刪除對應的 attachment_id
                            在 table `reply_attachment_app_field` insert 新的 appField
    12. 觸發 FileUploadingEvent: 對於 topicData 的每個檔案
        -	根據權限建立一個新的 ddf
        -	將檔案對應到 ddf
        -	刪除 table `attachment_keyman` 當中的 attachment_id
        -	insert table `attachment_keyman` 當中的 attachment_id
    13. 比對原本的 reply_text 以及更新後的 reply_text:
        -	如果有圖片在原本的 reply_text 但不在更新後的 reply_text
        - 	把圖片 id 存到 ddf_delete_queue
    14. 觸發 TopicChangingEvent:
        - 	更新 communities 的最後修改時間 (community_last_modified_time)
        - 	更新 forums 的最後修改時間 (forum_last_modified_time)
        - 	更新 topics 的最後修改時間 (topic_last_modified_time)
    15. 傳送通知: 根據 replyData 裡面 recipient 的名單傳送通知及 Email
    16. 刪除在 table `ddf_delete_queue` 的 message 當中 associated_Id = reply 對應的 topicId 且 type = FILE 的資料
    17. 回傳整個 reply 更新的內容
  */
  @Override
  public ResponseBean<ReplyListDetail> updateReply(
      @ApiParam(value = "", required = true) @Valid @RequestBody ReplyUpdatedData body,
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId)
      throws Exception {
    return new ResponseBean<>(replyService.updateReply(replyId, body));
  }

  /** deleteReply
    input: replyId
    1. 透過 replyId 取得目前 reply 的資料, 存到 replyInfo
    2. 如果 ReplyStatus 被關閉(Locked) or 該主題沒有回覆, 拋出錯誤
    3. 透過 replyInfo 取得對應的 topicId, 檢查 topicId 是否被鎖定(Sealed), 如果是: 拋出錯誤
    4. 透過 replyInfo 取得對應的 forumId, 並根據 forumId 取得 forumMemberList
    5. 根據 forumMemberList 檢查目前使用者是否可以 delete，如果不行: 拋出錯誤
    6. 取得目前時間跟使用者
    7. table `replies_text` 的 reply_text 如果包含(文字 + 圖片) -> 取得 DATAHIVE 對應的圖片id 放到 ddf_delete_queue
    8. 對於每一個 replyId:
        -	取得 reply_attachment 的 attachment_id, 放到 ddf_delete_queue
        -	update attachment_status 為 'delete'，並設置刪除者跟刪除時間
    9. 把 table `replies` 的 reply_status 為 'delete'，並設置刪除者跟刪除時間
    10.刪除在 table `ddf_delete_queue` 的 message 當中 associated_Id = reply 對應的 topicId 且 type = FILE 的資料
  */
  @Override
  public ResponseBean<Void> deleteReply(
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId)
      throws Exception {
    replyService.deleteReply(replyId, false);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Integer> createTopicOfPqm(
      @ApiParam(value = "", required = true) @Valid @RequestBody TopicPqmData body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("account") String account)
      throws Exception {
    return new ResponseBean<>(topicService.createTopicOfPqm(account, body));
  }

  @Override
  public ResponseBean<Void> setBookmarkOfTopic(
      @PathVariable("topicId") Integer topicId,
      @NotNull @Valid @RequestParam(value = "forumId", required = true) Integer forumId)
      throws Exception {
    bookmarkService.setBookmarkOfTopic(topicId, forumId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> removeBookmarkOfTopic(
      @PathVariable("topicId") Integer topicId,
      @NotNull @Valid @RequestParam(value = "forumId", required = true) Integer forumId)
      throws Exception {
    bookmarkService.removeBookmarkOfTopic(topicId, forumId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> sealTopic(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @NotNull
          @ApiParam(value = "forumId", required = true, defaultValue = "false")
          @Valid
          @RequestParam(value = "seal", required = true, defaultValue = "false")
          Boolean seal)
      throws Exception {
    response.setStatus(topicService.sealOrUnsealTopic(topicId, seal));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<ReplySearchResult> searchReplyListOfReply(
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort:   * -createTime - Sort by create time. Descending.   * +createTime - Sort by create time. Ascending. ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "Need attachment detail or not", defaultValue = "true")
          @Valid
          @RequestParam(value = "withAttachmentDetail", required = false, defaultValue = "true")
          Boolean withAttachmentDetail)
      throws Exception {
    return new ResponseBean<>(
        replyService.searchReplyOfReply(
            replyId, offset, limit, SortParam.get().getDirection(), withAttachmentDetail));
  }

  @Override
  public ResponseBean<List<ParticipatedTopic>> getParticipatedTopicsOfUser(
      @ApiParam(value = "User id", required = true) @PathVariable("userId") String userId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit)
      throws Exception {
    return new ResponseBean<>(topicService.getParticipatedTopicOfUser(userId, offset, limit));
  }

  @Override
  public ResponseBean<String> updatePinOfTopic(
      @ApiParam(value = "to pin or unpin the topic", required = true) @Valid @RequestBody Pin body,
      @ApiParam(value = "topic id", required = true) @PathVariable("topicId") Integer topicId)
      throws Exception {
    PinEnum pinEnum = body.getPin();
    if (pinEnum == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(topicService.updatePinOfTopic(topicId, body.getPin()));
  }

  @Override
  public ResponseBean<String> updateToppingOrderOfTopic(
      @ApiParam(value = "topic id", required = true) @PathVariable("topicId") Integer topicId,
      @ApiParam(value = "the topping order of the topic", required = true)
          @PathVariable("toppingOrder")
          Integer toppingOrder)
      throws Exception {
    topicService.updateToppingOrderOfTopic(topicId, toppingOrder);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> moveTopicToOtherForum(
      @ApiParam(value = "", required = true) @Valid @RequestBody ForumIdWithModifiedTime body,
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    topicService.moveTopicToOtherForum(topicId, body);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> validateTopicPrivilege(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId,
      @ApiParam(value = "operation", required = true) @PathVariable("operation")
          TopicOperation operation)
      throws Exception {
    topicService.validateTopicPrivilege(operation, topicId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> validateReplyPrivilege(
      @ApiParam(value = "Id of the reply", required = true) @PathVariable("replyId")
          Integer replyId,
      @ApiParam(value = "operation", required = true) @PathVariable("operation")
          ReplyOperation operation)
      throws Exception {
    replyService.validateReplyPrivilege(operation, replyId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<String> getForumTypeByTopicId(
      @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
          Integer topicId)
      throws Exception {
    return new ResponseBean<>(topicService.getForumTypeById(topicId));
  }

  @GetMapping("/topic/{topicId}/repliesReport")
  public ResponseEntity<InputStreamResource> generateTopicRepliesReport(
            @ApiParam(value = "Id of the topic", required = true) @PathVariable("topicId")
            Integer topicId)
            throws Exception {
        String referer = request.getHeader(HttpHeaders.REFERER);
        return topicReplyReportService.generateTopicRepliesReportEntity(topicId, referer);
    }
}
