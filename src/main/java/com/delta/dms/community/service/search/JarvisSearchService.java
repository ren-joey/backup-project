package com.delta.dms.community.service.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.JarvisAdapter;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.model.search.SearchRequest;
import com.delta.dms.community.service.AuthService;
import com.delta.dms.community.swagger.model.CommunityResultList;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.FileIcon;
import com.delta.dms.community.swagger.model.SearchType;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.swagger.model.TopicState;
import com.delta.dms.community.swagger.model.TopicType;

@Service
public class JarvisSearchService {

  private static final String COMMUNITY_FILTER_SEARCH_TYPE = "communityCategory";
  private static final String COMMUNITY_FILTER_COMMUNITY_ID = "communityId";
  private static final String COMMUNITY_FILTER_COMMUNITY_STATUS = "communityStatus";
  private static final String COMMUNITY_FILTER_FORUM_ID = "forumId";
  private static final String COMMUNITY_FILTER_TOPIC_STATE = "topicState";
  private static final String COMMUNITY_FILTER_TOPIC_TYPE = "topicType";
  private static final String COMMUNITY_FILTER_ATTACHMENT_ICON = "fileIcon";
  private static final String COMMUNITY_SORT_TOPIC_TYPE = "topicType";
  private static final String COMMUNITY_SORT_FORUM_TYPE = "forumType";
  private static final String COMMUNITY_SORT_FILE_EXT = "fileExt";

  private JarvisAdapter jarvisAdapter;
  private AuthService authService;

  @Autowired
  public JarvisSearchService(JarvisAdapter jarvisAdapter,
		  					 AuthService authService) {
    this.jarvisAdapter = jarvisAdapter;
    this.authService = authService;
  }

  public CommunityResultList searchCommunityList(
      String q,
      int offset,
      int limit,
      List<SearchType> searchType,
      Order order,
      String searchActivity,
      List<String> excludeStatusList) {
    SearchRequest searchRequest =
        buildCommunitySearchRequest(
            q,
            offset,
            limit,
            searchType,
            transferSortProperty(order, searchType),
            searchActivity,
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            Collections.emptyList(),
            excludeStatusList);
    return jarvisAdapter.searchCommunity(searchRequest);
  }

  public Map<String, Integer> searchCommunityCategory(
      String q, List<SearchType> searchType, List<String> excludeStatusList) {
    SearchRequest searchRequest = buildCategorySearchRequest(q, searchType, excludeStatusList);
    return jarvisAdapter.searchCommunityCategory(searchRequest);
  }

  private SearchRequest buildCategorySearchRequest(
      String q, List<SearchType> searchType, List<String> excludeStatusList) {
    Map<String, List<String>> filterMap = new HashMap<>();
    filterMap.put(
        COMMUNITY_FILTER_SEARCH_TYPE,
        Optional.ofNullable(searchType)
            .orElseGet(ArrayList::new)
            .parallelStream()
            .filter(Objects::nonNull)
            .distinct()
            .map(SearchType::toString)
            .collect(Collectors.toList()));
    filterMap.put(
        COMMUNITY_FILTER_COMMUNITY_STATUS,
        Arrays.stream(CommunityStatus.values())
            .map(CommunityStatus::toString)
            .filter(item -> !excludeStatusList.contains(item))
            .collect(Collectors.toList()));
    return new SearchRequest().query(q).filterMap(filterMap);
  }

  public CommunityResultList searchTopicAndAttachmentInCommunity(
      int communityId,
      String q,
      int offset,
      int limit,
      List<SearchType> searchType,
      Order order,
      String searchActivity,
      Integer forumId,
      String type,
      String state,
      List<String> fileExt) {
    if (!Optional.ofNullable(forumId)
        .orElseGet(() -> NumberUtils.INTEGER_ZERO)
        .equals(NumberUtils.INTEGER_ZERO)) {
      return searchTopicAndAttachmentInForum(
          forumId, q, offset, limit, searchType, order, searchActivity, type, state, fileExt);
    }
    List<SearchType> searchTypeFilterList =
        searchType
            .stream()
            .filter(item -> !(SearchType.COMMUNITY == item || SearchType.FORUM == item))
            .distinct()
            .collect(Collectors.toList());
    SearchRequest searchRequest =
        buildCommunitySearchRequest(
            q,
            offset,
            limit,
            searchTypeFilterList,
            transferSortProperty(order, searchTypeFilterList),
            searchActivity,
            type,
            state,
            fileExt,
            Collections.singletonList(CommunityStatus.DELETE.toString()));
    searchRequest
        .getFilterMap()
        .put(COMMUNITY_FILTER_COMMUNITY_ID, Collections.singletonList(String.valueOf(communityId)));
    return jarvisAdapter.searchCommunity(searchRequest);
  }

  private Order transferSortProperty(Order order, List<SearchType> searchTypeFilterList) {
    if (SortField.TYPE.toString().equals(order.getProperty()) && searchTypeFilterList.size() == 1) {
      return order.withProperty(
          getTypeSortPropertyBySearchType(
              searchTypeFilterList.stream().findFirst().orElseGet(() -> SearchType.COMMUNITY),
              order.getProperty()));
    } else if (SortField.STATE.toString().equals(order.getProperty())) {
      return order.withProperty(COMMUNITY_FILTER_TOPIC_STATE);
    }
    return order;
  }

  private String getTypeSortPropertyBySearchType(SearchType searchType, String originalProperty) {
    switch (searchType) {
      case FORUM:
        return COMMUNITY_SORT_FORUM_TYPE;
      case TOPIC:
        return COMMUNITY_SORT_TOPIC_TYPE;
      case ATTACHMENT:
        return COMMUNITY_SORT_FILE_EXT;
      default:
        return originalProperty;
    }
  }

  private SearchRequest buildCommunitySearchRequest(
      String q,
      int offset,
      int limit,
      List<SearchType> searchType,
      Order order,
      String searchActivity,
      String type,
      String state,
      List<String> fileExt,
      List<String> excludeStatusList) {
    final int page = ((int) Math.floor((float) offset / limit)) + 1;
    final int pitem = limit;
    Map<String, List<String>> filterMap = new HashMap<>();
    List<String> fileIconList =
        Optional.ofNullable(fileExt)
            .map(
                iconList ->
                    iconList
                        .stream()
                        .filter(
                            icon ->
                                Arrays.stream(FileIcon.values())
                                    .map(FileIcon::toString)
                                    .anyMatch(ext -> ext.equalsIgnoreCase(icon)))
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    Optional.of(fileIconList)
        .filter(list -> !list.isEmpty())
        .ifPresent(
            list -> {
              if (list.size() != FileIcon.values().length
                  && fileIconList.contains(FileIcon.OTHER.toString())) {
                filterMap.put(
                    COMMUNITY_FILTER_ATTACHMENT_ICON,
                    Arrays.stream(FileIcon.values())
                        .map(FileIcon::toString)
                        .filter(icon -> !fileIconList.contains(icon))
                        .map(icon -> new StringBuilder(SortParam.SORT_DESC).append(icon).toString())
                        .collect(Collectors.toList()));
              } else {
                filterMap.put(COMMUNITY_FILTER_ATTACHMENT_ICON, fileIconList);
              }
            });
    Optional.ofNullable(type)
        .map(TopicType::fromValue)
        .ifPresent(
            topicType ->
                filterMap.put(COMMUNITY_FILTER_TOPIC_TYPE, Arrays.asList(topicType.toString())));
    Optional.ofNullable(state)
        .map(TopicState::fromValue)
        .ifPresent(
            topicState ->
                filterMap.put(
                    COMMUNITY_FILTER_TOPIC_STATE,
                    Collections.singletonList(topicState.toString())));
    Optional.ofNullable(searchType)
        .filter(list -> !list.isEmpty())
        .ifPresent(
            list ->
                filterMap.put(
                    COMMUNITY_FILTER_SEARCH_TYPE,
                    list.parallelStream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(SearchType::toString)
                        .collect(Collectors.toList())));
    filterMap.put(
        COMMUNITY_FILTER_COMMUNITY_STATUS,
        Arrays.stream(CommunityStatus.values())
            .map(CommunityStatus::toString)
            .filter(item -> !excludeStatusList.contains(item))
            .collect(Collectors.toList()));
    return new SearchRequest()
        .query(q)
        .page(page)
        .pitem(pitem)
        .filterMap(filterMap)
        .sort(order)
        .searchActivity(searchActivity);
  }

  public CommunityResultList searchTopicAndAttachmentInForum(
      int forumId,
      String q,
      int offset,
      int limit,
      List<SearchType> searchType,
      Order order,
      String searchActivity,
      String type,
      String state,
      List<String> fileExt) {
    List<SearchType> searchTypeFilterList =
        searchType
            .stream()
            .filter(item -> !(SearchType.COMMUNITY == item || SearchType.FORUM == item))
            .distinct()
            .collect(Collectors.toList());
    SearchRequest searchRequest =
        buildCommunitySearchRequest(
            q,
            offset,
            limit,
            searchTypeFilterList,
            transferSortProperty(order, searchTypeFilterList),
            searchActivity,
            type,
            state,
            fileExt,
            Collections.singletonList(CommunityStatus.DELETE.toString()));
    searchRequest
        .getFilterMap()
        .put(COMMUNITY_FILTER_FORUM_ID, Collections.singletonList(String.valueOf(forumId)));
    return jarvisAdapter.searchCommunity(searchRequest);
  }
}
