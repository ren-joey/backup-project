package com.delta.dms.community.controller;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.delta.dms.community.swagger.model.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.controller.request.CommunityMemberTableViewRequest;
import com.delta.dms.community.dao.entity.CommunitySearchRequestEntity;
import com.delta.dms.community.enums.MedalIdType;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.HomePageService;
import com.delta.dms.community.service.MedalService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.service.search.JarvisSearchService;
import com.delta.dms.community.service.view.CommunityMembersTableViewRenderService;
import com.delta.dms.community.swagger.controller.CommunityApi;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Api(
    tags = {
      "Community",
    })
@RestController
public class CommunityController implements CommunityApi {

  public static final String MSG_JOIN_COMMUNITY = "Member.add.community.success";
  public static final String MSG_JOIN_COMMUNITY_FAILED = "Member.add.community.failed";
  public static final String MSG_LEFT_COMMUNITY = "Member.remove.community.success";
  public static final String MSG_LEFT_COMMUNITY_FAILED = "Member.remove.community.failed";

  private ObjectMapper mapper = new ObjectMapper();
  private final CommunityService communityService;
  private final HomePageService homePageService;
  private final ForumService forumService;
  private final TopicService topicService;
  private final JarvisSearchService jarvisSearchService;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final CommunityMembersTableViewRenderService membersTableViewService;
  private final MedalService medalService;

  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return Optional.ofNullable(mapper);
  }

  @Override
  public Optional<HttpServletRequest> getRequest() {
    return Optional.ofNullable(request);
  }

  @Override
  public ResponseBean<CommunitySearchResult> listSearch(
      @NotNull
          @ApiParam(
              value =
                  "Category of communities * all - All kinds of community * project - Project community * department - Department community * general - General community",
              required = true)
          @Valid
          @RequestParam(value = "category", required = true)
          CommunityCategory category,
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort * -updateTime - Sort by update time. Descending. * +name - Sort by community's name. Ascending.",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(
              value =
                  "Search scope * mineAdmin - My community with admin role * mine - My community * all - All community")
          @Valid
          @RequestParam(value = "scope", required = false)
          SearchScope scope,
      @ApiParam(value = "User id", defaultValue = "")
          @Valid
          @RequestParam(value = "userId", required = false, defaultValue = "")
          String userId)
      throws Exception {
    if (Objects.isNull(scope)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        communityService.getOpenCommunityByCategoryAndScope(
            category, scope, offset, limit, SortParam.get(), userId));
  }

  @Override
  public ResponseBean<Map<String, Integer>> categorySearch(
      @ApiParam(
              value =
                  "Search scope * mineAdmin - My community with admin role * mine - My community * all - All community")
          @Valid
          @RequestParam(value = "scope", required = false)
          SearchScope scope,
      @ApiParam(value = "exclude community status")
          @Valid
          @RequestParam(value = "excludeStatus", required = false)
          List<CommunityStatus> excludeStatus)
      throws Exception {
    if (Objects.isNull(scope)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        communityService.getAllCommunityCategoryCount(
            communityService.setSearchRequestInfo(
                new CommunitySearchRequestEntity()
                    .setScope(scope)
                    .setUserId(StringUtils.EMPTY)
                    .setExcludeStatusList(defaultExcludeCommunityStatus(excludeStatus)))));
  }

  @Override
  public ResponseBean<String> addMemberIntoCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          List<String> memberId) {
    boolean addMember =
        communityService.addMemberIntoCommunityByCommunityType(memberId, communityId);
    String msg = addMember ? MSG_JOIN_COMMUNITY : MSG_JOIN_COMMUNITY_FAILED;
    return new ResponseBean<>(msg);
  }

  @Override
  public ResponseBean<String> addMemberApplicationOfCommunity(
      @ApiParam(value = "Details of the application", required = true) @Valid @RequestBody
          ApplicationDetail body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId) {
    return new ResponseBean<>(communityService.addMemberApplicationOfCommunity(communityId, body));
  }

  @Override
  public ResponseBean<String> deleteMemberFromCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          String memberId) {
    boolean deleteMember = communityService.deleteMemberFromCommunity(memberId, communityId);
    String msg = deleteMember ? MSG_LEFT_COMMUNITY : MSG_LEFT_COMMUNITY_FAILED;
    return new ResponseBean<>(msg);
  }

  @Override
  public ResponseBean<List<ApplicantDetail>> getApprovalMemberList(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId) {
    return new ResponseBean<>(communityService.getApplicantList(communityId));
  }

  @Override
  public ResponseBean<String> reviewMemberApplicationOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("applicantId")
          String applicantId,
      @NotNull
          @ApiParam(
              value = "Review action:   * approved - Approve   * rejcted - Reject ",
              required = true,
              allowableValues = "approved, auto-approved, rejected")
          @Valid
          @RequestParam(value = "action", required = true)
          String action) {
    ReviewAction reviewAction = ReviewAction.fromValue(action);
    if (reviewAction == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        communityService.reviewMemberApplicationOfCommunity(
            communityId, applicantId, reviewAction));
  }

  @Override
  public ResponseBean<MemberListResult> getMemberListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit:   Negative number means unlimited ", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort:   * -role - Sort by role. Descending.   * +name - Sort by name. Ascending. ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "default/empty = all, otherwise admin or member")
        @Valid
        @RequestParam(value = "memberType", required = false)
        MemberType memberType,
      @ApiParam(value = "query name", defaultValue = "")
          @Valid
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q,
      @ApiParam(
              value = "isimgAvatar:   * true - Get avatar.   * false - Don't get avatar. ",
              defaultValue = "true")
          @Valid
          @RequestParam(value = "isImgAvatar", required = false, defaultValue = "true")
          Boolean isImgAvatar,
      @ApiParam(value = "user id", defaultValue = "")
      @Valid
      @RequestParam(value = "userId", required = false, defaultValue="")
      String userId)
      throws Exception {
    SortField sortType = SortField.fromValue(SortParam.get().getProperty());
    if (sortType == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        communityService.getMemberListResult(communityId, offset, limit, sortType, q, userId, isImgAvatar, memberType));
  }

  @Override
  public ResponseBean<MembersTableViewResult> getMembersTableViewOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "page", required = false)
          @RequestParam(required = false, defaultValue = "1")
          Integer page,
      @ApiParam(value = "pageSize", required = false)
          @RequestParam(required = false, defaultValue = "8")
          Integer pageSize,
      @ApiParam(
              value =
                  "Sort:   * -role - Sort by role. Descending.   * +name - Sort by name. Ascending. ",
              required = true)
          @RequestParam(value = "sortBy", required = false, defaultValue = "name")
          String sort,
      @ApiParam(value = "query name", defaultValue = "")
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q)
      throws Exception {
    CommunityMemberTableViewRequest request =
        CommunityMemberTableViewRequest.builder()
            .communityId(communityId)
            .page(page)
            .pageSize(pageSize)
            .sort(sort)
            .name(q)
            .build();
    MembersTableViewResult result = membersTableViewService.findByCriteria(request);
    return new ResponseBean<>(result);
  }

  @Override
  public ResponseBean<Integer> createCommunity(
      @ApiParam(
              value =
                  "* name is the community title * desc is the description * type describes public or private * status describes open or delete * admins describe all administrators * members describe all members * category describes project, department or common * notification type describles all, custom ",
              required = true)
          @Valid
          @RequestBody
          CreatedCommunityData json) {
    ResponseData responseData = communityService.createCommunity(json);
    response.setStatus(responseData.getStatusCode());
    if (responseData.getStatusCode().equals(HttpStatus.SC_CREATED)) {
      return new ResponseBean<>(responseData.getId());
    } else {
      return new ResponseBean<>();
    }
  }

  @Override
  public ResponseBean<Integer> reviewCommunityCreation(
      @ApiParam(
              value =
                  "status for approved, auto-approved, rejected * rejectedMessage record rejected reason",
              required = true)
          @Valid
          @RequestBody
          ApprovalStatus body,
      @ApiParam(value = "it used to search data", required = true) @PathVariable("batchId")
          Integer batchId) {
    ResponseData responseData = communityService.reviewCommunityCreation(batchId, body);
    response.setStatus(responseData.getStatusCode());
    if (responseData.getStatusCode().equals(HttpStatus.SC_CREATED)) {
      return new ResponseBean<>(responseData.getId());
    } else {
      return new ResponseBean<>();
    }
  }

  @Override
  public ResponseBean<CommunityHomePage> getCommunityInfo(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    return new ResponseBean<>(homePageService.getCommunityHomePage(communityId));
  }

  @Override
  public ResponseBean<Void> updateCommunity(
      @ApiParam(
              value =
                  "name is the community title * desc is the description * type describes public or private * status describes open or delete * admins describe all administrators * members describe all members * category describes project, department or common * communityModifiedTime describes current the moditied time * notification type describles all, custom",
              required = true)
          @Valid
          @RequestBody
          UpdatedCommunityData body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId) {
    response.setStatus(communityService.updateCommunity(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Image> getCommunityBanner(@PathVariable Integer communityId) {
    return new ResponseBean<>(communityService.getCommunityBanner(communityId));
  }

  @Override
  public ResponseBean<Void> updateCommunityBanner(
      @ApiParam(value = "Updated banner Img", required = true) @Valid @RequestBody Image body,
      @ApiParam(value = "it used to search data", required = true) @PathVariable("communityId")
          Integer communityId) {
    response.setStatus(communityService.updateCommunityBanner(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Image> getCommunityAvatar(
      @ApiParam(value = "it used to search data", required = true) @PathVariable("communityId")
          Integer communityId) {
    return new ResponseBean<>(communityService.getCommunityAvatar(communityId));
  }

  @Override
  public ResponseBean<Void> updateCommunityAvatar(
      @ApiParam(value = "Updated avatar Img", required = true) @Valid @RequestBody AvatarImage body,
      @ApiParam(value = "it used to search data", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    response.setStatus(communityService.updateCommunityAvatar(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<CommunityReviewList> getAllCommunityCreation(
      @NotNull
          @ApiParam(value = "Processed or not", required = true, defaultValue = "false")
          @Valid
          @RequestParam(value = "processed", required = true, defaultValue = "false")
          Boolean processed,
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
    return new ResponseBean<>(communityService.getAllCommunityCreation(processed, offset, limit));
  }

  @Override
  public ResponseBean<Void> notifyMemberOfCommunity(
      @ApiParam(value = "Details of the notification", required = true) @Valid @RequestBody
          NotificationDetail body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId) {
    response.setStatus(communityService.sendNotification(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<ForumSearchResult> searchForumList(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @NotNull
          @ApiParam(
              value = "Forum type",
              required = true,
              allowableValues = "public, private, system")
          @Valid
          @RequestParam(value = "forumType", required = true)
          List<String> forumType,
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
                  "Sort:   * -updateTime - Sort by Update time. Descending.   * -type - Sort by Forum's type. Descending.   * +privilege - Sort by privilege.  ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @NotNull
          @ApiParam(value = "with topping result", required = true, defaultValue = "false")
          @Valid
          @RequestParam(value = "withTopping", required = true, defaultValue = "false")
          Boolean withTopping)
      throws Exception {
    List<ForumType> forumTypeList =
        forumType.stream().map(ForumType::fromValue).collect(Collectors.toList());
    if (forumTypeList.contains(null)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        forumService.searchForumListOfCommunity(
            communityId, forumTypeList, offset, limit, SortParam.get(), withTopping));
  }

  @Override
  public ResponseBean<TopicSearchResult> searchTopicList(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
                  "Sort:   * -updateTime - Sort by Update time. Descending.   * +name - Sort by Topic's name. Ascending.   * +state - Sort by Topic's state.  ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "* unconcluded * concluded  ")
          @Valid
          @RequestParam(value = "state", required = false)
          String state)
      throws Exception {
    return new ResponseBean<>(
        topicService.searchTopicListOfCommunity(
            communityId, offset, limit, SortParam.get(), state));
  }

  @Override
  public ResponseBean<String> updatMemberRoleOfCommunity(
      @ApiParam(value = "Role Status", required = true) @Valid @RequestBody Role body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          String memberId) {
    return new ResponseBean<>(
        communityService.updatMemberRoleOfCommunity(communityId, memberId, body));
  }

  /*
  依附型社群規則：
  orgId -> 取得數個 userGroupGid
  __Admin = 為 社群Admin [groupList]不使用
  __Manager = 為 社群Admin [groupList]不使用
  AllMember = 為 社群Member, [groupList]為空
  FamilyMember = 為 社群Member, [groupList]為空 (FM=子部門）
  自定 = 為 社群Member [groupList]納入Member
  外部部門 = 為 社群Member [groupList]納入Member
  KmAdmin = 為 社群Member, [groupList]為空
   */
  @Override
  public ResponseBean<Integer> createAttachedCommunity(
      @Valid @RequestBody AttachedCommunityData json) throws Exception {
    ResponseData responseData = communityService.createAttachedCommunity(json);
    response.setStatus(responseData.getStatusCode());
    if (responseData.getStatusCode().equals(HttpStatus.SC_CREATED)) {
      return new ResponseBean<>(responseData.getId());
    } else {
      return new ResponseBean<>();
    }
  }

  @Override
  public ResponseBean<Void> updateAttachedCommunity(
      @ApiParam(value = "updateUserId describes update of userId", required = true)
          @Valid
          @RequestBody
          UpdateAttachedCommunityData body,
      @ApiParam(value = "groupId from myDms", required = true) @PathVariable("groupId")
          String groupId)
      throws Exception {
    response.setStatus(communityService.updateAttachedCommunity(groupId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<List<LatestTopic>> getLatestTopicsOfAllCommunity(
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
    return new ResponseBean<>(topicService.getLatestTopicOfAllCommunity(offset, limit));
  }

  @Override
  public ResponseBean<AttachmentSearchResult> getAttachmentOfCommunity(
      @ApiParam(value = "Community id", required = true) @PathVariable("communityId")
          Integer communityId,
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
                  "Sort:   * -updateTime - Sort by Update time. Descending.   * +name - Sort by Attacment's name. Ascending.   * -type - Sort by file extension. Descending. ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "file extension list")
          @Valid
          @RequestParam(value = "fileExt", required = false)
          List<String> fileExt)
      throws Exception {
    fileExt = fileExt == null ? new ArrayList<>() : fileExt;
    return new ResponseBean<>(
        topicService.getOwnAttachmentOfCommunity(
            communityId,
            offset,
            limit,
            SortParam.get(),
            fileExt.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER))));
  }

  @Override
  public ResponseBean<Void> applyDeletingCommunity(
      @ApiParam(value = "subject and description", required = true) @Valid @RequestBody
          ApplicationDetail body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    if (body.getSubject().isEmpty()) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    response.setStatus(communityService.deleteCommunityApplication(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> reviewDeletingApplicationOfCommunity(
      @ApiParam(value = "status and rejectedMessage", required = true) @Valid @RequestBody
          ApprovalStatus body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    response.setStatus(communityService.reviewDeletingApplicationOfCommunity(communityId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<DeleteApplicationResult> getApplicationOfDeletingCommunity(
      @NotNull
          @ApiParam(value = "Processed or not", required = true, defaultValue = "false")
          @Valid
          @RequestParam(value = "processed", required = true, defaultValue = "false")
          Boolean processed,
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
    return new ResponseBean<>(
        communityService.getApplicationOfDeletingCommunity(processed, offset, limit));
  }

  @Override
  public ResponseBean<Void> lockAttachedCommunity(
      @ApiParam(value = "groupId from myDms", required = true) @PathVariable("groupId")
          String groupId,
      @NotNull
          @ApiParam(value = "User id", required = true)
          @Valid
          @RequestParam(value = "userId", required = true)
          String userId)
      throws Exception {
    if (userId.isEmpty()) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    response.setStatus(communityService.lockCommunity(groupId, userId));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> createCommunityAnnouncement(
      @ApiParam(value = "Announcement Text", required = true) @Valid @RequestBody Announcement body,
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    response.setStatus(communityService.createCommunityAnnouncement(communityId, body.getText()));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> deleteCommunityAnnouncement(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    response.setStatus(communityService.deleteCommunityAnnouncement(communityId));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<String> getCommunityAnnouncement(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    return new ResponseBean<>(communityService.getCommunityAnnouncement(communityId));
  }

  @Override
  public ResponseBean<HotForumSearchResult> searchHotForumList(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
    return new ResponseBean<>(forumService.searchHotForumList(communityId, offset, limit));
  }

  @Override
  public ResponseBean<TopicSearchResult> searchHotTopicList(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
    return new ResponseBean<>(topicService.searchHotTopicList(communityId, offset, limit));
  }

  @Override
  public ResponseBean<List<IdNameDto>> getPrivilegedForumOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    return new ResponseBean<>(forumService.getPrivilegedForumOfCommunity(communityId));
  }

  @Override
  public ResponseBean<ActiveMemberListResult> getActiveMemberListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @NotNull
          @ApiParam(value = "Limit:   Negative number means unlimited ", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit)
      throws Exception {
    return new ResponseBean<>(communityService.getActiveMemberListOfCommunity(communityId, limit));
  }

  @Override
  public ResponseBean<Void> closeGeneralCommunity(
      @ApiParam(value = "communityModifiedTime is the community modifiedTime", required = true)
          @Valid
          @RequestBody
          ClosedCommunityData body,
      @ApiParam(value = "communityId", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    response.setStatus(
        communityService.closeGeneralCommunity(communityId, body.getCommunityModifiedTime()));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> reopenGeneralCommunity(
      @ApiParam(value = "communityModifiedTime is the community modifiedTime", required = true)
          @Valid
          @RequestBody
          ReopenedCommunityData body,
      @ApiParam(value = "communityId", required = true) @PathVariable("communityId")
          Integer communityId)
      throws Exception {
    response.setStatus(
        communityService.reopenGeneralCommunity(communityId, body.getCommunityModifiedTime()));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<CommunityResultList> communityList(
      @NotNull
          @ApiParam(
              value =
                  "Category of communities * all - All kinds of community * project - Project community * department - Department community * general - General community",
              required = true)
          @Valid
          @RequestParam(value = "category", required = true)
          CommunityCategory category,
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort * -updateTime - Sort by update time. Descending. * +name - Sort by community's name. Ascending.",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(
              value =
                  "Search scope * mineAdmin - My community with admin role * mine - My community * all - All community")
          @Valid
          @RequestParam(value = "scope", required = false)
          SearchScope scope,
      @ApiParam(value = "User id", defaultValue = "")
          @Valid
          @RequestParam(value = "userId", required = false, defaultValue = "")
          String userId,
      @ApiParam(value = "exclude community status")
          @Valid
          @RequestParam(value = "excludeStatus", required = false)
          List<CommunityStatus> excludeStatus)
      throws Exception {
    if (Objects.isNull(scope)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        communityService.getCommunityList(
            new CommunitySearchRequestEntity()
                .setCategory(ofNullable(category).orElse(CommunityCategory.ALL).toString())
                .setScope(scope)
                .setOffset(offset)
                .setLimit(limit)
                .setSort(SortParam.get())
                .setUserId(userId)
                .setExcludeStatusList(defaultExcludeCommunityStatus(excludeStatus))));
  }

  @Override
  public ResponseBean<CommunityResultList> searchCommunityList(
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit,
      @NotNull
          @ApiParam(
              value = "Sort * -updateTime - Sort by Update time. * -relevance - Sort by relevance.",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "Query term", defaultValue = "")
          @Valid
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q,
      @Size(min = 1, max = 4)
          @ApiParam(value = "Search type list")
          @Valid
          @RequestParam(value = "searchType", required = false)
          List<SearchType> searchType,
      @ApiParam(
              value =
                  "Search activity (search/ filterQuerySearch/ mainQuerySearch/ pageQuerySearch/ sortQuerySearch. default value is search)",
              allowableValues =
                  "search, filterQuerySearch, mainQuerySearch, pageQuerySearch, sortQuerySearch",
              defaultValue = "search")
          @Valid
          @RequestParam(value = "searchActivity", required = false, defaultValue = "search")
          String searchActivity,
      @ApiParam(value = "exclude community status")
          @Valid
          @RequestParam(value = "excludeStatus", required = false)
          List<CommunityStatus> excludeStatus)
      throws Exception {
    return new ResponseBean<>(
        jarvisSearchService.searchCommunityList(
            q,
            offset,
            limit,
            Utility.defaultSearchTypeList(searchType),
            SortParam.get(),
            searchActivity,
            defaultExcludeCommunityStatus(excludeStatus)));
  }

  @Override
  public ResponseBean<Map<String, Integer>> searchCommunityCategory(
      @ApiParam(value = "Query term", defaultValue = "")
          @Valid
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q,
      @Size(min = 1, max = 4)
          @ApiParam(value = "Search type list")
          @Valid
          @RequestParam(value = "searchType", required = false)
          List<SearchType> searchType,
      @ApiParam(value = "exclude community status")
          @Valid
          @RequestParam(value = "excludeStatus", required = false)
          List<CommunityStatus> excludeStatus)
      throws Exception {
    return new ResponseBean<>(
        jarvisSearchService.searchCommunityCategory(
            q,
            Utility.defaultSearchTypeList(searchType),
            defaultExcludeCommunityStatus(excludeStatus)));
  }

  @Override
  public ResponseBean<CommunityResultList> getForumListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
                  "Sort * -updateTime - Sort by Update time. Descending. * -type - Sort by Forum's type. Descending. * +privilege - Sort by privilege.",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "Forum type") @Valid @RequestParam(value = "forumType", required = false)
          List<String> forumType)
      throws Exception {
    List<ForumType> forumTypeList =
        Optional.ofNullable(forumType)
            .filter(list -> !list.isEmpty())
            .map(list -> list.stream().map(ForumType::fromValue).collect(Collectors.toList()))
            .orElseGet(() -> Arrays.stream(ForumType.values()).collect(Collectors.toList()));
    if (forumTypeList.contains(null)) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    return new ResponseBean<>(
        forumService.getForumListOfCommunity(
            communityId, forumTypeList, offset, limit, SortParam.get()));
  }

  @Override
  public ResponseBean<CommunityResultList> getHotForumListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
    return new ResponseBean<>(forumService.getHotForumListOfCommunity(communityId, offset, limit));
  }

  @Override
  public ResponseBean<CommunityResultList> getToppingForumListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
    return new ResponseBean<>(
        forumService.getToppingForumListOfCommunity(communityId, offset, limit));
  }

  @Override
  public ResponseBean<CommunityResultList> searchInCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1")
          Integer offset,
      @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1")
          Integer limit,
      @NotNull
          @ApiParam(
              value =
                  "Sort * -updateTime - Sort by update time. Descending. * -relevance - Sort by relevance. Descending. * -type - Sort by topic type. Descending.",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "Query term", defaultValue = "")
          @Valid
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q,
      @ApiParam(value = "unconcluded * concluded")
          @Valid
          @RequestParam(value = "state", required = false)
          String state,
      @ApiParam(value = "file extension list")
          @Valid
          @RequestParam(value = "fileExt", required = false)
          List<String> fileExt,
      @Size(min = 1, max = 4)
          @ApiParam(value = "Search type list")
          @Valid
          @RequestParam(value = "searchType", required = false)
          List<SearchType> searchType,
      @ApiParam(
              value =
                  "Search activity (search/ filterQuerySearch/ mainQuerySearch/ pageQuerySearch/ sortQuerySearch. default value is search)",
              allowableValues =
                  "search, filterQuerySearch, mainQuerySearch, pageQuerySearch, sortQuerySearch",
              defaultValue = "search")
          @Valid
          @RequestParam(value = "searchActivity", required = false, defaultValue = "search")
          String searchActivity,
      @ApiParam(value = "refer to TopicType") @Valid @RequestParam(value = "type", required = false)
          String type,
      @ApiParam(value = "Forum Id") @Valid @RequestParam(value = "forumId", required = false)
          Integer forumId)
      throws Exception {
    return new ResponseBean<>(
        jarvisSearchService.searchTopicAndAttachmentInCommunity(
            communityId,
            q,
            offset,
            limit,
            Utility.defaultSearchTypeList(searchType),
            SortParam.get(),
            searchActivity,
            forumId,
            type,
            state,
            fileExt));
  }

  @Override
  public ResponseBean<CommunityResultList> getTopicListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
                  "Sort * -updateTime - Sort by Update time. Descending. * -type - Sort by Topic's type. Descending (system, problem, general). * -state - Sort by Topic's state. Descending (unconcluded first). * +state - Sort by Topic's state. Ascending (concluded first).",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @ApiParam(value = "State * unconcluded * concluded * briefConcluded")
          @Valid
          @RequestParam(value = "state", required = false)
          String state,
      @ApiParam(value = "refer to TopicType") @Valid @RequestParam(value = "type", required = false)
          String type,
      @ApiParam(value = "Forum Id") @Valid @RequestParam(value = "forumId", required = false)
          Integer forumId)
      throws Exception {
    return new ResponseBean<>(
        topicService.getTopicListOfCommunity(
            communityId, offset, limit, SortParam.get(), state, type, forumId));
  }

  @Override
  public ResponseBean<CommunityResultList> getHotTopicListOfCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
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
    return new ResponseBean<>(topicService.getHotTopicListOfCommunity(communityId, offset, limit));
  }

  @Override
  public ResponseBean<CommunityResultList> getAttachmentListOfCommunity(
      @ApiParam(value = "Community id", required = true) @PathVariable("communityId")
          Integer communityId,
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
      @ApiParam(value = "file extension list")
          @Valid
          @RequestParam(value = "fileExt", required = false)
          List<String> fileExt,
      @ApiParam(
              value =
                  "Sort * -updateTime - Sort by Update time. Descending. * +name - Sort by Attacment's name. Ascending. * -type - Sort by file extension. Descending.",
              defaultValue = "-updateTime")
          @Valid
          @RequestParam(value = "sort", required = false, defaultValue = "-updateTime")
          String sort)
      throws Exception {
    return new ResponseBean<>(
        topicService.getAttachmentListOfCommunity(
            communityId, offset, limit, fileExt, SortParam.get()));
  }

  @Override
  public ResponseBean<List<IdNameDto>> getPrivilegedAllCommunity() throws Exception {
    return new ResponseBean<>(communityService.getPrivilegedAllCommunity());
  }

  @Override
  public ResponseBean<String> addMembersIntoCommunity(
      @ApiParam(value = "Id of the community", required = true) @PathVariable("communityId")
          Integer communityId,
      @ApiParam(value = "") @Valid @RequestBody MemberList body)
      throws Exception {
    boolean addMember =
        communityService.addMemberIntoCommunityByCommunityType(
            body.getId().parallelStream().distinct().collect(Collectors.toList()), communityId);
    String msg = addMember ? MSG_JOIN_COMMUNITY : MSG_JOIN_COMMUNITY_FAILED;
    return new ResponseBean<>(msg);
  }

  private List<String> defaultExcludeCommunityStatus(
      List<CommunityStatus> excludeStatusList) {
    List<CommunityStatus> result = new ArrayList<>();
    result.add(CommunityStatus.DELETE);
    Optional.ofNullable(excludeStatusList)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(result::addAll);
    return result.stream().map(CommunityStatus::toString).collect(toList());
  }

  @Override
  public ResponseBean<List<MedalDto>> getCommunityMedals(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    if (!communityService.checkUserCanUpdate(Utility.getCurrentUserIdWithGroupId(), communityId)) {
      throw new UnauthorizedException(I18nConstants.MSG_NOT_COMMUNITY_MANAGER);
    }
    return new ResponseBean<>(
        medalService
            .getMedals(MedalIdType.COMMUNITY, String.valueOf(communityId))
            .stream()
            .map(
                medal ->
                    new MedalDto()
                        .disabled(medal.isDisabled())
                        .expireTime(medal.getExpireTime())
                        .id(medal.getId())
                        .medal(medal.getFrame())
                        .name(medal.getName())
                        .title(medal.getTitle()))
            .collect(Collectors.toList()));
  }

  @Override
  public ResponseBean<List<AwardDto>> getCommunityAwards(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    return new ResponseBean<>(medalService.getCommunityAwards(communityId));
  }

  @Override
  public ResponseBean<CommunityMedalDto> getCommunityAvatarMedal(
      @ApiParam(value = "", required = true) @PathVariable("communityId") Integer communityId)
      throws Exception {
    return new ResponseBean<>(communityService.getCommunityAvatarMedal(communityId));
  }
}
