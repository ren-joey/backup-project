package com.delta.dms.community.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.event.EmailSendingEvent;
import com.delta.dms.community.swagger.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailService {
  private final CommunityService communityService;
  private final ForumService forumService;
  private final UserService userService;

  private static final String DATE_FOMAT = "yyyy-MM-dd kk:mm:ss";
  private static final String CURRENT_TIME = "currentTime";
  private static final String EMAIL_CONTEXT = "context";
  private static final String DATE = "date";
  private static final String STRING_UTILS = "StringUtils";

  private static final String NULL_EMAIL_ADDRESS = "null";

  private TemplateService templateService;
  private MailDao mailDao;
  private RichTextHandlerService richTextHandlerService;

  @Autowired
  public EmailService(
      UserService userService,
      CommunityService communityService,
      ForumService forumService,
      TemplateService templateService,
      MailDao mailDao,
      RichTextHandlerService richTextHandlerService) {
    this.userService = userService;
    this.communityService = communityService;
    this.forumService = forumService;
    this.templateService = templateService;
    this.mailDao = mailDao;
    this.richTextHandlerService = richTextHandlerService;
  }

  public void sendEmail(TemplateType templateType, EmailWithChineseAndEnglishContext context) {
    /*
    String recipient =
        context
            .getTo()
            .stream()
            .filter(Objects::nonNull)
            .filter(item -> !item.isEmpty())
            .filter(item -> !item.equals(NULL_EMAIL_ADDRESS))
            .collect(Collectors.joining(Constants.COMMA_DELIMITER));
     */
    String recipient = getRecipent(context);
    if (!recipient.isEmpty()) {
      String content = richTextHandlerService.replaceCopyedImageToBase64(context.getContent(), "");
      context.setContent(content);
      mailDao.insertMail(
          context.getCreator(),
          context.getSender(),
          recipient,
          context.getSubject(),
          getContentText(templateType, context),
          context.getPriority());
    }
  }

  private String getRecipent(EmailWithChineseAndEnglishContext context) {
    EmailType emailType = context.getType();
    List<String> userIdList = context.getTo();

    if(emailType.equals(EmailType.COMMUNITYJOINAPPLICATION)) {
      CommunityInfo communityInfo = CommunityInfo.class.cast(context.getCommunityInfo());
      userIdList = communityService.getAdminListOfCommunity(communityInfo.getCommunityId(),null, null, -1)
                      .stream()
                      .map(User::getMail)
                      .collect(Collectors.toList());
    } else if(emailType.equals(EmailType.COMMUNITYNOTIFICATION)) {
      if (context.getExtraMemberType().equals(EmailMemberType.ALLCOMMUNITYMEMBER)) {
        CommunityInfo communityInfo = CommunityInfo.class.cast(context.getCommunityInfo());
        List<String> allMemberIdList =
                communityService.getAdminListOfCommunity(
                        communityInfo.getCommunityId(),
                        null, null, -1)
                .stream()
                .map(User::getMail)
                .collect(Collectors.toList());
        userIdList = Stream.of(userIdList, allMemberIdList)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
      }
    } else if(emailType.equals(EmailType.COMMUNITYDELETION)) {
      if (context.getExtraMemberType().equals(EmailMemberType.ALLCOMMUNITYMEMBER)) {
        CommunityInfo communityInfo = CommunityInfo.class.cast(context.getCommunityInfo());
        userIdList = communityService.getAdminListOfCommunity(
                        communityInfo.getCommunityId(),
                        null, null, -1)
                .stream()
                .map(User::getMail)
                .collect(Collectors.toList());
      }
    } else if(emailType.equals(EmailType.FORUMNOTIFICATION)) {
      if (context.getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
        ForumInfo forumInfo = ForumInfo.class.cast(context.getForumInfo());
        List<String> allMemberIdList =
                forumService.getMemberOfForum(forumInfo.getForumId(), -1, -1)
                        .stream()
                        .map(User::getMail)
                        .collect(Collectors.toList());
        userIdList = Stream.of(userIdList, allMemberIdList)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
      }
    } else if(emailType.equals(EmailType.TOPICNOTIFICATION)) {
      if (context.getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
        ForumInfo forumInfo = ForumInfo.class.cast(context.getForumInfo());
        userIdList =
                forumService.getMemberOfForum(forumInfo.getForumId(), -1, -1)
                        .stream()
                        .map(User::getMail)
                        .collect(Collectors.toList());
      }
    } else if(emailType.equals(EmailType.FORUMDELETION)) {
      if (context.getExtraMemberType().equals(EmailMemberType.ALLFORUMMEMBER)) {
        ForumInfo forumInfo = ForumInfo.class.cast(context.getForumInfo());
        userIdList =
                forumService.getMemberOfForum(forumInfo.getForumId(), -1, -1)
                        .stream()
                        .map(User::getMail)
                        .collect(Collectors.toList());
      }
    }


    return userIdList
            .stream()
            .filter(Objects::nonNull)
            .filter(item -> !item.isEmpty())
            .filter(item -> !item.equals(NULL_EMAIL_ADDRESS))
            .collect(Collectors.joining(Constants.COMMA_DELIMITER));
  }

  private String getContentText(
      TemplateType templateType, EmailWithChineseAndEnglishContext context) {
    VelocityContext velocityContext = new VelocityContext();
    velocityContext.put(STRING_UTILS, StringUtils.class);
    velocityContext.put(EMAIL_CONTEXT, context);
    velocityContext.put(CURRENT_TIME, getCurrentDateTime());
    velocityContext.put(DATE, new DateTool());
    return templateService.getTemplate(templateType, velocityContext);
  }

  private String getCurrentDateTime() {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(DATE_FOMAT, Locale.TRADITIONAL_CHINESE);
    ZonedDateTime zdt =
        ZonedDateTime.now().toLocalDateTime().atZone(ZoneId.of(TimeZone.getDefault().getID()));
    return formatter.format(zdt);
  }
}
