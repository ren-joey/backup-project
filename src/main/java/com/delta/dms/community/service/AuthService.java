package com.delta.dms.community.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.JwtToken;
import com.delta.dms.community.config.DLConfig;
import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.swagger.model.AuthenticationToken;
import com.delta.dms.community.swagger.model.DeviceInfo;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.swagger.model.AppInfo;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.JwtParser;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;

@Service
public class AuthService {
  private UserGroupAdapter userGroupAdapter;
  private UserService userService;
  private DeviceService deviceService;
  private EventPublishService eventPublishService;
  private static final long RENEW_JWT_TIME_DIFF = 600;
  private List<String> filters = Arrays.asList("SRV-JARVIS", "SOFTBOT.ADMIN");
  private static final String EMAIL_REGEX =
      "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
  private static final String EMAIL_SEPARATOR = "@";
  private static final String SOURCE_OS_IOS = "iOS";
  private static final String SOURCE_OS_ANDROID = "android";
  private static final String DL_EMPLOYEE_TYPE = "DL";
  private LogUtil log = LogUtil.getInstance();
  private JwtParser parser;
  private Clock clock;
  private DLConfig dlConfig;

  @Autowired
  public AuthService(
      UserGroupAdapter userGroupAdapter,
      UserService userService,
      DeviceService deviceService,
      EventPublishService eventPublishService,
      DLConfig dlConfig) {
    this.userGroupAdapter = userGroupAdapter;
    this.userService = userService;
    this.deviceService = deviceService;
    this.eventPublishService = eventPublishService;
    this.parser = JwtParser.instance;
    this.clock = Clock.systemUTC();
    this.dlConfig = dlConfig;
  }

  public void setParser(JwtParser parser) {
    this.parser = parser;
  }

  public void setClock(Clock clock) {
    this.clock = clock;
  }

  public UserSession login(AuthenticationToken authenticationToken, String sourceOS) {
    //判斷前端來源，並取得相對應的application key
    String appKey = "";
    //目前只有mobile有application key, 如果不是從mobile來的，就給empty string
    if (null != sourceOS) {
        if (sourceOS.equals(SOURCE_OS_IOS) || sourceOS.equals(SOURCE_OS_ANDROID))
        	appKey = dlConfig.getMobileAppKey();    	
    }
    JwtToken jwtToken = userGroupAdapter.getUserTokenBySamAccountAndPassword(authenticationToken, appKey);
    if (null != jwtToken) {
      setResponseJwtCookie(jwtToken);
    } else {
      throw new AuthenticationException(I18nConstants.MSG_LOGIN_FAILED);
    }
    //預設登入者的name是login的username
    String userName = authenticationToken.getUsername();
    //假如登入者是直接人員，他的name就改用dl config中的username
    if(null != jwtToken.getEmployeeType()) {
        if(jwtToken.getEmployeeType().equals(DL_EMPLOYEE_TYPE))
        	userName = dlConfig.getDlUserName();
    }
    UserSession user = setSessionInfo(userName);
    //將ugs回傳的employee type值餵給user object
    if(null != jwtToken.getEmployeeType())
    	user.setEmployeeType(jwtToken.getEmployeeType());
    if (!filters.contains(user.getProfileSAMAccount())) {
      eventPublishService.publishActivityLogEvent(
          Utility.setActivityLogData(
              user.getCommonUUID(),
              Operation.LOGIN.toString(),
              PermissionObject.SYSTEM.toString(),
              0,
              Constants.INFO_PROJECT_NAME,
              Constants.CONTENT_EMPTY,
              Constants.ATTACHMENTID_EMPTY));
    }
    return user;
  }

  public void renewJwtCookiebyToken(String token) {
    if (isNeedRenewJwtToken()) {
      setResponseJwtCookie(userGroupAdapter.getUserTokenByToken(token));
    }
  }

  private boolean isNeedRenewJwtToken() {
    Cookie jwtCookie = Utility.getRequestCookie(Utility.COOKIE_NAME_DMS_JWT);
    long expireTime =
        TimeUnit.MILLISECONDS.toSeconds(parser.parseExp(jwtCookie.getValue()) - clock.millis());
    return expireTime < RENEW_JWT_TIME_DIFF;
  }

  private void setResponseJwtCookie(JwtToken jwtToken) {
    if (null != jwtToken) {
      int expire = Integer.parseInt(jwtToken.getExpiresIn());
      Utility.setResponseCookie(Utility.COOKIE_NAME_DMS_JWT, jwtToken.getAccessToken(), expire);
      Jwt.set(jwtToken.getAccessToken());
    }
  }

  public void registerDeviceToken(DeviceInfo deviceInfo) {
    Optional.ofNullable(deviceInfo)
        .filter(
            item ->
                !StringUtils.isBlank(item.getDeviceUUID())
                    && !StringUtils.isBlank(item.getDeviceToken()))
        .ifPresent(
            item ->
                deviceService.registerDevice(
                    deviceInfo
                        .userId(Utility.getUserIdFromSession())
                        .language(AcceptLanguage.get())));
  }

  public UserSession setSessionInfo(String username) {
    username = extractUserName(username);
    UserSession user = getUserInfo(username);
    Utility.setSessionAttribute(Constants.SESSION_USER_INFO, user);
    return user;
  }

  private UserSession getUserInfo(String username) {
    List<UserSession> userList =
        userService.getUserBySamAccounts(Arrays.asList(username));
    return getUser(userList);
  }

  private String extractUserName(String username) {
    if (username.matches(EMAIL_REGEX)) {
      return StringUtils.substringBefore(username, EMAIL_SEPARATOR);
    } else {
      return username;
    }
  }

  private UserSession getUser(List<UserSession> userList) {
    UserSession user = new UserSession();
    if (!userList.isEmpty()) {
      UserSession internalTalentUser = userList.get(0);
      String userId = internalTalentUser.getCommonUUID();
      user.commonUUID(internalTalentUser.getCommonUUID())
          .commonName(internalTalentUser.getCommonName().toUpperCase())
          .commonImage(internalTalentUser.getCommonImage())
          .profileDeptName(internalTalentUser.getProfileDeptName())
          .profileMail(internalTalentUser.getProfileMail())
          .profilePhone(internalTalentUser.getProfilePhone())
          .profileSAMAccount(internalTalentUser.getProfileSAMAccount().toUpperCase())
          .profileCname(internalTalentUser.getProfileCname())
          .profileEname(internalTalentUser.getProfileEname())
          .group(userGroupAdapter.getBelongGroupIdOfUser(userId))
          .isSystemAdmin(userService.isSysAdmin(userId));
    } else {
      throw new AuthenticationException(I18nConstants.MSG_ACCOUNT_NOT_FOUND);
    }
    return user;
  }

  public UserSession setSessionInfoByUserId(String userId) {
    UserSession user = getUserInfoById(userId);
    Utility.setSessionAttribute(Constants.SESSION_USER_INFO, user);
    return user;
  }

  private UserSession getUserInfoById(String userId) {
    List<UserSession> userList =
        userService.getUserById(Arrays.asList(userId), new ArrayList<>());
    return getUser(userList);
  }

  public UserSession getCurrentUserInfo() {
    try {
      String userId = parser.parseUserId(Jwt.get());
      log.debug("user id: " + userId);
      return checkIsDL(setSessionInfoByUserId(userId));
    } catch (AuthenticationException e) {
      log.error(e);
      throw new AuthenticationException(Constants.NOT_LOGIN);
    }
  }

  public AppInfo getCurrentUserAppInfo() {
    try {
      String userId = parser.parseUserId(Jwt.get());
      log.debug("user id: " + userId);
      AppInfo user = new AppInfo();
      user.isSystemAdmin(userService.isSysAdmin(userId));
      return user;
    } catch (AuthenticationException e) {
      log.error(e);
      throw new AuthenticationException(Constants.NOT_LOGIN);
    }
  }
  
  public DLInfo getDLUserInfo() {
    try {
    	//建立DLInfo object，預設isDL為false
    	DLInfo result = new DLInfo().setDL(false);
    	//取得登入者的user id
        String userId = parser.parseUserId(Jwt.get());
        //判斷此登入者id是否為DL專用帳號，如果是的話，把isDL設定為true，並且取得community跟forum白名單
        if (userId.equals(dlConfig.getDlUserId()))
        	result.setDL(true)
        		  .setAllowCommunityId(dlConfig.getDlCommunityId())
        		  .setAllowForumId(dlConfig.getDlForumId());
        return result;
      } catch (AuthenticationException e) {
        log.error(e);
        throw new AuthenticationException("");
      }
  }
  
  public UserSession checkIsDL(UserSession user) {
  	//取得登入者的user id
    String userId = user.getCommonUUID();
    //預設不是DL人員
    user.setEmployeeType("");
    //取得user id後，拿來跟config中的dl.allow-user-id做比對，比對為true的話，employeeType就設定為"DL"
    if (userId.equals(dlConfig.getDlUserId()))
    	user.setEmployeeType(DL_EMPLOYEE_TYPE);
    return user;
  }
  
  public boolean checkDLCommunityAuth(String communityId) {
	//取得登入者DLInfo
	DLInfo dlInfo = getDLUserInfo();
	//假如不是dl，回傳true
	if (dlInfo.isDL==false)
		return true;
	else {
		//假如是dl，檢查community id跟白名單是否一致
        Set<String> allowCommunitySet = org.springframework.util.StringUtils.commaDelimitedListToSet(dlInfo.getAllowCommunityId());
        for (String myVal : allowCommunitySet) {
        	if (myVal.equals(communityId))
        		return true;
        }
	}
    return false;
  }

  
  public boolean checkDLForumAuth(String forumId) {
	//取得登入者DLInfo
	DLInfo dlInfo = getDLUserInfo();
	//假如不是dl，回傳true
	if (dlInfo.isDL==false)
		return true;
	else {
		//假如是dl，檢查forum id跟白名單是否一致
        Set<String> allowForumSet = org.springframework.util.StringUtils.commaDelimitedListToSet(dlInfo.getAllowForumId());
        for (String myVal : allowForumSet) {
        	if (myVal.equals(forumId))
        		return true;
        }
	}
    return false;
  }
}
