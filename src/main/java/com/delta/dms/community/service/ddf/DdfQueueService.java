package com.delta.dms.community.service.ddf;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.delta.dms.community.dao.entity.*;
import com.delta.dms.community.enums.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.ContentSection;
import com.delta.datahive.api.DDF.DDF;
import com.delta.datahive.api.DDF.DDFDoc;
import com.delta.datahive.api.DDF.PrivilegeSection;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.Reference;
import com.delta.datahive.api.DDF.Reference.RefType;
import com.delta.datahive.api.DDF.ServiceType;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.datahive.structures.Tuple;
import com.delta.datahive.types.DDFStatus;
import com.delta.datahive.types.SystemTag;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.DdfDao;
import com.delta.dms.community.dao.ReplyDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.DataHiveStoreService;
import com.delta.dms.community.service.FileService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.TemplateService;
import com.delta.dms.community.service.TopicService;
import com.delta.dms.community.service.UserService;
import com.delta.dms.community.swagger.model.AllInformation;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.DdfRole;
import com.delta.dms.community.swagger.model.DdfType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.ReplyDetail;
import com.delta.dms.community.swagger.model.Tag;
import com.delta.dms.community.swagger.model.TemplateType;
import com.delta.dms.community.swagger.model.TopicState;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DdfQueueService {

  private static final LogUtil log = LogUtil.getInstance();
  private static final String COMMUNITY_CONTEXT_ROOT = "communityweb/zh-tw/Community/";
  private static final String TOPIC_REFURL_FORMAT = COMMUNITY_CONTEXT_ROOT + "Reply?topicId=%s";
  private static final String FORUM_REFURL_FORMAT = COMMUNITY_CONTEXT_ROOT + "Topic?forumId=%s";
  private static final String COMMUNITY_REFURL_FORMAT =
      COMMUNITY_CONTEXT_ROOT + "Home?communityId=%s";

  private static final String SPACE_SEPARATOR = " ";
  private static final String INVALID_TAG_REGEX =
      com.delta.datahive.types.Constants.INVALID_TAG_REGEX.replace(".*", StringUtils.EMPTY);
  private static final int CONCLUSION_INDEX = 0;
  private static final String CONTENT_REPLY_FORMAT = "%s %s...";
  private static final String DATAHIVE_URL_PREFIX = "/DH_STORE/DMS_Image/";
  private static final String DATAHIVE_URL_WITH_UUID =
      DATAHIVE_URL_PREFIX + "\\S{8}-\\S{4}-\\S{4}-\\S{4}-\\S{12}";
  public static final int ONLYDDF_HISTORY_COUNT = 1;

  private static final String TEMPLATE_AUTHOR = "author";
  private static final String TEMPLATE_TAG = "tag";
  private static final String TEMPLATE_REPLYLIST = "replyList";
  private static final String TEMPLATE_USERMAP = "userMap";

  private static final String TOPIC_SHOW_STATE_PREFIX = "showState-";

  private DataHiveStoreService dataHiveStoreService;
  private CommunityService communityService;
  private ForumService forumService;
  private TopicService topicService;
  private UserService userService;
  private TemplateService templateService;
  private FileService fileService;
  private DdfDao ddfDao;
  private TopicDao topicDao;
  private ReplyDao replyDao;
  private YamlConfig yamlConfig;

  @Autowired
  public DdfQueueService(
      DataHiveStoreService dataHiveStoreService,
      CommunityService communityService,
      ForumService forumService,
      TopicService topicService,
      UserService userService,
      TemplateService templateService,
      FileService fileService,
      DdfDao ddfDao,
      TopicDao topicDao,
      ReplyDao replyDao,
      YamlConfig yamlConfig) {
    this.dataHiveStoreService = dataHiveStoreService;
    this.communityService = communityService;
    this.forumService = forumService;
    this.topicService = topicService;
    this.userService = userService;
    this.templateService = templateService;
    this.fileService = fileService;
    this.ddfDao = ddfDao;
    this.topicDao = topicDao;
    this.replyDao = replyDao;
    this.yamlConfig = yamlConfig;
  }

  public void upsertDdfs() {
    List<DdfQueue> ddfQueue = ddfDao.getDdfQueueByStatus(DdfQueueStatus.WAIT.getValue());
    ddfQueue
        .parallelStream()
        .collect(groupingBy(DdfQueue::getType, mapping(DdfQueue::getId, toList())))
        .entrySet()
        .forEach(
            entry ->
                ddfDao.updateDdfQueueStatus(
                    entry.getKey(), entry.getValue(), DdfQueueStatus.PROCESSING.getValue()));
    ddfQueue.forEach(
        item -> {
          try {
            upsertDdfByTypeAndId(DdfType.fromValue(item.getType()), item.getId());
            ddfDao.deleteDdfQueue(item.getType(), item.getId());
          } catch (NoSuchElementException e) {
            log.error(e);
            ddfDao.upsertDdfQueue(
                item.getType(), item.getId(), DdfQueueStatus.PASS.getValue(), Optional.ofNullable(e.getMessage()).orElse(e.toString()));
          } catch (Exception e) {
            log.error(e);
            ddfDao.upsertDdfQueue(
                item.getType(), item.getId(), DdfQueueStatus.WAIT.getValue(), Optional.ofNullable(e.getMessage()).orElse(e.toString()));
          }
        });
  }

  public void deleteDdfs() {
    List<String> ddfQueue = ddfDao.getDdfDeleteQueueByStatus(DdfQueueStatus.WAIT.getValue());
    ofNullable(ddfQueue)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(
            ids -> {
              ddfDao.updateDdfDeleteQueueStatus(ids, DdfQueueStatus.PROCESSING.getValue());
              ids.forEach(
                  id -> {
                    try {
                      log.debug("Delete " + id);
                      deleteDdf(id);
                      ddfDao.deleteDdfDeleteQueue(id);
                    } catch (NoSuchElementException e) {
                      log.warn(e);
                      ddfDao.deleteDdfDeleteQueue(id);
                    } catch (Exception e) {
                      log.error(e);
                      ddfDao.upsertDdfDeleteQueue(
                          id, DdfQueueStatus.WAIT.getValue(), e.getMessage());
                    }
                  });
            });
  }

  private void upsertDdfByTypeAndId(DdfType type, int id) {
    log.debug("Generate the ddf of the " + type.toString());
    AllInformation information = ddfDao.getAllInformation(type.toString(), id);
    if (information == null || isDelete(type, information)) {
      ddfDao.deleteDdfQueue(type.toString(), id);
      return;
    }
    DDF ddf = null;
    // set english name of attached community
    if (StringUtils.isNotBlank(information.getCommunityGroupId())) {
      String communityEname =
          communityService
              .getAttachedCommunityNames(
                  singletonMap(information.getCommunityGroupId(), information.getCommunityName()),
                  Locale.US.toLanguageTag())
              .getOrDefault(information.getCommunityGroupId(), information.getCommunityName());
      information.setCommunityEname(communityEname);
    } else {
      information.setCommunityEname(information.getCommunityName());
    }
    if (DdfType.COMMUNITY.equals(type)) {
      ddf = setCommunityDdf(information);
    } else if (DdfType.FORUM.equals(type)) {
      ddf = setForumDdf(information);
    } else {
      List<String> forumMemberRoleList = forumService.getRoleListOfMainGroupForum(information.getForumId(), false);
      List<String> forumAdminRoleList = forumService.getRoleListOfMainGroupForum(information.getForumId(), true);
      Map<String, Set<PrivilegeType>> topicPrivilegeMap =
          fileService.getPrivilege(
              information.getTopicCreateUserId(), forumAdminRoleList, forumMemberRoleList);
      Map<String, List<UserGroupEntity>> attachmentRoleMap =
          getRoleMap(null, information.getTopicLastModifiedUserId(), new ArrayList<>());
      log.debug("Update attachments and image in the topic rich text");
      List<AttachmentInfo> attachmentList =
          topicDao
              .getTopicAttachments(information.getTopicId())
              .stream()
              .distinct()
              .collect(Collectors.toList());
      updateTopicAttachmentAndRichTextImage(
          information, attachmentList, attachmentRoleMap, topicPrivilegeMap,
          information.getCommunityGroupId());
      log.debug("Update attachments and image in the reply rich text");
      List<ReplyDetail> replyList = replyDao.getReplyList(information.getTopicId());
      replyList.forEach(
          item ->
              updateReplyAttachmentAndRichTextImage(
                  information, item, forumAdminRoleList, forumMemberRoleList, information.getCommunityGroupId()));
      replyList = getJsoupParseText(replyList);
      replyList.forEach(
          item ->
              item.setNestedReplyList(
                  getJsoupParseText(replyDao.getNestedReplyList(item.getReplyId()))));
      String topicText = Jsoup.parse(information.getTopicText()).text();
      information.setTopicText(topicText);
      Map<String, List<UserGroupEntity>> topicRoleMap =
          getRoleMap(
              information.getTopicCreateUserId(),
              information.getTopicLastModifiedUserId(),
              new ArrayList<>());
      ddf = setTopicDdf(information, replyList, attachmentList, topicRoleMap, topicPrivilegeMap);
    }
    if (!information.getCommunityGroupId().isEmpty()) {
      ddf.getBaseSection().setOrg(new UserGroupEntity(information.getCommunityGroupId()));
    }

    upsertDdf(id, type, ddf);
  }

  private boolean isDelete(DdfType type, AllInformation information) {
    boolean deleteStatus = true;
    if (DdfType.COMMUNITY.equals(type)) {
      deleteStatus =
          CommunityStatus.DELETE
              .toString()
              .equals(StringUtils.defaultString(information.getCommunityStatus()));
    } else if (DdfType.FORUM.equals(type)) {
      deleteStatus =
          CommunityStatus.DELETE
              .toString()
              .equals(StringUtils.defaultString(information.getForumStatus()));
    } else {
      deleteStatus =
          CommunityStatus.DELETE
              .toString()
              .equals(StringUtils.defaultString(information.getTopicStatus()));
    }
    return deleteStatus;
  }

  private DDF   setCommunityDdf(AllInformation community) {
    List<String> adminRoleList =
            communityService.getMainGroupCommunityRoleList(Role.COMMUNITY_ADMIN.getId(), null, community.getCommunityId())
                .stream()
                .map(CommunityRoleInfo::getUserId)
                .distinct()
                .collect(toList());
    List<String> memberRoleList =
            communityService.getMainGroupCommunityRoleList(Role.COMMUNITY_MEMBER.getId(), null, community.getCommunityId())
                .stream()
                .map(CommunityRoleInfo::getUserId)
                .distinct()
                .collect(toList());
    String serverHost = yamlConfig.getHost() + COMMUNITY_REFURL_FORMAT;
    String template = getCommunityTemplate(community);
    DDF ddf =
        new DDF()
            .setBaseSection(
                new BaseSection()
                    .setName(community.getCommunityName())
                    .setDesc(community.getCommunityDesc())
                    .setInternalCategory(DdfDocCat.COMMUNITY.toString())
                    .setIcon(DdfDocCat.COMMUNITY.toString())
                    .setRefUrl(String.format(serverHost, community.getCommunityId()))
                    .setCustom(getCommunityCustom(community))
                    .setStatus(DDFStatus.OPEN)
                    .setDisplayTime(Instant.ofEpochMilli(community.getCommunityLastModifiedTime()))
                    .setUserAspectTags(getCommunityAspectTag(community))
                    .setServiceType(ServiceType.DDF_ONLY)
                    .setUserCreatedDate(Instant.ofEpochMilli(community.getCommunityCreateTime()))
                    .setUserModifiedDate(
                        Instant.ofEpochMilli(community.getCommunityModifiedTime())))
            .setPrivilegeSection(
                new PrivilegeSection()
                    .setIdMap(
                        fileService.getPrivilege(
                            community.getCommunityCreateUserId(), adminRoleList, memberRoleList))
                    .addPublic(FileService.PRIV_PUBLIC_SR))
            .setContentSection(new ContentSection().setUserContent(template));
    getRoleMap(
            community.getCommunityCreateUserId(),
            community.getCommunityLastModifiedUserId(),
            new ArrayList<>())
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));

    return ddf;
  }

  private List<Tuple<String, String>> getCommunityAspectTag(AllInformation community) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.addAll(getCommunityInfoTag(community));
    return aspectTagList;
  }

  private List<Tuple<String, String>> getCommunityInfoTag(AllInformation information) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.add(getAspectTag(DdfAspectTagField.COMMUNITY_ID, information.getCommunityId()));
    aspectTagList.add(getAspectTag(DdfAspectTagField.COMMUNITY, information.getCommunityName()));
    aspectTagList.add(
        getAspectTag(DdfAspectTagField.COMMUNITY_EN, information.getCommunityEname()));
    aspectTagList.add(
        getAspectTag(DdfAspectTagField.COMMUNITY_TYPE, information.getCommunityType().toString()));
    aspectTagList.add(
        getAspectTag(
            DdfAspectTagField.COMMUNITY_CATEGORY, information.getCommunityCategory().toString()));
    aspectTagList.add(
        getAspectTag(DdfAspectTagField.COMMUNITY_STATUS, information.getCommunityStatus()));
    return aspectTagList;
  }

  private Tuple<String, String> getAspectTag(DdfAspectTagField field, Object value) {
    value = value == null ? StringUtils.EMPTY : removeInvalidTag(value.toString());
    return new Tuple<>(field.toString(), value.toString());
  }

  private String getCommunityCustom(AllInformation community) {
    Map<String, String> customMap = new HashMap<>();
    String avatar = communityService.getCommunityImgAvatarById(community.getCommunityId());
    if (avatar.length() > 65000) {
      avatar = "";
    }
    int memberCount = communityService.gatherAllCommunityMemberCount(
            community.getCommunityId(), null, null, -1);
    customMap.put(DdfCustomField.COMMUNITY_MEMBER_COUNT.toString(), String.valueOf(memberCount));
    customMap.put(DdfCustomField.COMMUNITY_AVATAR.toString(), avatar);
    return new ObjectMapper().valueToTree(customMap).toString();
  }

  private Map<String, List<UserGroupEntity>> getRoleMap(
      String creator, String lastModifiedUser, List<String> author) {
    Map<String, List<UserGroupEntity>> roleMap = fileService.getRoleMap(creator, author);
    Optional.ofNullable(lastModifiedUser)
        .ifPresent(
            user -> {
              if (!StringUtils.isEmpty(user)) {
                roleMap.put(
                        DdfRole.APPLASSIGNEDMODIFIER.toString(),
                        Arrays.asList(new UserGroupEntity(user)));
              }
            }
        );
    return roleMap
        .entrySet()
        .stream()
        .filter(item -> !item.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String getCommunityTemplate(AllInformation community) {
    Context context = new Context();
    context.setVariable(TemplateType.COMMUNITY.toString(), community);
    context.setVariable(
        TEMPLATE_AUTHOR,
        getUserNameMap(Arrays.asList(community.getCommunityCreateUserId()))
            .getOrDefault(community.getCommunityCreateUserId(), ""));
    return templateService.getThymeleafTemplate(TemplateType.COMMUNITY, context);
  }

  private Map<String, String> getUserNameMap(List<String> userIdList) {
    return userService
        .getUserByIds(userIdList)
        .stream()
        .collect(Collectors.toMap(User::getId, User::getName));
  }

  private DDF setForumDdf(AllInformation forum) {
    List<String> forumMemberRoleList = forumService.getRoleListOfMainGroupForum(forum.getForumId(), false);
    List<String> forumAdminRoleList = forumService.getRoleListOfMainGroupForum(forum.getForumId(), true);
    String serverHost = yamlConfig.getHost() + FORUM_REFURL_FORMAT;
    List<String> tags =
        removeInvalidTag(
            forumService
                .getTagOfForum(forum.getForumId())
                .parallelStream()
                .map(Tag::getLabel)
                .distinct()
                .collect(Collectors.toList()));
    String template =
        getForumTemplate(
            tags.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)), forum);
    DDF ddf =
        new DDF()
            .setBaseSection(
                new BaseSection()
                    .setName(forum.getForumName())
                    .setDesc(forum.getForumDesc())
                    .setInternalCategory(DdfDocCat.FORUM.toString())
                    .setIcon(DdfDocCat.FORUM.toString())
                    .setRefUrl(String.format(serverHost, forum.getForumId()))
                    .setStatus(DDFStatus.OPEN)
                    .setUserTags(tags)
                    .setDisplayTime(Instant.ofEpochMilli(forum.getForumLastModifiedTime()))
                    .setUserAspectTags(getForumAspectTag(forum))
                    .setServiceType(ServiceType.DDF_ONLY)
                    .setUserCreatedDate(Instant.ofEpochMilli(forum.getForumCreateTime()))
                    .setUserModifiedDate(Instant.ofEpochMilli(forum.getForumModifiedTime())))
            .setPrivilegeSection(
                new PrivilegeSection()
                    .setIdMap(
                        fileService.getPrivilege(
                            forum.getForumCreateUserId(), forumAdminRoleList, forumMemberRoleList)))
            .setContentSection(new ContentSection().setUserContent(template));
    if (!ForumType.PRIVATE.equals(forum.getForumType())) {
      ddf.getPrivilegeSection().addPublic(FileService.PRIV_PUBLIC_SR);
    } else {
      ddf.getPrivilegeSection().removePublic(FileService.PRIV_ALL);
    }
    getRoleMap(forum.getForumCreateUserId(), forum.getForumLastModifiedUserId(), new ArrayList<>())
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    return ddf;
  }

  private List<String> getForumAdmin(AllInformation information) {
    return forumService
        .getAdminListOfForum(information.getForumId(), -1, -1)
        .parallelStream()
        .map(User::getId)
        .distinct()
        .collect(toList());
  }

  private List<String> getForumMemberList(AllInformation information) {
    return forumService
        .getMemberOfForum(information.getForumId(), -1, -1)
        .parallelStream()
        .map(User::getId)
        .distinct()
        .collect(toList());
  }

  private List<Tuple<String, String>> getForumAspectTag(AllInformation forum) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.addAll(getCommunityInfoTag(forum));
    aspectTagList.addAll(getForumInfoTag(forum));
    return aspectTagList;
  }

  private List<Tuple<String, String>> getForumInfoTag(AllInformation information) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.add(getAspectTag(DdfAspectTagField.FORUM_ID, information.getForumId()));
    aspectTagList.add(getAspectTag(DdfAspectTagField.FORUM, information.getForumName()));
    aspectTagList.add(
        getAspectTag(DdfAspectTagField.FORUM_TYPE, information.getForumType().toString()));
    aspectTagList.add(getAspectTag(DdfAspectTagField.FORUM_STATUS, information.getForumStatus()));
    return aspectTagList;
  }

  private List<String> removeInvalidTag(List<String> tag) {
    return tag.stream()
        .map(this::removeInvalidTag)
        .filter(item -> !item.isEmpty())
        .collect(Collectors.toList());
  }

  private String removeInvalidTag(String value) {
    return Optional.ofNullable(value)
        .orElseGet(() -> StringUtils.EMPTY)
        .replaceAll(INVALID_TAG_REGEX, StringUtils.SPACE);
  }

  private String getForumTemplate(String tags, AllInformation forum) {
    Context context = new Context();
    context.setVariable(TemplateType.FORUM.toString(), forum);
    context.setVariable(
        TEMPLATE_AUTHOR,
        getUserNameMap(Arrays.asList(forum.getForumCreateUserId()))
            .getOrDefault(forum.getForumCreateUserId(), ""));
    context.setVariable(TEMPLATE_TAG, tags);
    return templateService.getThymeleafTemplate(TemplateType.FORUM, context);
  }

  private List<ReplyDetail> getJsoupParseText(List<ReplyDetail> replyList) {
    return replyList
        .stream()
        .map(
            item -> {
              if (item.getReplyIndex() != 0 || item.getReplyConclusionText().isEmpty()) {
                item.replyText(Jsoup.parse(item.getReplyText()).text());
              }
              return item;
            })
        .collect(Collectors.toList());
  }

  private DDF setTopicDdf(
      AllInformation topic,
      List<ReplyDetail> replyList,
      List<AttachmentInfo> attachmentList,
      Map<String, List<UserGroupEntity>> topicRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap) {
    String serverHost = yamlConfig.getHost() + TOPIC_REFURL_FORMAT;
    List<String> tags = removeInvalidTag(getTopicTag(topic.getForumId(), topic.getTopicId()));
    String template =
        getTopicTemplate(
            tags.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)), topic, replyList);
    DDF ddf =
        new DDF()
            .setBaseSection(
                new BaseSection()
                    .setName(topic.getTopicTitle())
                    .setInternalCategory(DdfDocCat.TOPIC.toString())
                    .setIcon(DdfDocCat.TOPIC.toString())
                    .setRefUrl(String.format(serverHost, topic.getTopicId()))
                    .setStatus(DDFStatus.OPEN)
                    .setUserTags(
                        removeInvalidTag(getTopicTag(topic.getForumId(), topic.getTopicId())))
                    .setDisplayTime(Instant.ofEpochMilli(topic.getTopicLastModifiedTime()))
                    .setUserAspectTags(getTopicAspectTag(topic))
                    .setServiceType(ServiceType.DDF_ONLY)
                    .setUserCreatedDate(Instant.ofEpochMilli(topic.getTopicCreateTime()))
                    .setUserModifiedDate(Instant.ofEpochMilli(topic.getTopicModifiedTime()))
                    .setReferences(
                        attachmentList
                            .stream()
                            .map(AttachmentInfo::getAttachmentId)
                            .map(this::getReference)
                            .collect(Collectors.toList())))
            .setContentSection(new ContentSection().setUserContent(template))
            .setPrivilegeSection(new PrivilegeSection().setIdMap(privilegeMap));
    if (TopicState.CONCLUDED.equals(topic.getTopicState())
        || TopicState.BRIEFCONCLUDED.equals(topic.getTopicState())) {
      replyList
          .parallelStream()
          .filter(item -> item.getReplyIndex() == CONCLUSION_INDEX)
          .findFirst()
          .ifPresent(
              conclusion -> {
                ddf.getBaseSection()
                    .setPeople(
                        DdfRole.CONCLUSIONAUTHOR.toString(),
                        Arrays.asList(new UserGroupEntity(conclusion.getReplyCreateUserId())));
                ddf.getContentSection()
                    .setFocalContent(
                        String.format(
                            CONTENT_REPLY_FORMAT,
                            getUserNameMap(Arrays.asList(conclusion.getReplyCreateUserId()))
                                .getOrDefault(conclusion.getReplyCreateUserId(), ""),
                            conclusion.getReplyConclusionText()));
              });
    }
    if (!ForumType.PRIVATE.equals(topic.getForumType())) {
      ddf.getPrivilegeSection().addPublic(FileService.PRIV_PUBLIC_SR);
    } else {
      ddf.getPrivilegeSection().removePublic(FileService.PRIV_ALL);
    }
    topicRoleMap
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    return ddf;
  }

  private List<String> getTopicTag(int forumId, int topicId) {
    List<String> forumTag =
        forumService
            .getTagOfForum(forumId)
            .parallelStream()
            .map(Tag::getLabel)
            .collect(Collectors.toList());
    List<String> topicTag =
        topicService
            .getTagOfTopic(topicId)
            .parallelStream()
            .map(Tag::getLabel)
            .collect(Collectors.toList());
    topicTag.addAll(forumTag);
    return topicTag.parallelStream().distinct().collect(Collectors.toList());
  }

  private Reference getReference(String attachmentId) {
    Reference ref = new Reference();
    ref.setRefId(attachmentId);
    ref.setRefType(RefType.ATTACHMENT);
    return ref;
  }

  private List<Tuple<String, String>> getTopicAspectTag(AllInformation topic) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.addAll(getCommunityInfoTag(topic));
    aspectTagList.addAll(getForumInfoTag(topic));
    aspectTagList.addAll(getTopicInfoTag(topic, false));
    topicService
         .getTopicAppField(topic.getTopicId(), DbLanguage.ENUS.toString())
         .stream()
         .filter(x -> !StringUtils.isEmpty(x.getName()))
         .map(appField -> new Tuple<>(DdfAspectTagField.APP_FIELD.toString(), appField.getName()))
         .forEach(aspectTagList::add);
    return aspectTagList;
  }

  private List<Tuple<String, String>> getTopicInfoTag(
      AllInformation topic, boolean includeIdAndTitle) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    if (includeIdAndTitle) {
      aspectTagList.add(getAspectTag(DdfAspectTagField.TOPIC_ID, topic.getTopicId()));
      aspectTagList.add(getAspectTag(DdfAspectTagField.TOPIC, topic.getTopicTitle()));
    }
    aspectTagList.add(getAspectTag(DdfAspectTagField.TOPIC_TYPE, topic.getTopicType().toString()));
    aspectTagList.add(getAspectTag(DdfAspectTagField.TOPIC_STATUS, topic.getTopicStatus()));
    aspectTagList.add(
        getAspectTag(DdfAspectTagField.TOPIC_SITUATION, topic.getTopicSituation().toString()));
    aspectTagList.add(
        getAspectTag(
            DdfAspectTagField.TOPIC_CONCLUSION_STATE,
            getTopicConclusionState(topic.getTopicState(), topic.isTopicShowState())));
    return aspectTagList;
  }

  private String getTopicConclusionState(TopicState state, Boolean showState) {
    if(state == null) {
      return "";
    }

    StringJoiner sj = new StringJoiner(SPACE_SEPARATOR);
    sj.add(state.toString());
    sj.add(TOPIC_SHOW_STATE_PREFIX.concat(showState.toString()));
    return sj.toString();
  }

  private String getTopicTemplate(String tags, AllInformation topic, List<ReplyDetail> replyList) {
    List<String> userIdList = new ArrayList<>();
    replyList.forEach(
        item -> {
          userIdList.add(item.getReplyCreateUserId());
          item.getNestedReplyList()
              .forEach(nestedReply -> userIdList.add(nestedReply.getReplyCreateUserId()));
        });
    Context context = new Context();
    context.setVariable(TemplateType.TOPIC.toString(), topic);
    context.setVariable(TEMPLATE_TAG, tags);
    context.setVariable(TEMPLATE_REPLYLIST, replyList);
    Map<String, String> userNameMap =
        getUserNameMap(
            userIdList
                .stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList()));
    context.setVariable(TEMPLATE_USERMAP, userNameMap);
    return templateService.getThymeleafTemplate(TemplateType.TOPIC, context);
  }

  private void updateTopicAttachmentAndRichTextImage(
      AllInformation topic,
      List<AttachmentInfo> attachmentList,
      Map<String, List<UserGroupEntity>> topicRoleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      String groupId) {
    List<Tuple<String, String>> attachmentAspectTagList = getAttachmentAspectTag(topic);
    updateRichTextImageDdf(topic, topicRoleMap, privilegeMap,
            attachmentAspectTagList, groupId);

    Map<String, List<AttachmentAppFieldEntity>> appFieldMap =
        topicDao
            .getTopicAllAttachmentAppField(
                attachmentList
                    .parallelStream()
                    .map(AttachmentInfo::getAttachmentId)
                    .collect(toList()),
                DbLanguage.ENUS.toString())
            .stream()
            .collect(Collectors.groupingBy(AttachmentAppFieldEntity::getAttachmentId));
    attachmentList.forEach(
        item -> {
          List<Tuple<String, String>> aspectTags = new ArrayList<>(attachmentAspectTagList);
          appFieldMap
              .getOrDefault(item.getAttachmentId(), emptyList())
              .stream()
              .filter(x -> !StringUtils.isEmpty(x.getAppFieldName()))
              .map(
                  appField ->
                      new Tuple<>(
                          DdfAspectTagField.APP_FIELD.toString(), appField.getAppFieldName()))
              .forEach(aspectTags::add);
          DDF ddf =
              setAttachmentDdf(
                  topic,
                  topicRoleMap,
                  privilegeMap,
                  aspectTags,
                  fileService.isImage(item.getFileExt()),
                  groupId);
          updateFile(item.getAttachmentId(), ddf);
        });
  }

  private void updateFile(String ddfId, DDF ddf) {
    fileService.updateFileWithoutLockChecked(ddfId, ddf);
  }

  private DDF setAttachmentDdf(
      AllInformation topic,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      List<Tuple<String, String>> aspectTags,
      boolean isImage,
      String groupId) {
    DDF ddf =
        new DDF()
            .setBaseSection(new BaseSection().setUserAspectTags(aspectTags))
            .setPrivilegeSection(new PrivilegeSection().setIdMap(privilegeMap));
    if (ForumType.PRIVATE == topic.getForumType()) {
      ddf.getPrivilegeSection().removePublic(FileService.PRIV_ALL);
    } else {
      ddf.getPrivilegeSection().addPublic(FileService.PRIV_PUBLIC_SR);
    }
    roleMap
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    if (isImage) {
      ddf.getBaseSection().setSystemTags(getImageSystemTags(topic));
    }
    if (!groupId.isEmpty()) {
      ddf.getBaseSection().setOrg(new UserGroupEntity(groupId));
    }
    return ddf;
  }

  private Map<SystemTag, String> getImageSystemTags(AllInformation topic) {
    Map<SystemTag, String> systemTags = new EnumMap<>(SystemTag.class);
    systemTags.put(SystemTag.NO_WATERMARK, ForumType.PUBLIC == topic.getForumType() ? EMPTY : null);
    return systemTags;
  }

  private List<Tuple<String, String>> getAttachmentAspectTag(AllInformation topic) {
    List<Tuple<String, String>> aspectTagList = new ArrayList<>();
    aspectTagList.addAll(getCommunityInfoTag(topic));
    aspectTagList.addAll(getForumInfoTag(topic));
    aspectTagList.addAll(getTopicInfoTag(topic, true));
    return aspectTagList;
  }

  private Set<String> extractImageIdFromRichText(String html) {
    Set<String> imageIdList = new HashSet<>();
    Pattern pattern = Pattern.compile(DATAHIVE_URL_WITH_UUID);
    Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      imageIdList.add(matcher.group().replace(DATAHIVE_URL_PREFIX, ""));
    }
    return imageIdList;
  }

  private void updateReplyAttachmentAndRichTextImage(
      AllInformation topic, ReplyDetail reply, List<String> adminRoleList, List<String> memberRoleList,
      String groupId) {
    List<AttachmentInfo> attachmentList = replyDao.getReplyAttachments(reply.getReplyId());

    Map<String, List<UserGroupEntity>> roleMap =
        getRoleMap(null, reply.getReplyModifiedUserId(), new ArrayList<>());
    Map<String, Set<PrivilegeType>> privilegeMap =
        fileService.getPrivilege(reply.getReplyCreateUserId(), adminRoleList, memberRoleList);
    List<Tuple<String, String>> attachmentAspectTagList = getAttachmentAspectTag(topic);
    updateRichTextImageDdf(topic, reply, roleMap, privilegeMap, attachmentAspectTagList, groupId);

    Map<String, List<AttachmentAppFieldEntity>> appFieldMap =
        replyDao
            .getReplyAllAttachmentAppField(
                attachmentList
                    .parallelStream()
                    .map(AttachmentInfo::getAttachmentId)
                    .collect(toList()),
                DbLanguage.ENUS.toString())
            .stream()
            .collect(Collectors.groupingBy(AttachmentAppFieldEntity::getAttachmentId));
    attachmentList.forEach(
        item -> {
          List<Tuple<String, String>> aspectTags = new ArrayList<>(attachmentAspectTagList);
          appFieldMap
              .getOrDefault(item.getAttachmentId(), emptyList())
              .stream()
              .filter(x -> !StringUtils.isEmpty(x.getAppFieldName()))
              .map(
                  appField ->
                      new Tuple<>(
                          DdfAspectTagField.APP_FIELD.toString(), appField.getAppFieldName()))
              .forEach(aspectTags::add);
          DDF ddf =
              setAttachmentDdf(
                  topic, roleMap, privilegeMap, aspectTags,
                      fileService.isImage(item.getFileExt()),
                      groupId);
          updateFile(item.getAttachmentId(), ddf);
        });
  }

  private void upsertDdf(int id, DdfType type, DDF ddf) {
    String uuid =
        dataHiveStoreService.upsertFile(
            new DDFDoc().setDDF(ddf),
            String.format("%s-%s", type, id),
            true,
            ONLYDDF_HISTORY_COUNT);
    log.debug("Upsert " + type.toString() + " ddf : " + uuid);
    storeDdfId(type, id, uuid);
  }

  private void storeDdfId(DdfType type, int id, String uuid) {
    ddfDao.storeDdfId(type.toString(), id, uuid);
  }

  private void deleteDdf(String fileId) {
    fileService.deleteRealFile(fileId);
  }

  private void updateRichTextImageDdf(
      AllInformation topic,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      List<Tuple<String, String>> aspectTags,
      String groupId) {
    List<String> richTextImages = new ArrayList<>();
    richTextImages.addAll(extractImageIdFromRichText(topic.getTopicText()));
    updateRichTextImageDdf(topic, richTextImages, roleMap, privilegeMap, aspectTags, groupId);
  }

  private void updateRichTextImageDdf(
      AllInformation topic,
      ReplyDetail reply,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      List<Tuple<String, String>> aspectTags,
      String groupId) {
    List<String> richTextImages = new ArrayList<>();
    richTextImages.addAll(extractImageIdFromRichText(reply.getReplyText()));
    richTextImages.addAll(extractImageIdFromRichText(reply.getReplyConclusionText()));
    updateRichTextImageDdf(topic, richTextImages, roleMap, privilegeMap, aspectTags, groupId);
  }

  private void updateRichTextImageDdf(
      AllInformation topic,
      List<String> fileIdList,
      Map<String, List<UserGroupEntity>> roleMap,
      Map<String, Set<PrivilegeType>> privilegeMap,
      List<Tuple<String, String>> aspectTags,
      String groupId) {
    DDF ddf = setAttachmentDdf(topic, roleMap, privilegeMap, aspectTags, true, groupId);
    // setSystemTags 不支援 UpdateAction.SET
    try {
      fileIdList.forEach(id -> updateFile(id, ddf));
    } catch (NoSuchElementException e) {
      log.error(Optional.ofNullable(e.getMessage()).orElse(e.toString()));
    }

  }
}