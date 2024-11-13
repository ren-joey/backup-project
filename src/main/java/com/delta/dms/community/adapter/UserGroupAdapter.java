package com.delta.dms.community.adapter;

import static java.util.Objects.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.exception.CreationException;
import com.delta.dms.community.utils.I18nConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import com.delta.dms.community.adapter.entity.JwtToken;
import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.adapter.entity.OrgProfile;
import com.delta.dms.community.adapter.entity.QueryUsers;
import com.delta.dms.community.adapter.entity.SourceGroup;
import com.delta.dms.community.adapter.entity.TargetSourceGroup;
import com.delta.dms.community.adapter.entity.UserGroup;
import com.delta.dms.community.config.GroupConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import com.delta.dms.community.swagger.model.CommunityCategory;
import com.delta.dms.community.swagger.model.DeltaPointInfo;
import com.delta.dms.community.swagger.model.GroupData;
import com.delta.dms.community.swagger.model.GroupMember;
import com.delta.dms.community.swagger.model.GroupUser;
import com.delta.dms.community.swagger.model.GroupUserField;
import com.delta.dms.community.swagger.model.MyDmsGroupData;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.GroupConstants;
import com.delta.dms.community.utils.Utility;
import com.delta.dms.community.enums.Role;
import com.delta.set.utils.LogUtil;
import com.delta.set.utils.RequestId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserGroupAdapter {

  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String USERGROUP_PREFIX_U = "U";
  private static final String USERGROUP_PREFIX_M = "M";
  private static final String HEADER_APP_KEY = "AppKey";
  private static final String HEADER_OLD_JWT = "OldJwt";
  private static final String GROUP_FIELD_ENAME = "ename";
  private static final String GROUPSERVICE_HTTP_STATUS = "GroupService HTTP Status:";

  private final AdapterUtil adapterUtil;
  private final GroupConfig groupConfig;
  private final YamlConfig yamlConfig;

  public JwtToken getUserTokenByToken(String token) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON_UTF8);
    httpHeaders.set(HEADER_APP_KEY, yamlConfig.getAppId());
    httpHeaders.set(HEADER_OLD_JWT, token);
    ResponseEntity<JwtToken> response =
        adapterUtil.sendRequest(
            groupConfig.getNewtokenUrl(), HttpMethod.GET, httpHeaders, null, null, JwtToken.class);
    return adapterUtil.getResponseBody(response);
  }

  public JwtToken getUserTokenBySamAccountAndPassword(AuthenticationToken authenticationToken, String appKey) {
    ObjectNode param = new ObjectMapper().createObjectNode();
    param.put(groupConfig.getTokenUsername(), authenticationToken.getUsername());
    param.put(groupConfig.getTokenPassword(), authenticationToken.getPassword());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON_UTF8);
    //假如appkey不為empty string，把appkey加到header
    if (appKey.length()>0)
    	headers.set(HEADER_APP_KEY, appKey);
    ResponseEntity<JwtToken> response =
        adapterUtil.sendRequest(
            groupConfig.getTokenUrl(),
            HttpMethod.POST,
            headers,
            param.toString(),
            null,
            JwtToken.class);
    if (Objects.isNull(response)) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (HttpStatus.OK != response.getStatusCode()) {
      throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    }
    return response.getBody();
  }

  public List<GroupUser> getUserByUids(List<String> queryValue, List<GroupUserField> fieldList) {
    if (CollectionUtils.isEmpty(queryValue)) {
      return Collections.emptyList();
    }
    Map<String, List<String>> queryMap = getUserIdQueryMap(queryValue);
    QueryUsers queryUsers =
        new QueryUsers()
            .setUids(queryMap.get(groupConfig.getBasicsUids()))
            .setMids(queryMap.get(groupConfig.getBasicsMids()));
    return getUser(queryUsers, fieldList);
  }

  public List<GroupUser> getUserBySamAccounts(
      List<String> samAccountList, List<GroupUserField> fieldList) {
    if (CollectionUtils.isEmpty(samAccountList)) {
      return Collections.emptyList();
    }
    QueryUsers queryUsers = new QueryUsers().setSamaccounts(samAccountList);
    return getUser(queryUsers, fieldList);
  }

  public List<GroupUser> getUserBasicInfoByUids(List<String> queryValue) {
    return getUserByUids(queryValue, Collections.singletonList(GroupUserField.BASICINFO));
  }

  public List<GroupUser> getUserBasicInfo(QueryUsers queryUsers) {
    return getUser(queryUsers, Collections.singletonList(GroupUserField.BASICINFO));
  }

  public List<GroupUser> getUser(QueryUsers queryUsers, List<GroupUserField> fieldList) {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    Optional.ofNullable(fieldList)
        .ifPresent(
            list ->
                list.forEach(
                    item -> uriVariables.add(groupConfig.getBasicsField(), item.toString())));
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getBasicsUrl(),
            HttpMethod.POST,
            null,
            queryUsers,
            uriVariables,
            JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    return Optional.ofNullable(responseBody)
        .map(body -> body.get(Constants.RESPONSE_DATA))
        .map(body -> body.get(Constants.RESPONSE_RESULTS))
        .map(
            results ->
                StreamSupport.stream(results.spliterator(), false)
                    .map(
                        item -> {
                          try {
                            return mapper.treeToValue(item, GroupUser.class);
                          } catch (JsonProcessingException e) {
                            log.error(e);
                            return new GroupUser();
                          }
                        })
                    .filter(user -> StringUtils.isNotEmpty(user.getUid()))
                    .collect(Collectors.toList()))
        .orElseGet(Collections::emptyList);
  }

  public Map<String, List<GroupData>> getOrgGroupUserGroupByGid(List<String> groupIdList) {
    groupIdList =
        Optional.ofNullable(groupIdList)
            .orElseGet(Collections::emptyList)
            .parallelStream()
            .filter(StringUtils::isNotEmpty)
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(groupIdList)) {
      return new HashMap<>();
    }
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(groupConfig.getFilter(), GroupConstants.NOT_FAMILY_MEMBER_FILTER_REGEX);
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getOrgGroupMembersUrl(),
            HttpMethod.POST,
            getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId()),
            buildGidsBody(groupIdList).toString(),
            uriVariables,
            JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (Objects.isNull(responseBody)) {
      return new HashMap<>();
    }
    return StreamSupport.stream(responseBody.spliterator(), false)
        .collect(
            Collectors.toMap(
                item -> Utility.getStringFromJsonNode(item.path(groupConfig.getId())),
                item ->
                    getUserGroupListFromChildren(item.path(GroupConstants.USERGROUP_CHILDREN))));
  }

  /**
   * get org group by id
   *
   * @param orgGroupId group id
   * @return
   */
  public OrgGroup getOrgGroup(String orgGroupId) {
    Assert.notNull(orgGroupId, "org group id can't be null");
    String url = groupConfig.getOrgGroupUrl(orgGroupId);
    final ResponseEntity<OrgGroup> orgGroupResponseEntity =
        adapterUtil.sendRequest(
            url,
            HttpMethod.GET,
            getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId()),
            null,
            null,
            OrgGroup.class);
    return orgGroupResponseEntity.getBody();
  }

  public List<GroupData> getUserGroupListFromChildren(JsonNode children) {
    if (children.isNull() || children.isMissingNode()) {
      return new ArrayList<>();
    }
    return StreamSupport.stream(children.spliterator(), false)
            .map(
                    item ->
                            new GroupData()
                                    .id(Utility.getStringFromJsonNode(item.path(groupConfig.getId())))
                                    .name(Utility.getStringFromJsonNode(item.path(groupConfig.getName())))
                                    .groupList(collectGroupListFromJsonNode(item.path(groupConfig.getGroupList()))))
            .collect(Collectors.toList());
  }

  private List<User> collectGroupMembersFromJsonNode(JsonNode members) {
    return StreamSupport.stream(members.spliterator(), false)
        .map(
            item ->
                new User()
                    .id(Utility.getStringFromJsonNode(item.path(GroupConstants.USERGROUP_MEMBERID)))
                    .name(
                        Utility.getStringFromJsonNode(
                            item.path(GroupConstants.USERGROUP_MEMBERNAME)))
                    .lock(true))
        .collect(Collectors.toList());
  }

  private List<String> collectGroupListFromJsonNode(JsonNode groupList) {
    return StreamSupport.stream(groupList.spliterator(), false)
            .map(
                    item ->
                            item.toString().replaceAll("\"", ""))
            .collect(Collectors.toList());
  }

  public List<MyDmsGroupData> getPathInfoByGid(String groupId) {
    JsonNode responseBody = getPathInfo(Collections.singletonList(groupId));
    List<MyDmsGroupData> result =
        StreamSupport.stream(responseBody.spliterator(), false)
            .findFirst()
            .map(
                data ->
                    StreamSupport.stream(data.spliterator(), false)
                        .map(
                            pathData ->
                                new MyDmsGroupData()
                                    .groupId(
                                        Utility.getStringFromJsonNode(
                                            pathData.path(groupConfig.getId())))
                                    .groupName(
                                        Utility.getStringFromJsonNode(
                                            pathData.path(groupConfig.getName())))
                                    .groupType(
                                        transToCommunityCategory(
                                            Utility.getStringFromJsonNode(
                                                pathData.path(GroupConstants.USERGROUP_TYPE)))))
                        .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    Collections.reverse(result);
    return result;
  }

  public Map<String, List<MyDmsGroupData>> getPathInfoByGidsWithLang(
      List<String> groupIds, String lang) {
    JsonNode responseBody = getPathInfo(groupIds);
    return StreamSupport.stream(responseBody.spliterator(), false)
        .map(
            data ->
                StreamSupport.stream(data.spliterator(), false)
                    .map(
                        pathData ->
                            new MyDmsGroupData()
                                .groupId(
                                    Utility.getStringFromJsonNode(
                                        pathData.path(groupConfig.getId())))
                                .groupName(
                                    Locale.US.toLanguageTag().equalsIgnoreCase(lang)
                                        ? Optional.ofNullable(
                                                Utility.getStringFromJsonNode(
                                                    pathData.path(GROUP_FIELD_ENAME)))
                                            .filter(StringUtils::isNoneBlank)
                                            .orElseGet(
                                                () ->
                                                    Utility.getStringFromJsonNode(
                                                        pathData.path(groupConfig.getName())))
                                        : Utility.getStringFromJsonNode(
                                            pathData.path(groupConfig.getName())))
                                .groupType(
                                    transToCommunityCategory(
                                        Utility.getStringFromJsonNode(
                                            pathData.path(GroupConstants.USERGROUP_TYPE)))))
                    .collect(Collectors.toList()))
        .collect(
            HashMap::new,
            (map, paths) -> {
              Collections.reverse(paths);
              map.put(paths.get(NumberUtils.INTEGER_ZERO).getGroupId(), paths);
            },
            Map::putAll);
  }

  private JsonNode getPathInfo(List<String> groupIds) {
    if (CollectionUtils.isEmpty(groupIds)) {
      return mapper.createArrayNode();
    }
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getPathUrl(),
            HttpMethod.POST,
            getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId()),
            buildGidsBody(groupIds).toString(),
            null,
            JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (Objects.isNull(responseBody)) {
      throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
    }
    return responseBody;
  }

  private String transToCommunityCategory(String myDmsType) {
    String type = null;
    if (myDmsType.equals(GroupConstants.USERGROUP_DEPGROUP)) {
      type = CommunityCategory.DEPARTMENT.toString();
    } else if (myDmsType.equals(GroupConstants.USERGROUP_PROJGROUP)) {
      type = CommunityCategory.PROJECT.toString();
    }
    return type == null ? CommunityCategory.PROJECT.toString() : type;
  }

  public ResponseEntity<JsonNode> getGroupBeanByGidWithResponse(String groupId) {
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    //headers.set(Constants.HEADER_NEED_GROUP_LIST, "true");
    return adapterUtil.sendRequest(
        groupConfig.getOrgGroupUrl(groupId) + "?needGroupList=true",
        HttpMethod.GET,
        headers,
        null,
        null,
        JsonNode.class);
  }

  public List<String> getBelongGroupIdOfUser(String userId) {
    Set<String> belongGroupIdList = new HashSet<>();
    List<SourceGroup> privilegedGroupList =
        getPrivilegedUserGroupByUserIdAndFilter(userId, StringUtils.EMPTY, false);
    belongGroupIdList.addAll(
        privilegedGroupList.parallelStream().map(SourceGroup::getId).collect(Collectors.toSet()));
    List<String> adminManagerMembersGroupIdList =
        privilegedGroupList
            .parallelStream()
            .filter(
                group ->
                    group.getName().matches(GroupConstants.ADMIN_MANAGER_ALLMEMBER_FILTER_REGEX))
            .map(SourceGroup::getId)
            .distinct()
            .collect(Collectors.toList());
    List<TargetSourceGroup> sourceGroupList =
        getCustomSourceGroupsByTargetGroupIds(adminManagerMembersGroupIdList);
    belongGroupIdList.addAll(
        sourceGroupList
            .parallelStream()
            .map(TargetSourceGroup::getSource)
            .filter(Objects::nonNull)
            .flatMap(List::parallelStream)
            .map(SourceGroup::getId)
            .collect(Collectors.toSet()));
    return new ArrayList<>(belongGroupIdList);
  }

  private List<SourceGroup> getPrivilegedUserGroupByUserIdAndFilter(
      String userId, String filter, boolean deep) {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(groupConfig.getDeep(), Boolean.toString(deep));
    uriVariables.add(groupConfig.getUid(), userId);
    Optional.ofNullable(filter)
        .filter(StringUtils::isNotBlank)
        .ifPresent(filterValue -> uriVariables.add(groupConfig.getFilter(), filterValue));
    HttpHeaders httpHeaders = getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId());
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getGroupUrl(),
            HttpMethod.POST,
            httpHeaders,
            buildGidsBody(Collections.emptyList()),
            uriVariables,
            JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    return Optional.ofNullable(responseBody)
        .map(
            body ->
                StreamSupport.stream(body.spliterator(), false)
                    .map(
                        group -> {
                          try {
                            return mapper.treeToValue(group, SourceGroup.class);
                          } catch (JsonProcessingException e) {
                            log.error(e);
                            return null;
                          }
                        })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()))
        .orElseGet(ArrayList::new);
  }

  public List<String> getPrivilegedUserGroupIdByUserIdAndFilter(String userId, boolean deep) {
    return getPrivilegedUserGroupByUserIdAndFilter(userId, StringUtils.EMPTY, deep)
        .parallelStream()
        .map(SourceGroup::getId)
        .distinct()
        .collect(Collectors.toList());
  }

  public List<TargetSourceGroup> getCustomSourceGroupsByTargetGroupIds(
      List<String> targetGroupIds) {
    MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
    uriVariables.add(groupConfig.getFilter(), GroupConstants.CUSTOM_GROUP_FILTER);
    HttpHeaders httpHeaders = getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId());
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getGroupListUrl(),
            HttpMethod.POST,
            httpHeaders,
            buildGidsBody(targetGroupIds),
            uriVariables,
            JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    return Optional.ofNullable(responseBody)
        .map(
            body ->
                StreamSupport.stream(body.spliterator(), false)
                    .map(
                        item -> {
                          try {
                            return mapper.treeToValue(item, TargetSourceGroup.class);
                          } catch (JsonProcessingException e) {
                            log.error(e);
                            return null;
                          }
                        })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))
        .orElseGet(ArrayList::new);
  }

  public List<User> getSystemAdminMembers() {
    return getGroupMembers(yamlConfig.getAppId(), yamlConfig.getSysAdminGid());
  }

  public List<User> getGroupMembers(String appKey, String gid) {
    HttpHeaders headers = getHttpHeadersWithAppKey(appKey);
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            groupConfig.getMembersUrl(gid), HttpMethod.GET, headers, null, null, JsonNode.class);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (responseBody == null) {
      return Collections.emptyList();
    }
    return StreamSupport.stream(responseBody.spliterator(), false)
        .map(
            member -> {
              try {
                return mapper.treeToValue(member, GroupMember.class);
              } catch (JsonProcessingException e) {
                log.error(e);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .map(member -> new User().id(member.getMemberId()).name(member.getMemberName()))
        .collect(Collectors.toList());
  }

  public List<DeltaPointInfo> getDeltaPoint(
      String userId, long startTime, long endTime, boolean detail) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    List<DeltaPointInfo> list = new ArrayList<>();

    try {
      MultiValueMap<String, String> uriVariables = new LinkedMultiValueMap<>();
      uriVariables.add(groupConfig.getStartTime(), convertToDateTime(dateTimeFormatter, startTime));
      uriVariables.add(groupConfig.getEndTime(), convertToDateTime(dateTimeFormatter, endTime));
      uriVariables.add(groupConfig.getWithDetail(), String.valueOf(detail));
      ResponseEntity<JsonNode> response =
          adapterUtil.sendRequest(
              groupConfig.getDeltapointsUrl(userId),
              HttpMethod.GET,
              adapterUtil.generateHeaderWithCookies(),
              null,
              uriVariables,
              JsonNode.class);
      log.debug("get delta points , status = " + response.getStatusCode());
      JsonNode responseBody = adapterUtil.getResponseBody(response);
      if (!Objects.isNull(responseBody)) {
        responseBody
            .path(Constants.RESPONSE_DATA)
            .forEach(p -> list.add(new ObjectMapper().convertValue(p, DeltaPointInfo.class)));
      }
    } catch (Exception e) {
      log.error("get delta points fail !! ");
      log.error(e);
    }
    return list;
  }

  private Map<String, List<String>> getUserIdQueryMap(List<String> queryValue) {
    Map<String, List<String>> queryMap = new LinkedHashMap<>();
    queryValue
        .parallelStream()
        .filter(StringUtils::isNotEmpty)
        .collect(
            Collectors.groupingBy(
                id -> id.substring(NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ONE)))
        .entrySet()
        .forEach(
            entry -> {
              if (USERGROUP_PREFIX_U.equals(entry.getKey())) {
                queryMap.put(groupConfig.getBasicsUids(), entry.getValue());
              } else if (USERGROUP_PREFIX_M.equals(entry.getKey())) {
                queryMap.put(groupConfig.getBasicsMids(), entry.getValue());
              }
            });
    return queryMap;
  }

  private HttpHeaders getHttpHeadersWithAppKey(String appKey) {
    HttpHeaders httpHeaders = adapterUtil.generateHeader(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.set(Constants.HEADER_APP_KEY_NAME, appKey);
    return httpHeaders;
  }

  private ObjectNode buildGidsBody(List<String> gids) {
    ObjectNode jsonBody = mapper.createObjectNode();
    ArrayNode gidsNode = jsonBody.putArray(GroupConstants.GIDS);
    Optional.ofNullable(gids).orElseGet(ArrayList::new).forEach(gidsNode::add);
    return jsonBody;
  }

  public List<UserGroup> getUserGroupDetail(List<String> userGroupIds) {
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    headers.set(RequestId.CORRELATION_ID_HEADER, RequestId.get());
    HttpEntity<String> entity = new HttpEntity<>(buildGidsBody(userGroupIds).toString(), headers);
    UriComponentsBuilder urlBuilder =
        UriComponentsBuilder.fromHttpUrl(groupConfig.getBaseUrl().append("userGroups").toString());
    ResponseEntity<List<UserGroup>> orgGroupResponseEntity =
        adapterUtil
            .restTemplate()
            .exchange(
                urlBuilder.toUriString(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<UserGroup>>() {});
    return orgGroupResponseEntity.getBody();
  }

  public OrgProfile getOrgProfileByGid(String gid) {
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getMydmsAppId());
    HttpEntity<String> entity = new HttpEntity<>(StringUtils.EMPTY, headers);
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(
            String.format("%s/%s/%s", groupConfig.getBaseUrl(), "department", gid));
    log.info(builder.toUriString());

    ResponseEntity<OrgProfile> groupResponseEntity =
        adapterUtil
            .restTemplate()
            .exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<OrgProfile>() {});
    if (groupResponseEntity.getStatusCode() == HttpStatus.OK) {
      return groupResponseEntity.getBody();
    } else {
      log.error(GROUPSERVICE_HTTP_STATUS + groupResponseEntity.getStatusCodeValue());
      return null;
    }
  }

  private String convertToDateTime(DateTimeFormatter dateTimeFormatter, long time) {
    ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
    return dateTime.format(dateTimeFormatter);
  }

  /* On Community/Forum create, call UserGroup API to create Admin & Member groups.
  * ParentId: For Community, it will be application group root.
  *           For Forum, it will be its community member-group-Id */
  public String createUserGroup(Role targetRole, int associatedId, String groupParentId, List<String> memberIdList) {
    // prepare body parameter
    String userGroupName = buildUserGroupName(targetRole, associatedId);

    // send request
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    ResponseEntity<String> response =
            adapterUtil.sendRequest(
                    groupConfig.getUserGroupUrl(),
                    HttpMethod.POST,
                    headers,
                    buildUserGroupBody(userGroupName, groupParentId, memberIdList, null).toString(),
                    null,
                    String.class);
    return adapterUtil.getResponseBody(response);
  }

  public Boolean overwriteUserGroupMembers(Role targetRole, int associatedId, String groupParentId, List<String> memberIdList, String currentGid) {
    // prepare body parameter
    String userGroupName = buildUserGroupName(targetRole, associatedId);

    // send request
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    ResponseEntity<String> response =
            adapterUtil.sendRequest(
                    groupConfig.getUserGroupUrl(),
                    HttpMethod.PUT,
                    headers,
                    buildUserGroupBody(userGroupName, groupParentId, memberIdList, currentGid).toString(),
                    null,
                    String.class);

    if (!HttpStatus.OK.equals(response.getStatusCode())) {
      throw new CreationException(response.getBody());
    }
    return HttpStatus.OK.equals(response.getStatusCode());
  }

  public Boolean appendUserGroupMembers(String currentGid, List<String> memberIdList) {
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    ResponseEntity<String> response =
            adapterUtil.sendRequest(
                    groupConfig.getMembersUrl(currentGid),
                    HttpMethod.PUT,
                    headers,
                    buildMemberBody(memberIdList).toString(),
                    null,
                    String.class);
    if (!HttpStatus.OK.equals(response.getStatusCode())) {
      throw new CreationException(response.getBody());
    }
    return HttpStatus.OK.equals(response.getStatusCode());
  }

  public Boolean removeUserGroupMembers(String currentGid, List<String> memberIdList) {
    HttpHeaders headers = getHttpHeadersWithAppKey(yamlConfig.getAppId());
    headers.set(HttpHeaders.AUTHORIZATION, Jwt.get());
    ResponseEntity<String> response =
            adapterUtil.sendRequest(
                    groupConfig.getMembersUrl(currentGid),
                    HttpMethod.DELETE,
                    headers,
                    buildMemberBody(memberIdList).toString(),
                    null,
                    String.class);
    if (!HttpStatus.OK.equals(response.getStatusCode())) {
      throw new CreationException(response.getBody());
    }
    return HttpStatus.OK.equals(response.getStatusCode());
  }

  private String buildUserGroupName(Role targetRole, int associatedId) {
    String userGroupType = (Role.COMMUNITY_ADMIN.equals(targetRole)
            || Role.COMMUNITY_MEMBER.equals(targetRole))
            ? GroupConstants.USERGROUP_TYPE_COMMUNITY : GroupConstants.USERGROUP_TYPE_FORUM;
    String memberType = (Role.COMMUNITY_ADMIN.equals(targetRole)
            || Role.FORUM_ADMIN.equals(targetRole))
            ? GroupConstants.USERGROUP_NAME_ADMIN : GroupConstants.USERGROUP_NAME_MEMBER;
    return String.format("%s%s%d", memberType, userGroupType, associatedId);
  }

  private ObjectNode buildUserGroupBody(String userGroupName, String userGroupParentId,
                                        List<String> memberIdList, String gid) {
    ObjectNode jsonBody = mapper.createObjectNode();
    jsonBody.put(GroupConstants.USERGROUP_NAME, userGroupName);
    jsonBody.put(GroupConstants.USERGROUP_DESCRIPTION, userGroupName);
    jsonBody.put(GroupConstants.USERGROUP_PARENT_ID, userGroupParentId);
    jsonBody.put(GroupConstants.ID, gid);

    jsonBody.putArray(GroupConstants.USERGROUP_CUS_FIELDS);
    jsonBody.putArray(GroupConstants.USERGROUP_GROUP_LIST);

    ArrayNode membersNode = jsonBody.putArray(GroupConstants.USERGROUP_MEMBERS);
    Optional.ofNullable(memberIdList).orElseGet(ArrayList::new).forEach(memberId -> {
      ObjectNode innerNode = mapper.createObjectNode().put(GroupConstants.USERGROUP_MEMBERID, memberId);
      membersNode.add(innerNode);
      });
    return jsonBody;
  }

  private ObjectNode buildMemberBody(List<String> memberIdList) {
    ObjectNode jsonBody = mapper.createObjectNode();

    ArrayNode membersNode = jsonBody.putArray(GroupConstants.USERGROUP_UIDS);
    Optional.ofNullable(memberIdList).orElseGet(ArrayList::new).forEach(membersNode::add);
    return jsonBody;
  }
}
