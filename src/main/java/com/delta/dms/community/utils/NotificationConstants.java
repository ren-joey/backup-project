package com.delta.dms.community.utils;

public class NotificationConstants {

  public static final String COMMUNITY_APPLICATION_URI_FORMAT = "%s/communityweb/%s/SystemManage";
  public static final String COMMUNITY_HOME_URI_FORMAT =
      "%s/communityweb/%s/Community/Home?communityId=%s";
  public static final String FORUM_HOME_URI_FORMAT =
      "%s/communityweb/%s/Community/Topic?forumId=%s";
  public static final String TOPIC_URI_FORMAT = "%s/communityweb/%s/Community/Reply?topicId=%s";

  public static final String COMMUNITY_CREATION_APPLICATION_TW_FORMAT = "<b>%s</b>申請建立<b>%s</b>。";
  public static final String COMMUNITY_CREATION_APPLICATION_CN_FORMAT = "<b>%s</b>申请建立<b>%s</b>。";
  public static final String COMMUNITY_CREATION_APPLICATION_EN_FORMAT =
      "<b>%s</b> asks to create <b>%s</b>.";
  public static final String COMMUNITY_CREATION_APPLICATION_APPROVAL_TW_FORMAT =
      "您申請的<b>%s</b>已成功建立！";
  public static final String COMMUNITY_CREATION_APPLICATION_APPROVAL_CN_FORMAT =
      "您申请的<b>%s</b>已成功建立！";
  public static final String COMMUNITY_CREATION_APPLICATION_APPROVAL_EN_FORMAT =
      "Your community <b>%s</b> has been built successfully!";
  public static final String COMMUNITY_CREATION_APPLICATION_REJECTION_TW_FORMAT =
      "您建立<b>%s</b>的申請已被拒絕。";
  public static final String COMMUNITY_CREATION_APPLICATION_REJECTION_CN_FORMAT =
      "您建立<b>%s</b>的申请已被拒绝。";
  public static final String COMMUNITY_CREATION_APPLICATION_REJECTION_EN_FORMAT =
      "Your application to create community <b>%s</b> has been rejected.";
  public static final String COMMUNITY_DELETION_TW_FORMAT = "<b>%s</b>已被<b>%s</b>刪除，您將無法再存取社群的內容。";
  public static final String COMMUNITY_DELETION_CN_FORMAT = "<b>%s</b>已被<b>%s</b>删除，您将无法再存取社群的内容。";
  public static final String COMMUNITY_DELETION_EN_FORMAT =
      "<b>%s</b> has been deleted by <b>%s</b>. You can no longer access any content of this community.";
  public static final String COMMUNITY_DELETION_APPLICATION_TW_FORMAT = "<b>%s</b>申請刪除<b>%s</b>。";
  public static final String COMMUNITY_DELETION_APPLICATION_CN_FORMAT = "<b>%s</b>申请删除<b>%s</b>。";
  public static final String COMMUNITY_DELETION_APPLICATION_EN_FORMAT =
      "<b>%s</b> asks to delete <b>%s</b>.";
  public static final String COMMUNITY_DELETION_APPLICATION_APPROVAL_TW_FORMAT =
      "<b>%s</b>已被刪除，您將無法再存取社群的內容。";
  public static final String COMMUNITY_DELETION_APPLICATION_APPROVAL_CN_FORMAT =
      "<b>%s</b>已被删除，您将无法再存取社群的内容。";
  public static final String COMMUNITY_DELETION_APPLICATION_APPROVAL_EN_FORMAT =
      "<b>%s</b> has been deleted. You can no longer access any content of this community.";
  public static final String COMMUNITY_DELETION_APPLICATION_REJECTION_TW_FORMAT =
      "您刪除<b>%s</b>的申請已被拒絕。";
  public static final String COMMUNITY_DELETION_APPLICATION_REJECTION_CN_FORMAT =
      "您删除<b>%s</b>的申请已被拒绝。";
  public static final String COMMUNITY_DELETION_APPLICATION_REJECTION_EN_FORMAT =
      "Your application to delete community <b>%s</b> has been rejected.";
  public static final String COMMUNITY_JOIN_TW_FORMAT = "您已被加入<b>%s</b>。您將同時被加入此社群的所有公共討論區。";
  public static final String COMMUNITY_JOIN_CN_FORMAT = "您已被加入<b>%s</b>。您将同时被加入此社群的所有公共讨论区。";
  public static final String COMMUNITY_JOIN_EN_FORMAT =
      "You've been added to <b>%s</b>. And you are the member of all public forums in this community now.";
  public static final String COMMUNITY_REMOVE_TW_FORMAT = "您已從<b>%s</b>中被移除。您將同時從此社群的所有公共討論區中被移除。";
  public static final String COMMUNITY_REMOVE_CN_FORMAT = "您已从<b>%s</b>中被移除。您将同时从此社群的所有公共讨论区中被移除。";
  public static final String COMMUNITY_REMOVE_EN_FORMAT =
      "You've been removed from <b>%s</b>. And you are no longer the member of any public forum in this community.";

  public static final String FORUM_DELETION_TW_FORMAT = "<b>%s</b>已被<b>%s</b>刪除，您將無法再存取此討論區的內容。";
  public static final String FORUM_DELETION_CN_FORMAT = "<b>%s</b>已被<b>%s</b>删除，您将无法再存取此讨论区的内容。";
  public static final String FORUM_DELETION_EN_FORMAT =
      "<b>%s</b> has been deleted by <b>%s</b>. You can no longer access any content of this forum.";
  public static final String FORUM_JOIN_TW_FORMAT = "您已被加入<b>%s</b>。";
  public static final String FORUM_JOIN_EN_FORMAT = "You’ve been added to <b>%s</b>.";
  public static final String FORUM_REMOVE_TW_FORMAT = "您已從<b>%s</b>中被移除。";
  public static final String FORUM_REMOVE_CN_FORMAT = "您已从<b>%s</b>中被移除。";
  public static final String FORUM_REMOVE_EN_FORMAT = "You've been removed from <b>%s</b>.";

  public static final String TOPIC_CONCLUSION_MADE_TW_FORMAT =
      "<b>%s</b>已為您在<b>%s</b>內<b>%s</b>的討論作結論。";
  public static final String TOPIC_CONCLUSION_MADE_CN_FORMAT =
      "<b>%s</b>已为您在<b>%s</b>内<b>%s</b>的讨论作结论。";
  public static final String TOPIC_CONCLUSION_MADE_EN_FORMAT =
      "<b>%s</b> has made a conclusion for your topic <b>%s</b> in <b>%s</b>.";
  public static final String TOPIC_NOTIFICATION_TW_FORMAT = "<b>%s</b>邀請您加入<b>%s</b>內<b>%s</b>的討論。";
  public static final String TOPIC_NOTIFICATION_CN_FORMAT = "<b>%s</b>邀请您加入<b>%s</b>内<b>%s</b>的讨论。";
  public static final String TOPIC_NOTIFICATION_EN_FORMAT =
      "<b>%s</b> invites you to join the discussion of <b>%s</b> in <b>%s</b>.";

  public static final String JOIN_APPLICATION_TW_FORMAT = "<b>%s</b>申請加入<b>%s</b>。";
  public static final String JOIN_APPLICATION_CN_FORMAT = "<b>%s</b>申请加入<b>%s</b>。";
  public static final String JOIN_APPLICATION_EN_FORMAT = "<b>%s</b> requests to join <b>%s</b>.";
  public static final String JOIN_APPLICATION_APPROVAL_TW_FORMAT = "您加入<b>%s</b>的申請已被核准。";
  public static final String JOIN_APPLICATION_APPROVAL_CN_FORMAT = "您加入<b>%s</b>的申请已被核准";
  public static final String JOIN_APPLICATION_APPROVAL_EN_FORMAT =
      "Your request of joining <b>%s</b> has been approved.";
  public static final String JOIN_APPLICATION_REJECTION_TW_FORMAT = "您加入<b>%s</b>的申請已被拒絕。";
  public static final String JOIN_APPLICATION_REJECTION_CN_FORMAT = "您加入<b>%s</b>的申请已被拒绝。";
  public static final String JOIN_APPLICATION_REJECTION_EN_FORMAT =
      "Your request of joining <b>%s</b> has been rejected.";
  public static final String NOTIFICATION_TW_FORMAT = "<b>%s</b>從<b>%s</b>發送了通知給您：";
  public static final String NOTIFICATION_CN_FORMAT = "<b>%s</b>从<b>%s</b>发送了通知给您：";
  public static final String NOTIFICATION_EN_FORMAT =
      "<b>%s</b> sends you a notification from <b>%s</b>:";

  public static final int PRIORITY_5 = 5;
  public static final String LANG_TW = "zh-tw";
  public static final String LANG_CN = "zh-cn";
  public static final String LANG_EN = "en-us";

  private NotificationConstants() {}
}
