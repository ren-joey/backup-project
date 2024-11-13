package com.delta.dms.community.service;

import com.delta.dms.community.adapter.AdapterUtil;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.enums.ExcelReplyReportHeaderRaw;
import com.delta.dms.community.enums.I18nEnum;
import com.delta.dms.community.model.ExcelHeaderDetail;
import com.delta.dms.community.swagger.model.Emoji;
import com.delta.dms.community.swagger.model.EmojiResult;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.utils.ExcelUtility;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.delta.dms.community.utils.DateUtility.convertToDateTime;
import static com.delta.dms.community.utils.URLConstants.*;

@Service
@Transactional
public class TopicReplyReportService {
    private TopicService topicService;
    private ReplyService replyService;
    private MessageSource messageSource;
    private AdapterUtil adapterUtil;
    private YamlConfig yamlConfig;
    private static final int MAX_LIKE_FETCH_LIMIT = 99999;
    private static final String FILE_NAME_DATE_FORMAT = "yyyyMMdd";
    private static final String TOPIC_REPLY_REPORT_CONTENT_DISPOSITION = "attachment;filename=%s_%s_%s.xlsx";

    public TopicReplyReportService(TopicService topicService, ReplyService replyService, MessageSource messageSource,
                                   AdapterUtil adapterUtil, YamlConfig yamlConfig) {
        this.topicService = topicService;
        this.replyService = replyService;
        this.messageSource = messageSource;
        this.adapterUtil = adapterUtil;
        this.yamlConfig = yamlConfig;
    }

    public ByteArrayOutputStream generateTopicReplyReport(TopicInfo topicInfo,
                                                          List<Map<String, String>> excelRowMapList,
                                                          String referrer)
            throws Exception {
        Workbook workbook = new XSSFWorkbook();
        List<ExcelHeaderDetail> excelHeaderDetails = new ArrayList<>();
        for (ExcelReplyReportHeaderRaw header : ExcelReplyReportHeaderRaw.values()) {
            excelHeaderDetails.add(
                    new ExcelHeaderDetail()
                            .setKey(header.getKey())
                            .setValue(header.getHeader(messageSource))
                            .setWidth(header.getWidth()));
        }
        String sheetName = messageSource.getMessage(String.valueOf(I18nEnum.REPLY_SERVICE_EXPORT_REPLY),
                null, LocaleContextHolder.getLocale());
        // 前面兩列分別是主題連結與贊數，所以從第三列開始寫入資料
        ExcelUtility.writeToExcel(workbook, sheetName, excelHeaderDetails, excelRowMapList, 2);
        Hyperlink topicLink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
        if (referrer != null) {
            topicLink.setAddress(referrer);
        } else {
            // 保底的fallback
            String topicURL = String.format(COMMUNITY_TOPIC_URL,
                    yamlConfig.getHost().replaceAll(URL_TRAILING_SLASH_REGEX, ""),
                    LocaleContextHolder.getLocale().toString()
                            .replace(BACKEND_LANG_SEPARATOR, FRONTEND_LANG_SEPARATOR).toLowerCase(),
                    topicInfo.getTopicId());
            topicLink.setAddress(topicURL);
        }
        String topicLinkName = messageSource.getMessage(String.valueOf(I18nEnum.REPLY_SERVICE_TOPIC_LINK),
                null, LocaleContextHolder.getLocale());
        String topicLikeNum = messageSource.getMessage(String.valueOf(I18nEnum.REPLY_SERVICE_TOPIC_LIKES),
                null, LocaleContextHolder.getLocale());
        // 第一列寫入主題連結資訊
        ExcelUtility.writeToExcelForHorizontal(workbook, sheetName, topicLinkName, topicInfo.getTopicTitle(), 0, topicLink);
        // TODO: 當按讚數量太多時可能超過這邊Hard-coded value的數字，不過目前系統應該是暫無此問題
        EmojiResult emojiResult = topicService.getEmojiDetailOfTopic(topicInfo.getTopicId(), Emoji.LIKE, 0, MAX_LIKE_FETCH_LIMIT,
                new Sort.Order(Sort.Direction.DESC, SortField.UPDATETIME.toString()));
        // 第二列寫入主題讚數
        ExcelUtility.writeToExcelForHorizontal(workbook, sheetName, topicLikeNum, emojiResult.getNumFound().toString(), 1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream;
    }

    public ResponseEntity<InputStreamResource> generateTopicRepliesReportEntity(Integer topicId, String referer)
        throws Exception {
        List<Map<String, String>> excelRowMapList = new ArrayList<>();
        TopicInfo topicInfo = topicService.getTopicInfoById(topicId);
        replyService.putAllTopicReplyReportRow(topicInfo, excelRowMapList);

        ByteArrayOutputStream outputStream = this.generateTopicReplyReport(
                topicInfo, excelRowMapList, referer);

        HttpHeaders headers = adapterUtil.generateHeader(MediaType.APPLICATION_OCTET_STREAM);
        DateTimeFormatter fileNameDateFormat = DateTimeFormatter.ofPattern(FILE_NAME_DATE_FORMAT);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                String.format(TOPIC_REPLY_REPORT_CONTENT_DISPOSITION,
                        java.net.URLEncoder.encode(topicInfo.getTopicTitle(),
                                StandardCharsets.UTF_8.toString())
                                .replace(URL_ENCODED_SIDE_EFFECTS_SYMBOL, URL_ENCODED_SPACE),
                        java.net.URLEncoder.encode(messageSource.getMessage(
                                String.valueOf(I18nEnum.REPLY_SERVICE_EXPORT_REPLY_TITLE),
                                        null,
                                        LocaleContextHolder.getLocale()),
                                StandardCharsets.UTF_8.toString())
                                .replace(URL_ENCODED_SIDE_EFFECTS_SYMBOL, URL_ENCODED_SPACE),
                        convertToDateTime(fileNameDateFormat, System.currentTimeMillis())
                ));
        return ResponseEntity.ok().headers(headers)
                .body(new InputStreamResource(
                        new ByteArrayInputStream(outputStream.toByteArray())
                ));
    }
}
