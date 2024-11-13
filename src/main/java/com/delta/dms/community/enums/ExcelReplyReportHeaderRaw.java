package com.delta.dms.community.enums;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public enum ExcelReplyReportHeaderRaw {
    REPLY_FLOOR("replyFloor", 10),
    REPLY_SUB_FLOOR("replySubFloor", 10),
    COMMENT_AUTHOR("commentAuthor", 30),
    LIKE_COUNT("likeCount", 10),
    EMPLOYEE_EMAIL("employeeEmail", 10),
    DEPARTMENT("department", 10),
    OFFICE("office", 10),
    COMMENT_TEXT("commentText", 20),
    CREATE_TIME("createTime", 20),
    MODIFIED_TIME("modifiedTime", 20);

    private String key;
    private int width;

    ExcelReplyReportHeaderRaw(String key, int width) {
        this.key = key;
        this.width = width;
    }

    public String toString() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getHeader(MessageSource messageSource) {
        return messageSource.getMessage(String.format("ExcelReplyReportHeaderRaw.%s", key),
                null, LocaleContextHolder.getLocale());
    }

    public int getWidth() {
        return width;
    }

    public static ExcelReplyReportHeaderRaw fromKey(String key) {
        for (ExcelReplyReportHeaderRaw b : ExcelReplyReportHeaderRaw.values()) {
            if (b.key.equals(key)) {
                return b;
            }
        }
        return null;
    }
}
