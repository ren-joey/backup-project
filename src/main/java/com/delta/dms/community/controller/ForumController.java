package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.HomePageService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.service.search.JarvisSearchService;
import com.delta.dms.community.swagger.controller.ForumApi;
import com.delta.dms.community.swagger.model.ApplicantDetail;
import com.delta.dms.community.swagger.model.ApplicationDetail;
import com.delta.dms.community.swagger.model.CommunityResultList;
import com.delta.dms.community.swagger.model.ForumData;
import com.delta.dms.community.swagger.model.ForumHomePage;
import com.delta.dms.community.swagger.model.ForumMoveData;
import com.delta.dms.community.swagger.model.ForumStatusInput;
import com.delta.dms.community.swagger.model.MemberList;
import com.delta.dms.community.swagger.model.MemberListResult;
import com.delta.dms.community.swagger.model.NotificationDetail;
import com.delta.dms.community.swagger.model.Pin;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.ResponseData;
import com.delta.dms.community.swagger.model.ReviewAction;
import com.delta.dms.community.swagger.model.Role;
import com.delta.dms.community.swagger.model.SearchType;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.swagger.model.TopicSearchResult;
import com.delta.dms.community.swagger.model.UpdatedForumData;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Forum",
    })
@RestController
public class ForumController implements ForumApi {

  private ObjectMapper mapper = new ObjectMapper();
  private ForumService forumService;
  private HomePageService homePageService;
  private TopicService topicService;
  private JarvisSearchService jarvisSearchService;
  private HttpServletRequest request;
  private HttpServletResponse response;

  @Autowired
  public ForumController(
      ForumService forumService,
      HomePageService homePageService,
      TopicService topicService,
      JarvisSearchService jarvisSearchService,
      HttpServletRequest request,
      HttpServletResponse response) {
    this.forumService = forumService;
    this.homePageService = homePageService;
    this.topicService = topicService;
    this.jarvisSearchService = jarvisSearchService;
    this.request = request;
    this.response = response;
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
  public ResponseBean<String> addMemberApplicationOfForum(
      @ApiParam(value = "Details of the application", required = true) @Valid @RequestBody
          ApplicationDetail body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    return new ResponseBean<>(forumService.addMemberApplicationOfPrivateForum(forumId, body));
  }

  @Override
  public ResponseBean<List<ApplicantDetail>> getReviewedMemberListOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    return new ResponseBean<>(forumService.getApplicantList(forumId));
  }

  @Override
  public ResponseBean<String> reviewMemberApplicationOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
        forumService.reviewMemberApplicationOfForum(forumId, applicantId, reviewAction));
  }

  @Override
  public ResponseBean<Void> notifyMemberOfForum(
      @ApiParam(value = "Details of the notification", required = true) @Valid @RequestBody
          NotificationDetail body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    response.setStatus(forumService.sendNotification(forumId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<String> deleteMemberFromForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          String memberId) {
    return new ResponseBean<>(forumService.deleteMemberFromForum(memberId, forumId));
  }

  @Override
  public ResponseBean<ForumHomePage> getForumInfo(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    return new ResponseBean<>(homePageService.getForumHomePage(forumId));
  }

  @Override
  public ResponseBean<TopicSearchResult> searchTopicListOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
                  "Sort:   * -updateTime - Sort by Update time. Descending.   * -type - Sort by Topic's type (system, problem, general).   * +state - Sort by state. Ascending (concluded first)  ",
              required = true)
          @Valid
          @RequestParam(value = "sort", required = true)
          String sort,
      @NotNull
          @ApiParam(value = "with topping result", required = true, defaultValue = "false")
          @Valid
          @RequestParam(value = "withTopping", required = true, defaultValue = "false")
          Boolean withTopping,
      @ApiParam(value = "* unconcluded * concluded  ")
          @Valid
          @RequestParam(value = "state", required = false)
          String state)
      throws Exception {
    return new ResponseBean<>(
        topicService.searchTopicListOfForum(
            forumId, offset, limit, SortParam.get(), withTopping, state));
  }

  @Override
  public ResponseBean<Integer> createForum(@Valid @RequestBody ForumData forumData) {
    ResponseData responseData = forumService.createForum(forumData);
    response.setStatus(responseData.getStatusCode());
    if (responseData.getStatusCode().equals(HttpStatus.SC_CREATED)) {
      return new ResponseBean<>(responseData.getId());
    } else {
      return new ResponseBean<>();
    }
  }

  @Override
  public ResponseBean<Void> updateForum(
      @ApiParam(
              value =
                  "communityId is the Community id * name is the forum title * tag is the tag * type describes public or private * status describes open or delete * admins describe all administrators * members describe all members * forumModifiedTime describes current the forum moditied time",
              required = true)
          @Valid
          @RequestBody
          UpdatedForumData body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    response.setStatus(forumService.updateForum(forumId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<String> deleteForum(
      @ApiParam(value = "delete status", required = true) @Valid @RequestBody ForumStatusInput body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId) {
    return new ResponseBean<>(forumService.deleteForum(forumId, body));
  }

  @Override
  public ResponseBean<MemberListResult> getMemberListOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
      @NotNull
          @ApiParam(
              value =
                  "memberInForum:   * true - Get the members in the forum.   * false - Get the members in the community who are not in this forum. ",
              required = true,
              defaultValue = "true")
          @Valid
          @RequestParam(value = "memberInForum", required = true, defaultValue = "true")
          Boolean memberInForum,
      @ApiParam(value = "User id", defaultValue = "") @Valid @RequestParam(value = "userId", required = false, defaultValue="") String userId,
      @ApiParam(value = "query name", defaultValue = "")
          @Valid
          @RequestParam(value = "q", required = false, defaultValue = "")
          String q,
      @ApiParam(
              value = "isimgAvatar:   * true - Get avatar.   * false - Don't get avatar. ",
              defaultValue = "true")
          @Valid
          @RequestParam(value = "isImgAvatar", required = false, defaultValue = "true")
          Boolean isImgAvatar)
      throws Exception {
    SortField sortType = SortField.fromValue(SortParam.get().getProperty());
    if (sortType == null) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    MemberListResult memberListResult =
        Boolean.TRUE.equals(memberInForum)
            ? forumService.getMemberListResult(forumId, offset, limit, sortType, q, userId, isImgAvatar)
            : forumService.getUserListNotInForumOfCommunity(forumId, offset, limit, sortType, q, userId, isImgAvatar);
    return new ResponseBean<>(memberListResult);
  }

  @Override
  public ResponseBean<String> updatMemberRoleOfForum(
      @ApiParam(value = "Role Status", required = true) @Valid @RequestBody Role body,
      @ApiParam(value = "Id of the community", required = true) @PathVariable("forumId")
          Integer forumId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          String memberId)
      throws Exception {
    return new ResponseBean<>(forumService.updateMemberRoleOfForum(forumId, memberId, body));
  }

  @Override
  public ResponseBean<Void> addMemberIntoForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
      @ApiParam(value = "Id of the member", required = true) @PathVariable("memberId")
          List<String> memberId)
      throws Exception {
    forumService.addMemberIntoForum(forumId, memberId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<String> updatPinOfForum(
      @ApiParam(value = "to pin or unpin forum", required = true) @Valid @RequestBody Pin body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId)
      throws Exception {
    return new ResponseBean<>(forumService.updatePinOfForum(forumId, body));
  }

  @Override
  public ResponseBean<String> updatPriorityOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
      @ApiParam(value = "the top order of forum", required = true) @PathVariable("toppingOrder")
          Integer toppingOrder)
      throws Exception {
    return new ResponseBean<>(forumService.updatePriorityOfForum(forumId, toppingOrder));
  }

  @Override
  public ResponseBean<Void> moveForumToAnotherCommunity(
      @ApiParam(value = "", required = true) @Valid @RequestBody ForumMoveData body,
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId)
      throws Exception {
    response.setStatus(forumService.moveForum(forumId, body));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<CommunityResultList> getTopicListOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
      @ApiParam(value = "filter topping ammounts or not", defaultValue = "true")
          @Valid
          @RequestParam(value = "withTopping", required = false, defaultValue = "true")
          Boolean withTopping)
      throws Exception {
    return new ResponseBean<>(
        topicService.getTopicListOfForum(
            forumId, offset, limit, SortParam.get(), state, type, withTopping));
  }

  @Override
  public ResponseBean<CommunityResultList> getToppingTopicListOfForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
    return new ResponseBean<>(topicService.getToppingTopicListOfForum(forumId, offset, limit));
  }

  @Override
  public ResponseBean<CommunityResultList> searchInForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
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
          String type)
      throws Exception {
    return new ResponseBean<>(
        jarvisSearchService.searchTopicAndAttachmentInForum(
            forumId,
            q,
            offset,
            limit,
            Utility.defaultSearchTypeList(searchType),
            SortParam.get(),
            searchActivity,
            type,
            state,
            fileExt));
  }

  @Override
  public ResponseBean<MemberListResult> getAdminListOfForum(
          @NotNull
          @ApiParam(value = "Offset", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "offset", required = true, defaultValue = "-1") Integer offset,
          @NotNull
          @ApiParam(value = "Negative number means unlimited", required = true, defaultValue = "-1")
          @Valid
          @RequestParam(value = "limit", required = true, defaultValue = "-1") Integer limit,
          @ApiParam(value = "Sort * -role - Sort by role. Descending. * +name - Sort by name. Ascending.",
                  defaultValue = "+name")
          @Valid @RequestParam(value = "sort", required = false, defaultValue = "+name") String sort,
          @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
          @ApiParam(value = "User id", defaultValue = "") @Valid
          @RequestParam(value = "userId", required = false, defaultValue = "") String userId)
      throws Exception {
      SortField sortType = SortField.fromValue(SortParam.get().getProperty());
      if (sortType == null) {
          throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
      }
    return new ResponseBean<>(forumService.getAdminListOfForumWithFilter(forumId, userId, offset, limit, sortType));
  }

  @Override
  public ResponseBean<Void> addMembersIntoForum(
      @ApiParam(value = "Id of the forum", required = true) @PathVariable("forumId")
          Integer forumId,
      @ApiParam(value = "") @Valid @RequestBody MemberList body)
      throws Exception {
    forumService.addMemberIntoForum(
        forumId, body.getId().parallelStream().distinct().collect(Collectors.toList()));
    return new ResponseBean<>();
  }
}
