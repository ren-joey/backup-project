package com.delta.dms.community.service;


import com.delta.datahive.searchobj.response.SearchResponse;
import com.delta.datahive.searchobj.type.activitylog.ActivityLogResult;
import com.delta.dms.community.adapter.ActivityLogAdapter;
import com.delta.dms.community.adapter.ExcelAdapter;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.config.DsmpConfig;
import com.delta.dms.community.dao.ForumDao;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CustomReplyInfoForExcel;
import com.delta.dms.community.dao.entity.CustomTopicInfoForExcel;
import com.delta.dms.community.model.DSMPExcel;
import com.delta.dms.community.swagger.model.BasicInfo;
import com.delta.dms.community.swagger.model.GroupUser;
import com.delta.dms.community.utils.DsmpConstants;

import com.delta.dms.community.utils.TimestampConverter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Transactional
public class ReportService {
    private ForumDao forumDao;
    private TopicDao topicDao;
    private ReplyDao replyDao;
    private final UserGroupAdapter userGroupAdapter;
    private final DsmpConfig dsmpConfig;
    private ExcelAdapter excelAdapter;
    private ActivityLogAdapter activityLogAdapter;


    public ReportService(
            ForumDao forumDao,
            TopicDao topicDao,
            ReplyDao replyDao,
            UserGroupAdapter userGroupAdapter,
            DsmpConfig dsmpConfig,
            ExcelAdapter excelAdapter,
            ActivityLogAdapter activityLogAdapter) {
        this.forumDao = forumDao;
        this.topicDao = topicDao;
        this.replyDao = replyDao;
        this.userGroupAdapter = userGroupAdapter;
        this.dsmpConfig = dsmpConfig;
        this.excelAdapter = excelAdapter;
        this.activityLogAdapter = activityLogAdapter;
    }

    public ResponseEntity<InputStreamResource> generateDSMPReportExcel(Long startTime, Long endTime) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = generateDSMPReportSheets(startTime, endTime);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            HttpHeaders headers = new HttpHeaders();
            headers.add(DsmpConstants.CONTENT_DISPOSITION_HEADER, DsmpConstants.ATTACHMENT_FILENAME);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(byteArrayInputStream));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ByteArrayOutputStream generateDSMPReportSheets(Long startTime, Long endTime) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // generate read topic sheet
        generateDSMPTopicSheet(
                workbook,
                DsmpConstants.SHEET_NAME_READ_TOPIC,
                DsmpConstants.QUERY_ACTIVITY_READ,
                DsmpConstants.QUERY_OBJECT_TYPE_TOPIC,
                startTime,
                endTime
        );

        // generate create topic sheet
        generateDSMPTopicSheet(
                workbook,
                DsmpConstants.SHEET_NAME_CREATE_TOPIC,
                DsmpConstants.QUERY_ACTIVITY_CREATE,
                DsmpConstants.QUERY_OBJECT_TYPE_TOPIC,
                startTime,
                endTime
        );

        // generate create reply sheet
        generateDSMPReplySheet(
                workbook,
                DsmpConstants.SHEET_NAME_CREATE_REPLY,
                DsmpConstants.QUERY_ACTIVITY_CREATE,
                DsmpConstants.QUERY_OBJECT_TYPE_REPLY,
                startTime,
                endTime
        );

        // adjust sheet order
        excelAdapter.setSheetOrder(workbook, DsmpConstants.SHEET_ORDER);

        // generate output stream
        return excelAdapter.generateExcel(workbook);
    }

    private void generateDSMPTopicSheet(
            Workbook workbook,
            String sheetName,
            String activityType,
            String objectType,
            Long startTime,
            Long endTime) {

        // get forumId from communityId
        List<Integer> forumIds = forumDao.getForumIdsByCommunityId(dsmpConfig.getCommunityId());

        // get topic Info from forumId
        List<CustomTopicInfoForExcel> topics = topicDao.getTopicsByForumIds(forumIds);

        // get topicId list
        List<String> topicIds = topics.stream()
                .map(topic -> String.valueOf(topic.getTopicId()))
                .collect(Collectors.toList());

        // map topicId -> topicName
        Map<String, String> topicMap = topics.stream()
                .collect(Collectors.toMap(
                        topic -> String.valueOf(topic.getTopicId()),
                        CustomTopicInfoForExcel::getTopicTitle
                ));

        // query activityLog
        SearchResponse<ActivityLogResult> response =
                activityLogAdapter.queryActivityLog(activityType, objectType, topicIds, startTime, endTime);

        // get uid list
        Set<String> userIdList = new HashSet<>();
        if (null != response && null != response.getResults()) {
            for (ActivityLogResult result : response.getResults()) {
                userIdList.add(result.getDocument().getUserId());
            }
        }

        // get user BasicInfo by user groups api
        List<GroupUser> users = userGroupAdapter.getUserByUids(new ArrayList<>(userIdList), new ArrayList<>());

        // create excel
        if (response != null && response.getResults() != null) {
            excelAdapter.createSheetWithActivityLog(
                    workbook,
                    sheetName,
                    DsmpConstants.TOPIC_COLUMNS,
                    response.getResults(),
                    result -> buildDSMPTopicSheet(result, topicMap, users)
            );
        }

        // if activity = read, generate employee and topic list
        if (activityType.equals(DsmpConstants.QUERY_ACTIVITY_READ)) {
            // generate employee list sheet
            generateDSMPEmployeeListSheet(workbook, users);
            // generate topic list sheet
            generateDSMPTopicListSheet(workbook, topics);
        }
    }

    private void generateDSMPReplySheet(
            Workbook workbook,
            String sheetName,
            String activityType,
            String objectType,
            Long startTime,
            Long endTime) {

        // get forumId from communityId
        List<Integer> forumIds = forumDao.getForumIdsByCommunityId(dsmpConfig.getCommunityId());

        // get topic Info from forumId
        List<CustomTopicInfoForExcel> topics = topicDao.getTopicsByForumIds(forumIds);

        // get topicId list
        List<String> topicIds = topics.stream()
                .map(topic -> String.valueOf(topic.getTopicId()))
                .collect(Collectors.toList());

        // map topicId -> topicName
        Map<String, String> topicMap = topics.stream()
                .collect(Collectors.toMap(
                        topic -> String.valueOf(topic.getTopicId()),
                        CustomTopicInfoForExcel::getTopicTitle
                ));

        List<CustomReplyInfoForExcel> replies = replyDao.getRepliesByTopicIds(topicIds);

        // map replyId -> topicId
        Map<String, String> replyToTopicMap = replies.stream()
                .collect(Collectors.toMap(
                        CustomReplyInfoForExcel::getReplyId,
                        CustomReplyInfoForExcel::getFollowTopicId
                ));

        // get replyId list
        List<String> replyIds = replies.stream()
                .map(CustomReplyInfoForExcel::getReplyId)
                .collect(Collectors.toList());

        // query activityLog
        SearchResponse<ActivityLogResult> response =
                activityLogAdapter.queryActivityLog(activityType, objectType, replyIds, startTime, endTime);

        // get uid list
        Set<String> userIdList = new HashSet<>();
        if (response != null && response.getResults() != null) {
            for (ActivityLogResult result : response.getResults()) {
                userIdList.add(result.getDocument().getUserId());
            }
        }

        // get user BasicInfo by user groups api
        List<GroupUser> users = userGroupAdapter.getUserByUids(new ArrayList<>(userIdList), new ArrayList<>());

        // create excel
        if (response != null && response.getResults() != null) {
            excelAdapter.createSheetWithActivityLog(
                    workbook,
                    sheetName,
                    DsmpConstants.REPLY_COLUMNS,
                    response.getResults(),
                    result -> buildDSMPReplySheet(result, replyToTopicMap, topicMap, users)
            );
        }

        // generate reply list sheet
        generateDSMPReplyListSheet(workbook, replies);
    }

    private Object[] buildDSMPTopicSheet(ActivityLogResult result, Map<String, String> topicMap, List<GroupUser> users) {
        DSMPExcel readTopic = new DSMPExcel();

        readTopic.setTopicId(result.getDocument().getObjectId());
        readTopic.setTopicName(topicMap.get(result.getDocument().getObjectId()));
        readTopic.setOperation(result.getDocument().getActivity());
        readTopic.setUserId(result.getDocument().getUserId());

        for (GroupUser user : users) {
            if (user.getUid().equals(readTopic.getUserId())) {
                BasicInfo basicInfo = user.getBasicInfo();
                readTopic.setUserName(basicInfo.getDisplayName());
                readTopic.setBg(basicInfo.getBg());
                readTopic.setBu(basicInfo.getBu());
                readTopic.setDepartment(basicInfo.getDepartment());
                break;
            }
        }

        Instant activityTimeInstant = result.getDocument().getActivityTime();
        LocalDateTime timestamp = TimestampConverter.convertToLocalDateTimeWithOffset(
                activityTimeInstant, DsmpConstants.TIMEZONE_OFFSET_HOURS);
        readTopic.setTimestamp(timestamp.format(DateTimeFormatter.ofPattern(DsmpConstants.TIME_FORMAT)));

        return new Object[]{
                readTopic.getTopicId(),
                readTopic.getTopicName(),
                readTopic.getOperation(),
                readTopic.getUserId(),
                readTopic.getUserName(),
                readTopic.getBg(),
                readTopic.getBu(),
                readTopic.getDepartment(),
                readTopic.getTimestamp()
        };
    }

    private Object[] buildDSMPReplySheet(
            ActivityLogResult result,
            Map<String, String> replyToTopicMap,
            Map<String, String> topicMap,
            List<GroupUser> users) {

        DSMPExcel readReply = new DSMPExcel();

        String replyId = result.getDocument().getObjectId();
        readReply.setReplyId(replyId);

        String topicId = replyToTopicMap.get(replyId);
        readReply.setTopicId(topicId);

        readReply.setTopicName(topicMap.get(topicId));
        readReply.setOperation(result.getDocument().getActivity());
        readReply.setUserId(result.getDocument().getUserId());

        for (GroupUser user : users) {
            if (user.getUid().equals(readReply.getUserId())) {
                BasicInfo basicInfo = user.getBasicInfo();
                readReply.setUserName(basicInfo.getDisplayName());
                readReply.setBg(basicInfo.getBg());
                readReply.setBu(basicInfo.getBu());
                readReply.setDepartment(basicInfo.getDepartment());
                break;
            }
        }

        Instant activityTimeInstant = result.getDocument().getActivityTime();
        LocalDateTime timestamp = TimestampConverter.convertToLocalDateTimeWithOffset(
                activityTimeInstant, DsmpConstants.TIMEZONE_OFFSET_HOURS);
        readReply.setTimestamp(timestamp.format(DateTimeFormatter.ofPattern(DsmpConstants.TIME_FORMAT)));

        return new Object[]{
                readReply.getTopicId(),
                readReply.getTopicName(),
                readReply.getReplyId(),
                readReply.getOperation(),
                readReply.getUserId(),
                readReply.getUserName(),
                readReply.getBg(),
                readReply.getBu(),
                readReply.getDepartment(),
                readReply.getTimestamp()
        };
    }


    private void generateDSMPEmployeeListSheet(Workbook workbook, List<GroupUser> users) {
        // create employee list sheet
        Sheet sheet = workbook.createSheet(DsmpConstants.SHEET_NAME_EMPLOYEE_LIST);
        excelAdapter.createHeaderRow(sheet, DsmpConstants.EMPLOYEE_COLUMNS);

        int rowNum = 1;
        for (GroupUser user : users) {
            Row row = sheet.createRow(rowNum++);
            BasicInfo basicInfo = user.getBasicInfo();
            row.createCell(0).setCellValue(basicInfo.getDisplayName());
            row.createCell(1).setCellValue(basicInfo.getBg());
            row.createCell(2).setCellValue(basicInfo.getBu());
            row.createCell(3).setCellValue(basicInfo.getDepartment());
        }
        // auto resize column length
        excelAdapter.autoSizeColumns(sheet, DsmpConstants.EMPLOYEE_COLUMNS.length);
    }

    private void generateDSMPTopicListSheet(Workbook workbook, List<CustomTopicInfoForExcel> topics) {
        // create topic list sheet
        Sheet sheet = workbook.createSheet(DsmpConstants.SHEET_NAME_TOPIC_LIST);
        excelAdapter.createHeaderRow(sheet, DsmpConstants.TOPIC_LIST_COLUMNS);

        int rowNum = 1;
        for (CustomTopicInfoForExcel topic : topics) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(topic.getTopicId());
            row.createCell(1).setCellValue(topic.getTopicTitle());
        }
        // auto resize column length
        excelAdapter.autoSizeColumns(sheet, DsmpConstants.TOPIC_LIST_COLUMNS.length);
    }

    private void generateDSMPReplyListSheet(Workbook workbook, List<CustomReplyInfoForExcel> replies) {
        // create reply list sheet
        Sheet sheet = workbook.createSheet(DsmpConstants.SHEET_NAME_REPLY_LIST);
        excelAdapter.createHeaderRow(sheet, DsmpConstants.REPLY_LIST_COLUMNS);

        int rowNum = 1;
        for (CustomReplyInfoForExcel reply : replies) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(reply.getReplyId());
        }
        // auto resize column length
        excelAdapter.autoSizeColumns(sheet, DsmpConstants.REPLY_LIST_COLUMNS.length);
    }

}