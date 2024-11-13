package com.delta.dms.community.service;

import static com.delta.dms.community.utils.Constants.EMAIL_REGEX;
import static com.delta.dms.community.utils.Constants.SEMICOLON;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.JwtToken;
import com.delta.dms.community.adapter.entity.QueryUsers;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.UserDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.UserInfo;
import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.service.autocomplete.UserAutocompleteService;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import com.delta.dms.community.swagger.model.AutoCompleteUser;
import com.delta.dms.community.swagger.model.BasicInfo;
import com.delta.dms.community.swagger.model.GroupUser;
import com.delta.dms.community.swagger.model.GroupUserField;
import com.delta.dms.community.swagger.model.OutlookQueryData;
import com.delta.dms.community.swagger.model.OutlookQueryResult;
import com.delta.dms.community.swagger.model.OutlookQueryUnmatchedResult;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.swagger.model.UserQueryDto;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.swagger.model.UserStatus;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.Utility;

@Service
public class UserService {

  private static final String COMMON_NAME_FORMAT = "%s %s";
  protected static final List<GroupUserField> DEFAULT_USER_FIELD_LIST =
      Arrays.asList(GroupUserField.BASICINFO, GroupUserField.OFFICEINFO, GroupUserField.AVATAR);

  private UserGroupAdapter userGroupAdapter;
  private YamlConfig yamlConfig;
  private CommunityService communityService;
  private ForumService forumService;
  private UserAutocompleteService userAutocompleteService;
  private UserDao userDao;

  @Autowired
  public UserService(
      UserGroupAdapter userGroupAdapter,
      YamlConfig yamlConfig,
      @Lazy CommunityService communityService,
      @Lazy ForumService forumService,
      UserAutocompleteService userAutocompleteService,
      UserDao userDao) {
    this.userGroupAdapter = userGroupAdapter;
    this.yamlConfig = yamlConfig;
    this.communityService = communityService;
    this.forumService = forumService;
    this.userAutocompleteService = userAutocompleteService;
    this.userDao = userDao;
  }

  public User getUserById(String userId) {
    UserInfo userInfo = userDao.getUserById(userId);
    return Optional.ofNullable(userInfo)
        .map(
            user ->
                new User()
                    .id(userInfo.getUserId())
                    .name(userInfo.getCname())
                    .status(Utility.getUserStatus(userInfo.getStatus())))
        .orElseGet(() -> new User().id(userId));
  }

  public List<UserSession> getUserById(List<String> userIdList, List<GroupUserField> fieldList) {
    Optional.ofNullable(fieldList)
        .filter(CollectionUtils::isEmpty)
        .ifPresent(item -> item.addAll(DEFAULT_USER_FIELD_LIST));
    List<GroupUser> user = userGroupAdapter.getUserByUids(userIdList, fieldList);
    return convertToUserSession(user);
  }

  public List<UserSession> getUserBySamAccounts(List<String> samAccountList) {
    List<GroupUser> user =
        userGroupAdapter.getUserBySamAccounts(samAccountList, Collections.emptyList());
    return convertToUserSession(user);
  }

  public List<UserSession> getUserByUserQuery(UserQueryDto dto) {
    QueryUsers queryUsers =
        new QueryUsers()
            .setUids(dto.getUids())
            .setSamaccounts(dto.getSamAccounts())
            .setNames(dto.getNames());
    List<GroupUserField> fieldList = new ArrayList<>();
    fieldList.add(GroupUserField.BASICINFO);
    if (Boolean.TRUE.equals(dto.isWithAvatar())) {
      fieldList.add(GroupUserField.AVATAR);
    }

    List<GroupUser> user = userGroupAdapter.getUser(queryUsers, fieldList);
    return convertToUserSession(user);
  }

  public OutlookQueryResult searchOutlookUser(OutlookQueryData outlookQueryData) {
    List<String> splitData =
        Arrays.stream(StringUtils.split(outlookQueryData.getQuery(), SEMICOLON))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotEmpty)
            .distinct()
            .collect(toList());

    Map<String, List<String>> emailMap =
        splitData
            .stream()
            .filter(queryValue -> queryValue.matches(".*" + EMAIL_REGEX + ".*"))
            .collect(groupingBy(this::extractEmail, LinkedHashMap::new, toList()));
    if (emailMap.isEmpty()) {
      return new OutlookQueryResult()
          .match(emptyList())
          .unMatch(new OutlookQueryUnmatchedResult().delta(emptyList()).others(splitData));
    }
    List<String> queryEmail = new ArrayList<>(emailMap.keySet());
    List<GroupUser> groupUserList =
        userGroupAdapter.getUser(
            new QueryUsers().setEmails(queryEmail), Arrays.asList(GroupUserField.BASICINFO));
    Set<String> matchedEmails =
        groupUserList
            .stream()
            .map(groupUser -> groupUser.getBasicInfo().getEmail().toUpperCase())
            .collect(toSet());
    List<String> unmatchedList =
        emailMap
            .entrySet()
            .stream()
            .filter(entry -> !matchedEmails.contains(entry.getKey()))
            .map(Entry::getValue)
            .flatMap(List::stream)
            .distinct()
            .collect(toList());
    if (ofNullable(outlookQueryData.getCommunityId())
            .map(id -> Objects.equals(INTEGER_ZERO, id))
            .orElse(true)
        && ofNullable(outlookQueryData.getForumId())
            .map(id -> Objects.equals(INTEGER_ZERO, id))
            .orElse(true)) {
      return convertToOutlookQueryResult(queryEmail, unmatchedList, groupUserList);
    } else {
      Set<String> validUserIds =
          getValidUsersById(outlookQueryData).stream().map(User::getId).collect(toSet());
      return convertToOutlookQueryResult(queryEmail, unmatchedList, groupUserList, validUserIds);
    }
  }

  public String getOutlookUserByUserId(List<String> uidList) {
    List<GroupUser> groupUserList =
        userGroupAdapter.getUserByUids(
            uidList, Collections.singletonList(GroupUserField.BASICINFO));
    return groupUserList
        .stream()
        .filter(groupUser -> StringUtils.isNotBlank(groupUser.getBasicInfo().getEmail()))
        .map(
            groupUser ->
                String.format(
                    Constants.EMAIL_OUTLOOK_FORMAT,
                    getCommonName(groupUser.getBasicInfo()),
                    groupUser.getBasicInfo().getEmail()))
        .collect(Collectors.joining(Constants.SEMICOLON));
  }

  public List<AutoCompleteUser> searchUserByName(
      String q, int limit, List<String> exclude, boolean withAvatar) {
    List<GroupUserField> fieldList = new ArrayList<>();
    fieldList.add(GroupUserField.BASICINFO);
    fieldList.add(GroupUserField.OFFICEINFO);
    Optional.of(withAvatar)
        .filter(item -> item)
        .ifPresent(item -> fieldList.add(GroupUserField.AVATAR));
    List<String> excludeList = Optional.ofNullable(exclude).orElseGet(ArrayList::new);
    Map<String, String> suggestionMap =
        userAutocompleteService.getSuggestions(q, limit + excludeList.size());
    List<String> userIdList =
        suggestionMap
            .entrySet()
            .parallelStream()
            .filter(entry -> !excludeList.contains(entry.getKey()))
            .map(Entry::getKey)
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    Map<String, UserSession> userMap =
        getUserById(userIdList, fieldList)
            .stream()
            .collect(
                LinkedHashMap::new,
                (map, item) -> map.put(item.getCommonUUID(), item),
                Map::putAll);
    return userIdList
        .stream()
        .map(
            item -> {
              UserSession user = Optional.ofNullable(userMap.get(item)).orElseGet(UserSession::new);
              return new AutoCompleteUser()
                  .id(item)
                  .name(user.getCommonName())
                  .department(user.getProfileDeptName())
                  .ext(user.getProfilePhone())
                  .imgAvatar(withAvatar ? user.getCommonImage() : StringUtils.EMPTY)
                  .mail(user.getProfileMail())
                  .status(UserStatus.fromValue(user.getStatus()));
            })
        .collect(Collectors.toList());
  }

  public List<String> getEmailByUserId(List<String> userIdList) {
    List<GroupUser> user = userGroupAdapter.getUserByUids(userIdList, new ArrayList<>());
    return user.stream().map(item -> item.getBasicInfo().getEmail()).collect(Collectors.toList());
  }

  public boolean isSysAdmin() {
    return isSysAdmin(Utility.getUserIdFromSession());
  }

  public boolean isSysAdmin(String uid) {
    UserSession userSession = Utility.getUserFromSession();

    // when target uid is current user and data not expired, use session status
    if (Utility.getUserIdFromSession().equals(uid)) {
      if (null != userSession.getSystemAdminExpiredTime()
              && null != userSession.isIsSystemAdmin()
              && userSession.getSystemAdminExpiredTime() >= System.currentTimeMillis()) {
        return userSession.isIsSystemAdmin();
      }
    }

    // otherwise, get data from UserGroup API and save
    boolean isSystemAdmin = getSystemAdminIds()
        .parallelStream()
        .anyMatch(
            adminId ->
                Arrays.asList(uid.split(Constants.COMMA_DELIMITER))
                    .parallelStream()
                    .map(StringUtils::trim)
                    .anyMatch(u -> StringUtils.equals(u, adminId)));
    userSession.isSystemAdmin(isSystemAdmin);
    userSession.setSystemAdminExpiredTime(System.currentTimeMillis() + Constants.USER_GROUP_DATA_VALID_MILLISEC);
    return isSystemAdmin;
  }

  public List<String> getSystemAdminIds() {
    return Optional.ofNullable(userGroupAdapter.getSystemAdminMembers())
        .orElseGet(ArrayList::new)
        .stream()
        .map(User::getId)
        .collect(Collectors.toList());
  }

  public List<User> splitRecipient(List<User> recipient) {
    if (!recipient.isEmpty()) {
      String userIdList = recipient.get(0).getId();
      return getUserById(
              Arrays.stream(userIdList.split(Constants.COMMA_DELIMITER))
                  .collect(Collectors.toList()),
              Arrays.asList(GroupUserField.BASICINFO))
          .stream()
          .map(item -> new User().id(item.getCommonUUID()).name(item.getCommonName()))
          .collect(Collectors.toList());
    }
    return recipient;
  }

  public UserSession getUserInfo(String account) {
    List<UserSession> userList = getUserBySamAccounts(Arrays.asList(account));
    UserSession user = new UserSession();
    if (!userList.isEmpty()) {
      UserSession internalTalentUser = userList.get(0);
      String userId = internalTalentUser.getCommonUUID();
      user.commonUUID(internalTalentUser.getCommonUUID())
          .group(userGroupAdapter.getBelongGroupIdOfUser(userId));
    } else {
      throw new AuthenticationException(I18nConstants.MSG_ACCOUNT_NOT_FOUND);
    }
    return user;
  }

  public UserStatus getUserStatus(String userId) {
    int status = userDao.getUserStatus(userId);
    return Utility.getUserStatus(status);
  }

  public List<User> getUserByIds(List<String> userIdList) {
    if (Optional.ofNullable(userIdList).orElseGet(ArrayList::new).isEmpty()) {
      return new ArrayList<>();
    }

    return userDao
        .getUserByIds(userIdList)
        .parallelStream()
        .filter(Objects::nonNull)
        .distinct()
        .map(
            userInfo ->
                new User()
                    .id(userInfo.getUserId())
                    .name(userInfo.getCname())
                    .mail(userInfo.getMail())
                    .status(Utility.getUserStatus(userInfo.getStatus())))
        .collect(Collectors.toList());
  }

  private List<UserSession> convertToUserSession(List<GroupUser> userList) {
    return userList
        .stream()
        .map(this::convertToUserSession)
        .filter(item -> !item.getCommonUUID().isEmpty())
        .collect(Collectors.toList());
  }

  private UserSession convertToUserSession(GroupUser user) {
    UserSession result =
        new UserSession()
            .commonUUID(user.getUid())
            .commonImage(StringUtils.trimToEmpty(user.getAvatar()))
            .status(
                Boolean.TRUE.equals(user.isOnduty())
                    ? UserStatus.ACTIVE.toString()
                    : UserStatus.INACTIVE.toString());
    Optional.ofNullable(user.getBasicInfo())
        .ifPresent(
            basicInfo ->
                result
                    .commonName(getCommonName(basicInfo))
                    .profileSAMAccount(
                        StringUtils.trimToEmpty(basicInfo.getAccount()).toUpperCase())
                    .profileDeptName(StringUtils.trimToEmpty(basicInfo.getDepartment()))
                    .profileMail(StringUtils.trimToEmpty(basicInfo.getEmail()))
                    .profileCname(StringUtils.trimToEmpty(basicInfo.getLocalName()).toUpperCase())
                    .profileEname(
                        StringUtils.trimToEmpty(basicInfo.getEnglishName()).toUpperCase()));
    Optional.ofNullable(user.getOfficeInfo())
        .ifPresent(
            officeInfo ->
                result
                    .profilePhone(StringUtils.trimToEmpty(officeInfo.getExtension()))
                    .profileOffice(StringUtils.trimToEmpty(officeInfo.getOffice())));
    return result;
  }

  private String getCommonName(BasicInfo basicInfo) {
    if (!StringUtils.isEmpty(basicInfo.getDisplayName())) {
      return basicInfo.getDisplayName();
    }
    String commonName =
        StringUtils.trimToEmpty(basicInfo.getLocalName()).isEmpty()
            ? StringUtils.trimToEmpty(basicInfo.getEnglishName())
            : StringUtils.trimToEmpty(basicInfo.getLocalName());
    if (commonName.isEmpty()) {
      return basicInfo.getAccount().toUpperCase();
    } else {
      return String.format(COMMON_NAME_FORMAT, basicInfo.getAccount(), commonName).toUpperCase();
    }
  }

  private OutlookQueryResult convertToOutlookQueryResult(
      List<String> queryEmail, List<String> invalidOutlookData, List<GroupUser> deltaUserList) {
    Map<String, User> activeUserMap =
        deltaUserList
            .stream()
            .map(this::convertToUser)
            .filter(user -> UserStatus.ACTIVE.equals(user.getStatus()))
            .collect(
                toMap(user -> user.getMail().toUpperCase(), Function.identity(), (v1, v2) -> v1));
    List<User> matchedUserList =
        queryEmail
            .stream()
            .filter(activeUserMap::containsKey)
            .map(activeUserMap::get)
            .collect(toList());
    return new OutlookQueryResult()
        .match(matchedUserList)
        .unMatch(
            new OutlookQueryUnmatchedResult()
                .delta(
                    deltaUserList
                        .stream()
                        .map(this::convertToUser)
                        .filter(user -> !activeUserMap.containsKey(user.getMail().toUpperCase()))
                        .collect(toList()))
                .others(invalidOutlookData));
  }

  private OutlookQueryResult convertToOutlookQueryResult(
      List<String> queryEmail,
      List<String> invalidOutlookData,
      List<GroupUser> deltaUserList,
      Set<String> validUserIds) {
    OutlookQueryResult result =
        convertToOutlookQueryResult(
            queryEmail,
            invalidOutlookData,
            deltaUserList
                .stream()
                .filter(user -> validUserIds.contains(user.getUid()))
                .collect(toList()));
    Set<String> matchedEmails =
        result.getMatch().stream().map(User::getMail).map(String::toUpperCase).collect(toSet());
    List<User> unmatchedUsers =
        deltaUserList
            .stream()
            .map(this::convertToUser)
            .filter(user -> !matchedEmails.contains(upperCase(user.getMail())))
            .sorted(Comparator.comparing(user -> queryEmail.indexOf(user.getMail().toUpperCase())))
            .collect(toList());
    result.getUnMatch().setDelta(unmatchedUsers);
    return result;
  }

  private User convertToUser(GroupUser groupUser) {
    return new User()
        .status(Boolean.TRUE.equals(groupUser.isOnduty()) ? UserStatus.ACTIVE : UserStatus.INACTIVE)
        .id(groupUser.getUid())
        .name(getCommonName(groupUser.getBasicInfo()))
        .mail(groupUser.getBasicInfo().getEmail());
  }

  public String getSysAdminToken() {
    return getUserToken(yamlConfig.getSysAdminAuthenticationToken());
  }

  public String getUserToken(AuthenticationToken token) {
    return Optional.ofNullable(userGroupAdapter.getUserTokenBySamAccountAndPassword(token, ""))
        .map(JwtToken::getAccessToken)
        .orElse(EMPTY);
  }

  private List<User> getValidUsersById(OutlookQueryData outlookQueryData) {
    if (!INTEGER_ZERO.equals(outlookQueryData.getForumId())) {
      return forumService.getMemberOfForum(outlookQueryData.getForumId(), -1, -1);
    } else {
      CommunityInfo communityInfo =
          communityService.getCommunityInfoById(outlookQueryData.getCommunityId());
      return communityService.getAllMemberOfCommunityById(
          communityInfo.getCommunityId(),null, null, -1, -1);
    }
  }

  private String extractEmail(String str) {
    Pattern pattern = Pattern.compile(EMAIL_REGEX);
    Matcher matcher = pattern.matcher(str);
    if (matcher.find()) {
      return matcher.group().toUpperCase();
    }
    return null;
  }
}
