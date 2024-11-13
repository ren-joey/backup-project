package com.delta.dms.community.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.delta.dms.community.swagger.model.ActivityLogData;
import com.delta.dms.community.swagger.model.SearchType;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.swagger.model.UserStatus;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;

public class Utility {

  public static final String COOKIE_NAME_DMS_JWT = "dms-jwt";
  public static final String COOKIE_COMMENT = "added from dmscommunity";
  private static final String HTML_TAG_A = "a";
  private static final String[] LINE_BREAK_TAGS = {"p", "br"};
  private static final String[] ATTRIBUTE_STYLE_TAGS = {
    "ol", "ul", "li", "table", "thead", "th", "tr", "td", "tbody"
  };
  private static final String[] DISPLAY_TAGS = {HTML_TAG_A, Constants.LINE_BREAKS};
  private static final String HTML_A_ATTR_HREF = "href";
  private static final String HTML_A_ATTR_TARGET = "target";
  private static final String HTML_ATTR_STYLE = "style";
  private static final LogUtil log = LogUtil.getInstance();

  private Utility() {}

  /**
   * Get the string value of the json node
   *
   * @param jsonNode Json node
   * @return Result string
   */
  public static String getStringFromJsonNode(JsonNode jsonNode) {
    if (jsonNode != null && !jsonNode.isMissingNode() && jsonNode.isTextual()) {
      return jsonNode.asText();
    }
    return "";
  }

  public static String getUserIdFromSession() {
    return getUserFromSession().getCommonUUID();
  }

  public static UserSession getUserFromSession() {
    UserSession user = getSessionAttribute(Constants.SESSION_USER_INFO);
    return user == null ? new UserSession() : user;
  }

  public static void setResponseCookie(String name, String value, int expire) {
    log.debug(name + " " + value + " " + expire);
    Cookie cookie = new Cookie(name, value);
    cookie.setPath(String.join(StringUtils.EMPTY, Constants.SLASH));
    cookie.setMaxAge(expire);
    cookie.setComment(COOKIE_COMMENT);
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    Optional.ofNullable(attr.getResponse()).ifPresent(response -> response.addCookie(cookie));
  }

  public static Cookie getRequestCookie(String name) {
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    Optional<Cookie[]> cookies = Optional.ofNullable(attr.getRequest().getCookies());
    if (cookies.isPresent()) {
      Optional<Cookie> jwtCookie =
          Stream.of(cookies.get()).filter(cookie -> cookie.getName().equals(name)).findFirst();
      if (jwtCookie.isPresent()) {
        return jwtCookie.get();
      }
    }
    return null;
  }

  public static void setSessionAttribute(String key, UserSession value) {
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    attr.getRequest().getSession(true).setAttribute(key, value);
  }

  public static UserSession getSessionAttribute(String key) {
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return (UserSession) attr.getRequest().getSession(true).getAttribute(key);
  }

  public static String getRequestHeaderAttribute(String key) {
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    return attr.getRequest().getHeader(key);
  }

  public static String getCurrentUserIdWithGroupId() {
    UserSession userSession = getUserFromSession();
    return getUserIdWithGroupInfo(userSession.getCommonUUID(), userSession.getGroup());
  }

  public static ActivityLogData setActivityLogData(
      String userId,
      String operation,
      String object,
      int objectPkId,
      String origin,
      String content,
      String attachmentId) {
    return new ActivityLogData()
        .userId(userId)
        .operation(operation)
        .object(object)
        .objectPk(objectPkId)
        .operationTime(System.currentTimeMillis())
        .origin(origin)
        .content(content)
        .attachmentId(attachmentId);
  }

  public static String getTextFromHtml(String htmlText) {
    Document document = Jsoup.parse(htmlText);
    Document.OutputSettings outputSettins = new Document.OutputSettings().prettyPrint(false);
    document.outputSettings(outputSettins);
    Stream.of(LINE_BREAK_TAGS).forEach(tag -> document.select(tag).prepend(Constants.LINE_BREAKS));

    Whitelist whitelist = new Whitelist();
    whitelist.addAttributes(HTML_TAG_A, HTML_A_ATTR_HREF, HTML_A_ATTR_TARGET);
    Stream.of(ATTRIBUTE_STYLE_TAGS).forEach(tag -> whitelist.addAttributes(tag, HTML_ATTR_STYLE));
    whitelist.addTags(ATTRIBUTE_STYLE_TAGS);
    whitelist.addTags(DISPLAY_TAGS);

    String cleanText =
        Jsoup.clean(document.body().outerHtml(), StringUtils.EMPTY, whitelist, outputSettins);
    return StringUtils.replace(cleanText, Constants.LINE_BREAKS, Constants.HTML_LINE_BREAKS);
  }

  public static boolean checkUserIsAuthor(String authorId, String userId) {
    return authorId.equals(userId);
  }

  public static String getUserIdWithGroupInfo(String userId, List<String> groupIds) {
    if(null == groupIds) {
      return userId;
    }
    String groupId =
        groupIds
            .stream()
            .filter(item -> !item.isEmpty())
            .collect(Collectors.joining(Constants.COMMA_DELIMITER));
    if (!groupId.isEmpty()) {
      return userId + Constants.COMMA_DELIMITER + groupId;
    } else {
      return userId;
    }
  }

  public static HttpHeaders generateHeaderWithAuthorization(String token) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(HttpHeaders.AUTHORIZATION, token);
    httpHeaders.setContentType(Constants.MEDIA_APPLICATION_JSON_UTF8);
    return httpHeaders;
  }

  public static Map<String, Object> getEmailParamMap(String title, String subTitle) {
    Map<String, Object> param = new HashMap<>();
    Optional.ofNullable(title).ifPresent(item -> param.put(EmailConstants.MAIL_TITLE, title));
    Optional.ofNullable(subTitle)
        .ifPresent(item -> param.put(EmailConstants.MAIL_SUB_TITLE, subTitle));
    return param;
  }

  public static UserStatus getUserStatus(int status) {
    UserStatus userStatus;
    switch (status) {
      case 1:
      case 2:
        userStatus = UserStatus.INACTIVE;
        break;
      case 0:
        userStatus = UserStatus.ACTIVE;
        break;
      default:
        userStatus = UserStatus.ACTIVE;
        break;
    }
    return userStatus;
  }

  public static int covertUserStatusToInt(UserStatus userStatus) {
    int value = 0;
    switch (userStatus) {
      case INACTIVE:
        value = 1;
        break;
      case ACTIVE:
        value = 0;
        break;
      default:
        value = 0;
        break;
    }
    return value;
  }

  public static List<SearchType> defaultSearchTypeList(List<SearchType> searchType) {
    return Optional.ofNullable(searchType)
        .filter(list -> !list.isEmpty())
        .orElseGet(() -> Arrays.stream(SearchType.values()).collect(Collectors.toList()));
  }
}
