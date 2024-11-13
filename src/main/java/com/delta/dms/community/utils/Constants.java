package com.delta.dms.community.utils;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Constants {

  // error
  public static final String ERR_INVALID_PARAM = "Invalid parameters";

  public static final String CORS_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String CORS_ALLOW_METHODS = "Access-Control-Allow-Methods";
  public static final String CORS_ALLOW_HEADERS = "Access-Control-Allow-Headers";
  public static final String CORS_MAX_AGE = "Access-Control-Max-Age";
  public static final String CORS_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
  public static final String CORS_REQ_METHOD = "Access-Control-Request-Method";

  public static final String CORS_ALLOW_ORIGIN_ALL = "*";
  public static final String CORS_REQ_METHODS = "GET, POST, OPTIONS";
  public static final String CORS_REQ_HEADERS = "Content-Type, Authorization";
  public static final String CORS_MAX_AGE_7200 = "7200";
  public static final String CORS_CREDENTIALS = "true";
  public static final String CORS_REQ_OPTIONS = "OPTIONS";

  public static final String IBM_WAS = "IBM WebSphere Application Server";
  public static final String IBM_SECURITY_LOGOUT = "/ibm_security_logout";
  public static final String LTPA_TOKEN2 = "LtpaToken2";

  public static final String LOGIN_SUCCESS = "login successfully";
  public static final String NOT_LOGIN = "Not login";

  public static final String RESPONSE_ERROR = "error";
  public static final String RESPONSE_SUCCESS = "success";

  public static final String MEDIA_TYPE_APPLICATION = "application";
  public static final String MEDIA_SUBTYPE_APPLICATION = "json";
  public static final String AUTO_USERID = "AUTO";
  public static final String JSON_EMPTY = "json data is empty";

  public static final String RESPONSE_RESULT = "result";
  public static final String RESPONSE_DATA = "data";
  public static final String RESPONSE_RESULTS = "results";

  public static final String USER_UUID_FIELD = "uid";
  public static final String USER_NAME_FIELD = "Common.Name";
  public static final String USER_AVATAR_FIELD = "avatar";
  public static final String USER_DEPARTMENT_FIELD = "department";
  public static final String USER_EXT_FIELD = "extension";
  public static final String USER_ACCOUNT_FIELD = "account";
  public static final String USER_MAIL_FIELD = "email";
  public static final String USER_CNAME_FIELD = "Profile.Cname";
  public static final String USER_ENAME_FIELD = "englishName";
  public static final String SESSION_USER_INFO = "userInfo";
  public static final String USER_LOCALNAME_FIELD = "localName";
  public static final String USER_BASICINFO_FIELD = "basicInfo";
  public static final String USER_OFFICEINFO_FIELD = "officeInfo";

  public static final String INTERNALTALENT_QUERY_TYPE_UUID = "uuid";
  public static final String INTERNALTALENT_QUERY_TYPE_ACCOUNT = "account";

  public static final String SQL_COUNT = "count";
  public static final String SQL_APPLICANT_ID = "application_id";
  public static final String SQL_APPLICATION_DESC = "application_desc";
  public static final String SQL_DESC = "desc";

  public static final int REPLY_CONCLUSION_INDEX = 0;
  public static final String REPLY_WITH_PREFIX_FORMAT = "[%s]%s";
  public static final String REPLY_WITH_POSTFIX_FORMAT = "%s[%s]";
  public static final String REPLY_QUESTION_CONCLUSION_TEXT_FORMAT = "%s: %s\n\n%s: %s\n\n%s: %s";
  public static final String REPLAY_RICH_TEXT_ATTACHMENT_MATCH_REGEX = "(<img|<iframe)";
  public static final String REPLAY_RICH_TEXT_TABLE_MATCH_REGEX = "<table";
  public static final Pattern REPLAY_RICH_TEXT_ATTACHMENT_PATTERN = Pattern.compile(
          REPLAY_RICH_TEXT_ATTACHMENT_MATCH_REGEX, Pattern.MULTILINE);
  public static final Pattern REPLAY_RICH_TEXT_TABLE_MATCH_REGEX_PATTERN = Pattern.compile(
          REPLAY_RICH_TEXT_TABLE_MATCH_REGEX, Pattern.MULTILINE);

  // info
  public static final String INFO_PROJECT_NAME = "community";
  public static final String INFO_NAME_DMS = "myDMS";

  public static final String HEADER_COOKIE = "Cookie";
  public static final String GROUP_ALPHABET = "G";

  // for removing img src
  public static final String BASE64_CONTEXT = "data:image/.*;base64,";
  public static final String BASE64_JPEG_CONTEXT = "data:image/jpeg;base64,";
  public static final String IMAGE_CONTEXT_REPLACEMENT = "";

  public static final String CONTENT_EMPTY = "";
  public static final String CONTENT_RAW_FILE = "raw file";
  public static final String CONTENT_PDF_FILE = "pdf file";
  public static final String ATTACHMENTID_EMPTY = "";

  public static final String JPG = "jpg";
  public static final String PUBLIC = "public";
  public static final String PRIVATE = "private";

  public static final String AUTO_COMPLETE_REGEX_FORMAT = "(?i)(^%s.*|.* %s.*)";

  public static final String COMMA_DELIMITER = ",";
  public static final String SEMICOLON = ";";
  public static final String SLASH = "/";
  public static final String COLON = ":";
  public static final String EQUAL = "=";
  public static final String QUESTION_MARK = "?";
  public static final String LINEBREAK_TAB_REGEX = "(\\n|\\r|\\t)";

  public static final String INFO_NAME_PQM = "pqm";
  public static final String HTML_BR = "<br/>";
  public static final String TITLE_TOPIC_PQM_NAME = "【PQM問題及對策】";

  public static final String TCP = "tcp://";
  public static final int QOS = 0;
  public static final int HTTPCLIENT_TIMEOUT = 30000;
  public static final String CONNECT = " CONNECT ";
  public static final String ERROR = " ERROR ";

  public static final String HEADER_APP_KEY_NAME = "AppKey";
  public static final String HEADER_NEED_GROUP_LIST = "needGroupList";

  public static final MediaType MEDIA_APPLICATION_JSON_UTF8 =
      new MediaType(
          Constants.MEDIA_TYPE_APPLICATION,
          Constants.MEDIA_SUBTYPE_APPLICATION,
          StandardCharsets.UTF_8);

  public static final String ACTIVITY_APP_VERSION = "v3.0";
  public static final String ACTIVITY_LOG_OBJECT = "object";
  public static final String ACTIVITY_LOG_CONTENT = "content";
  public static final String ACTIVITY_LOG_SOURCE_OS_VERSION = "sourceOsVersion";
  public static final int LAST_WEEK_START = -6;
  public static final int LAST_WEEK_END = 0;
  public static final int WEEK_BEFORE_LAST_WEEK_START = -13;
  public static final int WEEK_BEFORE_LAST_WEEK_END = -7;

  public static final String EMAIL_REGEX = "\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b";
  public static final String EMAIL_OUTLOOK_FORMAT = "%s <%s>";
  public static final String LINE_BREAKS = "\n";
  public static final String HTML_LINE_BREAKS = "<br>";

  public static final String DEFAULT_LOG_SENDER = "Community";

  public static final int COMMUNITY_ROLE_PRIORITY_MANAGER = 1;
  public static final int COMMUNITY_ROLE_PRIORITY_ADMIN = 2;
  public static final int COMMUNITY_ROLE_PRIORITY_MEMBER = 3;
  public static final int COMMUNITY_ROLE_PRIORITY_KNOWLEDGE_ADMIN = 4;
  public static final int COMMUNITY_ROLE_PRIORITY_KM_KNOWLEDGE_UNIT = 5;
  public static final int COMMUNITY_ROLE_PRIORITY_SUPPLIER_KU = 6;
  public static final int COMMUNITY_ROLE_PRIORITY_KM = 7;
  public static final int COMMUNITY_ROLE_PRIORITY_CUSTOM = 8;

  public static final int COMMUNITY_LIST_MEMBER_DISPLAY_NUM = 40;
  public static final int FORUM_LIST_MEMBER_DISPLAY_NUM = 20;
  public static final int USER_GROUP_DATA_VALID_MILLISEC = 5 * 60 * 1000; // 5 min
  public static final String DRC_SYNC_USER = "user";
  public static final String DRC_SYNC_ID = "id";
  public static final String DRC_SYNC_USERNAME = "username";
  public static final String DRC_SYNC_EMAIL = "email";
  public static final String DRC_SYNC_ROLE = "role";
  public static final String DRC_SYNC_LEVEL = "level";
  public static final String DRC_SYNC_TOKENS = "tokens";
  public static final String DRC_SYNC_ACCESS_TOKEN = "access_token";
  public static final String DRC_SYNC_REFRESH_TOKEN = "refresh_token";
  public static final String DRC_SYNC_TOKEN_TYPE = "token_type";
  public static final String DRC_SYNC_FILENAME = "filename";
  public static final String DRC_SYNC_PROJECT_ID = "project_id";
  public static final String DRC_SYNC_COLLECTION_ID = "collection_id";
  public static final String DRC_SYNC_AUTHORIZATION = "Authorization";
  public static final String DRC_SYNC_BEARER = "Bearer ";
  public static final String DRC_SYNC_PASSWORD = "password";
  public static final String DRC_SYNC_DOT_HTML = ".html";
  public static final String DRC_SYNC_FILE = "file";


  // for I18N
  public static final String EN_US = "enUS";
  public static final String REQUEST_LOCALE_PARAM = "lang";
  public static final String DEFAULT_I18N_LANG_CODE = "zh_tw";

  // 熱門主題與討論區計算的需要的時間參數，要從分鐘轉秒成的值
  public static final int MINUTE_TO_SECOND_MULTIPLY = 60;

  private Constants() {}
}
