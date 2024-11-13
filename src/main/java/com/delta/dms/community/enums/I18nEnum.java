package com.delta.dms.community.enums;

public enum I18nEnum {
    REPLY_SERVICE_EXPORT_REPLY("replyService.export.reply"),
    REPLY_SERVICE_EXPORT_REPLY_TITLE("replyService.export.reply.title"),
    REPLY_SERVICE_TOPIC_LINK("replyService.topic.link"),
    REPLY_SERVICE_TOPIC_LIKES("replyService.topic.likes"),

    EXCEL_REPLY_REPORT_HEADER_RAW_REPLY_FLOOR("ExcelReplyReportHeaderRaw.replyFloor"),
    EXCEL_REPLY_REPORT_HEADER_RAW_REPLY_SUB_FLOOR("ExcelReplyReportHeaderRaw.replySubFloor"),
    EXCEL_REPLY_REPORT_HEADER_RAW_COMMENT_AUTHOR("ExcelReplyReportHeaderRaw.commentAuthor"),
    EXCEL_REPLY_REPORT_HEADER_RAW_LIKE_COUNT("ExcelReplyReportHeaderRaw.likeCount"),
    EXCEL_REPLY_REPORT_HEADER_RAW_EMPLOYEE_EMAIL("ExcelReplyReportHeaderRaw.employeeEmail"),
    EXCEL_REPLY_REPORT_HEADER_RAW_DEPARTMENT("ExcelReplyReportHeaderRaw.department"),
    EXCEL_REPLY_REPORT_HEADER_RAW_OFFICE("ExcelReplyReportHeaderRaw.office"),
    EXCEL_REPLY_REPORT_HEADER_RAW_COMMENT_TEXT("ExcelReplyReportHeaderRaw.commentText"),
    EXCEL_REPLY_REPORT_HEADER_RAW_createTime("ExcelReplyReportHeaderRaw.createTime"),
    EXCEL_REPLY_REPORT_HEADER_RAW_modifiedTime("ExcelReplyReportHeaderRaw.modifiedTime"),

    EXCEL_REPLY_REPORT_CONCLUSION("ExcelReplyReport.conclusion"),
    EXCEL_REPLY_REPORT_ATTACHMENT("ExcelReplyReport.attachment"),
    EXCEL_REPLY_REPORT_TABLE("ExcelReplyReport.table"),
    QUESTION_CONCLUSION_PROBLEM_DESCRIPTION("QuestionConclusion.ProblemDescription"),
    QUESTION_CONCLUSION_SOLUTION_DESCRIPTION("QuestionConclusion.SolutionDescription"),
    QUESTION_CONCLUSION_ADDITIONAL_INFORMATION("QuestionConclusion.AdditionalInformation");


    private String value;

    I18nEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static I18nEnum fromValue(String text) {
        for (I18nEnum b : I18nEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + text + "'");
    }
}
