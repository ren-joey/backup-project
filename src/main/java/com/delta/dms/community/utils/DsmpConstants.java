package com.delta.dms.community.utils;

public class DsmpConstants {

    public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    public static final String ATTACHMENT_FILENAME = "attachment; filename=ReadTopicReport.xlsx";

    public static final String SHEET_NAME_READ_TOPIC = "read topic";
    public static final String SHEET_NAME_CREATE_TOPIC = "create topic";
    public static final String SHEET_NAME_CREATE_REPLY = "create reply";
    public static final String SHEET_NAME_EMPLOYEE_LIST = "employee list";
    public static final String SHEET_NAME_TOPIC_LIST = "topic list";
    public static final String SHEET_NAME_REPLY_LIST = "reply list";

    public static final String QUERY_ACTIVITY_READ = "read";
    public static final String QUERY_ACTIVITY_CREATE = "create";
    public static final String QUERY_TYPE_ACTIVITY = "activity:";
    public static final String QUERY_OBJECT_TYPE_TOPIC = "objectType:topicId";
    public static final String QUERY_OBJECT_TYPE_REPLY = "objectType:replyId";

    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_ZONE_ID_UTC = "UTC";
    public static final int TIMEZONE_OFFSET_HOURS = 8;
    public static final int QUERY_LIMIT = 1000;

    public static final String[] TOPIC_COLUMNS = {"Topic Id", "Topic Name", "Operation", "User Id", "UserName", "BG", "BU", "Department", "Timestamp"};
    public static final String[] REPLY_COLUMNS = {"Topic Id", "Topic Name", "Reply Id", "Operation", "User Id", "UserName", "BG", "BU", "Department", "Timestamp"};
    public static final String[] EMPLOYEE_COLUMNS = {"UserName", "BG", "BU", "Department"};
    public static final String[] TOPIC_LIST_COLUMNS = {"Topic Id", "Topic Name"};
    public static final String[] REPLY_LIST_COLUMNS = {"Reply Id"};
    public static final String[] SHEET_ORDER = {
            DsmpConstants.SHEET_NAME_READ_TOPIC,
            DsmpConstants.SHEET_NAME_CREATE_TOPIC,
            DsmpConstants.SHEET_NAME_CREATE_REPLY,
            DsmpConstants.SHEET_NAME_EMPLOYEE_LIST,
            DsmpConstants.SHEET_NAME_TOPIC_LIST,
            DsmpConstants.SHEET_NAME_REPLY_LIST
    };

    public static final String OBJECT_ID_QUERY_TEMPLATE = "objectId:(%s)";
    public static final String OBJECT_ID_DELIMITER = " || ";
    public static final String TIME_RANGE_QUERY_TEMPLATE = "activityTime:[%s TO %s]";

}