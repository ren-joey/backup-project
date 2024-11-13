package com.delta.dms.community.utils;

public class EmailConstants {

  public static final String DMS_COMMUNITY_EMAIL = "[DMS] ";
  public static final String TITLE_FORMAT_COMMUNITY = "Community 社群: %s";
  public static final String TITLE_FORMAT_FORUM = "Forum 討論區: %s";
  public static final String TITLE_FORMAT_TOPIC = "Topic 主題: %s";
  public static final String COMMUNITY_DEFAULT_URI_FORMAT = "%s/communityweb";
  public static final String COMMUNITY_HOME_URI_FORMAT =
      "%s/communityweb/%s/Community/Home?communityId=%s";
  public static final String COMMUNITY_REVIEW_URI_FORMAT = "%s/communityweb/zh-tw/SystemManage";
  public static final String COMMUNITY_JOIN_APPLICATION_CHINESE_FORMAT = "%s 申請加入社群";
  public static final String COMMUNITY_JOIN_APPLICATION_ENGLISH_FORMAT =
      "%s requests to join the community.";
  public static final String APPROVE_JOIN_COMMUNITY_APPLICATION_CHINESE_FORMAT = "您加入社群的申請已被核准";
  public static final String APPROVE_JOIN_COMMUNITY_APPLICATION_ENGLISH_FORMAT =
      "Your request to join the community has been approved.";
  public static final String REJECT_JOIN_COMMUNITY_APPLICATION_CHINESE_FORMAT = "您加入社群的申請已被拒絕";
  public static final String REJECT_JOIN_COMMUNITY_APPLICATION_ENGLISH_FORMAT =
      "Your request to join the community has been rejected.";
  public static final String APPROVE_CREATE_APPLICATION_CHINESE_FORMAT = "您申請的社群已成功建立！";
  public static final String APPROVE_CREATE_APPLICATION_ENGLISH_FORMAT =
      "Your community has been created successfully!";
  public static final String REJECT_CREATE_APPLICATION_CHINESE_FORMAT = "您建立社群的申請已被拒絕";
  public static final String REJECT_CREATE_APPLICATION_ENGLISH_FORMAT =
      "Your application to create the community has been rejected.";
  public static final String REJECT_CREATE_APPLICATION_CONTENT_FORMAT = "Reason 原因: %s";
  public static final String APPLICATION_CHINESE_CONTENT_FORMAT = "原因：%s";
  public static final String APPLICATION_ENGLISH_CONTENT_FORMAT = "Reason：%s";
  public static final String REMOVE_FROM_COMMUNITY_CHINESE_FORMAT =
      "您已從 %s 中被移出。您將同時從此社群的所有公共討論區中被移除。";
  public static final String REMOVE_FROM_COMMUNITY_ENGLISH_FORMAT =
      "You've been removed from %s. And you are no longer the member of any public forum in this community.";
  public static final String COMMUNITY_NOTIFICATION_CHINESE_FORMAT = "%s 從社群發送了通知給您";
  public static final String COMMUNITY_NOTIFICATION_ENGLISH_FORMAT =
      "%s sent you a notification from the community.";
  public static final String COMMUNITY_JOIN_APPLICATION_ENGLISH_CONTENT_FORMAT =
      "Please go to %s to process the request.";
  public static final String COMMUNITY_DELETE_APPLICATION_CHINESE_FORMAT = "%s 申請刪除社群";
  public static final String COMMUNITY_DELETE_APPLICATION_ENGLISH_FORMAT =
      "%s requests to delete the community.";
  public static final String COMMUNITY_DELETE_CHINESE_FORMAT = "%s 已被 %s 刪除，您將無法再存取此社群的內容。";
  public static final String COMMUNITY_DELETE_ENGLISH_FORMAT =
      "%s has been deleted by %s. You can no longer access any content of this community.";
  public static final String COMMUNITY_DELETE_REJECTED_CHINESE_FORMAT = "您刪除社群的申請已被拒絕";
  public static final String COMMUNITY_DELETE_REJECTED_ENGLISH_FORMAT =
      "Your application to delete the community has been rejected.";

  public static final String APPROVE_JOIN_COMMUNITY_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Join Request Approved";
  public static final String REJECT_JOIN_COMMUNITY_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Join Request Rejected";
  public static final String APPROVE_CREATE_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Created";
  public static final String REJECT_CREATE_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Not Created";
  public static final String LEAVE_SUBJECT = DMS_COMMUNITY_EMAIL + "Member Withdrawal Notification";
  public static final String REMOVE_FROM_COMMUNITY_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Membership Cancellation Notification";
  public static final String JOIN_COMMUNITY_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Join Notification";
  public static final String DELETE_COMMUNITY_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Request to delete the community";
  public static final String DELETE_COMMUNITY_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Deletion Notification";
  public static final String DELETE_COMMUNITY_REJECTED_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Community Not Deleted";
  public static final String JOIN_COMMUNITY_APPLICATION_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + "Request to join the community";

  public static final String FORUM_HOME_URI_FORMAT =
      "%s/communityweb/%s/Community/Topic?forumId=%s";
  public static final String FORUM_JOIN_APPLICATION_CHINESE_FORMAT = "%s 申請加入討論區";
  public static final String FORUM_JOIN_APPLICATION_ENGLISH_FORMAT =
      "%s requests to join the forum.";
  public static final String FORUM_REMOVE_FORUM_CHINESE_FORMAT = "您已從 %s 中被移除";
  public static final String FORUM_REMOVE_FORUM_ENGLISH_FORMAT = "You've been removed from %s";
  public static final String FORUM_DELETE_FORUM_CHINESE_FORMAT = "%s 已被  %s 刪除，您將無法再存取此討論區的內容。 ";
  public static final String FORUM_DELETE_FORUM_ENGLISH_FORMAT =
      "%s has been deleted by %s. You can no longer access any content of this forum.";
  public static final String APPROVE_JOIN_APPLICATION_CHINESE_FORMAT = "您加入討論區的申請已被核准";
  public static final String APPROVE_JOIN_APPLICATION_ENGLISH_FORMAT =
      "Your request to join the forum has been approved.";
  public static final String REJECT_JOIN_APPLICATION_CHINESE_FORMAT = "您加入討論區的申請已被拒絕";
  public static final String REJECT_JOIN_APPLICATION_ENGLISH_FORMAT =
      "Your request to join the forum has been rejected.";
  public static final String FORUM_NOTIFICATION_CHINESE_FORMAT = "%s 從討論區發送了通知給您";
  public static final String FORUM_NOTIFICATION_ENGLISH_FORMAT =
      "%s sent you a notification from the forum.";
  public static final String FORUM_JOIN_APPLICATION_ENGLISH_CONTENT_FORMAT =
      "<br/>Go to %s to process the request";

  public static final String JOIN_JOIN_FORUM_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + "Community Join Notification";
  public static final String JOIN_REMOVE_FORUM_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + "Membership Cancellation Notification";
  public static final String JOIN_REMOVE_DELETE_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + "Community Deletion Notification";
  public static final String JOIN_FORUM_APPLICATION_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + "Request to join the forum";
  public static final String APPROVE_JOIN_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Join Request Approved";
  public static final String REJECT_JOIN_APPLICATION_SUBJECT =
      DMS_COMMUNITY_EMAIL + "Join Request Rejected";

  public static final String TOPIC_URI_FORMAT = "%s/communityweb/%s/Community/Reply?topicId=%s";
  public static final String TOPIC_NOTIFICATION_CHINESE_FORMAT = "%s 邀請您加入主題討論";
  public static final String TOPIC_NOTIFICATION_ENGLISH_FORMAT =
      "%s invites you to join the discussion.";

  public static final String REPLY_NOTIFICATION_CHINESE_FORMAT = "%s 通知您查看討論回覆";
  public static final String REPLY_NOTIFICATION_ENGLISH_FORMAT =
      "%s invites you to check out the reply.";

  public static final String CONCLUSION_NOTIFICATION_CHINESE_FORMAT = "%s 通知您查看討論結論";
  public static final String CONCLUSION_NOTIFICATION_ENGLISH_FORMAT =
      "%s invites you to check out the conclusion.";

  public static final String CONCLUSION_CHINESE_FORMAT = "%s 已為您的討論作結論";
  public static final String CONCLUSION_ENGLISH_FORMAT = "%s has made a conclusion for your topic.";

  public static final String PQM_FACTORY_AREA = "廠區";
  public static final String PQM_PRODUCING_ZONE = "生產區域";
  public static final String PQM_TRIGGERED_DATE = "觸發日期";
  public static final String PQM_PRODUCTION_LINE = "線別";
  public static final String PQM_WORK_STATION = "站別";
  public static final String PQM_EQUIPMENT_MODEL = "設備型號";
  public static final String PQM_CURRENT_STATE = "當前狀態";
  public static final String PQM_MODEL = "機種資訊";
  public static final String PQM_DIMENSION = "零件尺寸";
  public static final String PQM_DATA = "機台數據";
  public static final String PQM_ERROR_CODE = "異常代碼(Error Code)";
  public static final String PQM_CLASSIFICATION = "現象分類";
  public static final String PQM_ABNORMALCY_DESCRIPTION = "異常描述";
  public static final String PQM_ABNORMALCY_COUNT = "異常次數/監測天數";
  public static final String PQM_ABNORMALCY_TIME = "異常時間";
  public static final String PQM_DESCRIPTION = "現場現象簡述";
  public static final String COMMUNITY_NAME_USED =
      "此社群名稱已被使用，請以其他名稱重新申請。<br/>This community name has been used. Please re-apply with another name.<br/>";

  public static final String EERPP_REVIEW_REPORT_DESC = "Dear PIT,";
  public static final String EERPP_REVIEW_REPORT_NOTIFICATION_ENGLISH_FORMAT =
      "[%s] needs You to Review";
  public static final String EERPP_REVIEW_REPORT_NOTIFICATION_CHINESE_FORMAT = "[%s] 需要您的審閱";
  public static final String EERPP_REVIEW_REPORT_SUBJECT_FORMAT =
      DMS_COMMUNITY_EMAIL + EERPP_REVIEW_REPORT_NOTIFICATION_ENGLISH_FORMAT;
  public static final String EERPP_REVIEW_REPORT_URI_FORMAT =
      "%s/mydmsweb/%s/OrgMainPage/%s/Document?folderId=%d";

  public static final int HIGHEST_PRIORITY_MAIL = 1;
  public static final int HIGH_PRIORITY_MAIL = 2;
  public static final int MIDDLE_PRIORITY_MAIL = 3;
  public static final int LOW_PRIORITY_MAIL = 4;
  public static final int LOWEST_PRIORITY_MAIL = 5;

  public static final String MAIL_TITLE = "title";
  public static final String MAIL_SUB_TITLE = "subTitle";

  public static final String MAIL_ERROR_CODE = "errorCode";
  public static final String MAIL_HISTORIES = "histories";

  private EmailConstants() {}
}
