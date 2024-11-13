package com.delta.dms.community.service;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.config.DiaConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.DiaDao;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.dao.entity.DiaAttachmentDetailEntity;
import com.delta.dms.community.dao.entity.DiaAttachmentPathEntity;
import com.delta.dms.community.dao.entity.DiaEntity;
import com.delta.dms.community.dao.entity.DiaMemberEntity;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.enums.DiaAttachmentPathStatus;
import com.delta.dms.community.enums.DiaStatus;
import com.delta.dms.community.model.BasicAuthToken;
import com.delta.dms.community.model.Jwt;
import com.delta.dms.community.swagger.model.ApprovalStatus;
import com.delta.dms.community.swagger.model.AttachmentWithAuthor;
import com.delta.dms.community.swagger.model.CommunityCategory;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.CreatedCommunityData;
import com.delta.dms.community.swagger.model.DiaAttachmentPathDto;
import com.delta.dms.community.swagger.model.DiaClassification;
import com.delta.dms.community.swagger.model.DiaDto;
import com.delta.dms.community.swagger.model.DiaErrorMessage;
import com.delta.dms.community.swagger.model.DiaMemberDto;
import com.delta.dms.community.swagger.model.DiaMemberType;
import com.delta.dms.community.swagger.model.DiaResultDto;
import com.delta.dms.community.swagger.model.ForumData;
import com.delta.dms.community.swagger.model.ForumStatus;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.GeneralStatus;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.ResponseData;
import com.delta.dms.community.swagger.model.ReviewAction;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.swagger.model.TopicCreationData;
import com.delta.dms.community.swagger.model.TopicType;
import com.delta.dms.community.swagger.model.UserSession;
import com.delta.dms.community.swagger.model.UserStatus;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DiaService {

  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private static final String DIA_OK = "D";
  private static final long MAX_SINGLE_FILE_SIZE = 629145600L;
  private static final int ERROR_MAX_LENGTH = 1024;

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String LOG_SUBJECT_FORMAT = "[DMS] dia auto upload ddf process log [%s]";
  private static final String LOG_CONTENT_FORMAT_SUCCEED =
      "<p>[Succeeded]</p><p>process period: %s~%s</p><p>created ddf id: %s</p>";
  private static final String LOG_CONTENT_FORMAT_FAIL =
      "<p>[Failed]</p><p>process period: %s~%s</p><p>error message: %s</p>";
  private static final int LOG_PRIORITY = 3;

  private static final String FORUM_NAME_FORMAT =
      "Application %d (Committee members & Applicants) #%s";
  private static final String FORUM_NAME_REGEX = "^Application ([0-9]*)";

  private UserService userService;
  private CommunityService communityService;
  private ForumService forumService;
  private TopicService topicService;
  private FileService fileService;
  private DiaSmbService diaSmbService;
  private DiaConfig diaConfig;
  private YamlConfig yamlConfig;
  private DiaDao diaDao;
  private MailDao mailDao;

  @Autowired
  public DiaService(
      UserService userService,
      CommunityService communityService,
      ForumService forumService,
      TopicService topicService,
      FileService fileService,
      DiaSmbService diaSmbService,
      DiaConfig diaConfig,
      YamlConfig yamlConfig,
      DiaDao diaDao,
      MailDao mailDao) {
    this.userService = userService;
    this.communityService = communityService;
    this.forumService = forumService;
    this.topicService = topicService;
    this.fileService = fileService;
    this.diaSmbService = diaSmbService;
    this.diaConfig = diaConfig;
    this.yamlConfig = yamlConfig;
    this.diaDao = diaDao;
    this.mailDao = mailDao;
  }

  @Transactional
  public DiaResultDto createDia(DiaDto data) throws IOException {
    String decodedAuthToken =
        new String(
            Base64.getUrlDecoder().decode(BasicAuthToken.get()), StandardCharsets.UTF_8.name());
    if (!StringUtils.equals(diaConfig.getDecodedAuthToken(), decodedAuthToken)) {
      return new DiaResultDto().status(GeneralStatus.FAIL).message(DiaErrorMessage.FORBIDDEN);
    }
    if (Objects.isNull(data.getAction())) {
      return new DiaResultDto().status(GeneralStatus.FAIL).message(DiaErrorMessage.ACTION_INVALID);
    }
    if (!Objects.equals(data.getAction(), DIA_OK)) {
      return new DiaResultDto().status(GeneralStatus.SUCCESS);
    }
    if (Objects.isNull(DiaClassification.fromValue(data.getApplicationCategory()))) {
      return new DiaResultDto()
          .status(GeneralStatus.FAIL)
          .message(DiaErrorMessage.CATEGORY_INVALID);
    }
    Jwt.set(userService.getSysAdminToken());
    DiaEntity entity = convertToDiaEntity(data);
    List<String> attachmentPathList =
        entity
            .getAttachmentPathList()
            .parallelStream()
            .map(DiaAttachmentPathEntity::getAttachmentPath)
            .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(attachmentPathList)
        && diaDao.countDiaAttachmentPath(attachmentPathList) > NumberUtils.INTEGER_ZERO) {
      return new DiaResultDto()
          .status(GeneralStatus.FAIL)
          .message(DiaErrorMessage.ATTACHMENT_DUPLICATE);
    }

    try {
      diaDao.insertDia(entity);
      if (!CollectionUtils.isEmpty(entity.getAttachmentPathList())) {
        diaDao.insertDiaAttachmentPath(entity.getInnovationAwardId(), attachmentPathList);
      }
      if (!CollectionUtils.isEmpty(entity.getMemberList())) {
        diaDao.insertDiaMember(entity.getInnovationAwardId(), entity.getMemberList());
      }
      DiaResultDto result = new DiaResultDto().status(GeneralStatus.SUCCESS);
      result.setId(entity.getInnovationAwardId());
      return result;
    } catch (IllegalArgumentException e) {
      return new DiaResultDto()
          .status(GeneralStatus.FAIL)
          .message(DiaErrorMessage.fromValue(e.getMessage()));
    }
  }

  public void createAllDiaAttachment() {
    validateSysPrivilege();
    final String startTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
    DiaStatus result = DiaStatus.SUCCESS;
    String resultMessage = StringUtils.EMPTY;

    List<DiaAttachmentDetailEntity> attachmentNameList = diaDao.getDiaAttachmentWithoutDdf();
    List<String> attachmentPathList =
        diaDao.getDiaAttachmentPathByStatus(DiaAttachmentPathStatus.WAIT);
    Set<String> successAttachmentPathSet = new HashSet<>();
    Optional.ofNullable(attachmentPathList)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(
            list -> diaDao.updateDiaAttachmentPathStatus(list, DiaAttachmentPathStatus.PROCESSING));
    try {
      List<String> succeedDdfIdList = downloadAndGenerateDdf(attachmentNameList);
      for (String path : attachmentPathList) {
        List<String> fileNameList = insertAndUpdateAttachmentInfo(path);
        succeedDdfIdList.addAll(
            downloadAndGenerateDdf(
                fileNameList
                    .parallelStream()
                    .map(
                        fileName ->
                            new DiaAttachmentDetailEntity()
                                .setAttachmentPath(path)
                                .setFileName(fileName))
                    .collect(Collectors.toList())));
        successAttachmentPathSet.add(path);
      }
      resultMessage =
          succeedDdfIdList.stream().collect(Collectors.joining(Constants.COMMA_DELIMITER));
    } catch (Exception e) {
      result = DiaStatus.FAIL;
      resultMessage = e.getMessage();
      Optional.ofNullable(
              attachmentPathList
                  .parallelStream()
                  .filter(item -> !successAttachmentPathSet.contains(item))
                  .collect(toList()))
          .filter(CollectionUtils::isNotEmpty)
          .ifPresent(
              list -> diaDao.updateDiaAttachmentPathStatus(list, DiaAttachmentPathStatus.WAIT));
    }
    diaSmbService.close();
    final String endTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
    diaDao.insertSyncDiaAttachmentLog(startTime, endTime, result, resultMessage);
    insertDiaMail(startTime, endTime, result, resultMessage);
  }

  public void createAllDiaTopic() {
    validateSysPrivilege();
    checkAndUpdateAttachmentPathStatus();
    checkAndUpdateDiaStatus();
    createDiaTopic();
  }

  private Map<String, String> getUserIdMapByAccount(List<String> account) {
    return userService
        .getUserBySamAccounts(account)
        .stream()
        .filter(user -> StringUtils.equals(UserStatus.ACTIVE.toString(), user.getStatus()))
        .distinct()
        .collect(
            Collectors.toMap(
                user -> user.getProfileSAMAccount().toLowerCase(), UserSession::getCommonUUID));
  }

  private List<String> insertAndUpdateAttachmentInfo(String path) throws IOException {
    Map<String, Long> attachmentInfoMap = diaSmbService.listFileNameAndSize(path);
    if (!attachmentInfoMap.isEmpty()) {
      diaDao.insertDiaAttachment(path, attachmentInfoMap);
    }
    diaDao.updateDiaAttachmentPathStatus(singletonList(path), DiaAttachmentPathStatus.CHECKED);
    return attachmentInfoMap
        .entrySet()
        .parallelStream()
        .map(Entry::getKey)
        .collect(Collectors.toList());
  }

  private List<String> downloadAndGenerateDdf(List<DiaAttachmentDetailEntity> attachmentList) {
    List<String> ddfIdList = new ArrayList<>();
    for (DiaAttachmentDetailEntity attachment : attachmentList) {
      if (attachment.getFileSize() > MAX_SINGLE_FILE_SIZE) {
        log.error(
            String.format(
                "File {%s/%s} size exceeded: %d",
                attachment.getAttachmentPath(),
                attachment.getFileName(),
                attachment.getFileSize()));
        continue;
      }

      try {
        String ddfId =
            downloadAndGenerateDdf(attachment.getAttachmentPath(), attachment.getFileName());
        diaDao.updateDiaAttachmentDdfId(
            attachment.getAttachmentPath(), attachment.getFileName(), ddfId);
        ddfIdList.add(ddfId);
      } catch (IOException | NotFoundException e) {
        log.error(e);
      }
    }
    return ddfIdList;
  }

  private String downloadAndGenerateDdf(String attachmentPath, String fileName)
      throws IOException, NotFoundException {
    byte[] fileData = diaSmbService.downloadFile(attachmentPath, fileName);
    Jwt.set(userService.getUserToken(diaConfig.getAdminAuthenticationToken()));
    String ddfId = fileService.uploadFile(fileName, fileData);
    log.debug(String.format("Created attachment {%s}, ddfId: {%s}", fileName, ddfId));
    if (StringUtils.isEmpty(ddfId)) {
      throw new NotFoundException(
          String.format("Failed to create Ddf for {%s/%s}", attachmentPath, fileName));
    }
    return ddfId;
  }

  private void checkAndUpdateAttachmentPathStatus() {
    List<String> checkedAttachmentPathList =
        diaDao.getDiaAttachmentPathByStatus(DiaAttachmentPathStatus.CHECKED);
    checkedAttachmentPathList.forEach(
        path -> {
          List<String> ddfIdList =
              diaDao.getDiaAttachmentDdfIdByPath(Collections.singletonList(path));
          if (ddfIdList.parallelStream().noneMatch(StringUtils::isEmpty)) {
            diaDao.updateDiaAttachmentPathStatus(
                singletonList(path), DiaAttachmentPathStatus.DOWNLOADED);
          }
        });
  }

  private void checkAndUpdateDiaStatus() {
    List<DiaEntity> waitDiaList = diaDao.getDiaByStatus(DiaStatus.WAIT);
    waitDiaList.forEach(
        dia -> {
          if (dia.getAttachmentPathList()
              .parallelStream()
              .allMatch(
                  path -> DiaAttachmentPathStatus.DOWNLOADED == path.getAttachmentPathStatus())) {
            diaDao.updateDiaStatus(
                Collections.singletonList(dia.getInnovationAwardId()),
                DiaStatus.ATTACHMENT_CREATED);
          }
        });
  }

  private void createDiaTopic() {
    List<DiaEntity> checkedDiaInfoList = diaDao.getDiaByStatus(DiaStatus.ATTACHMENT_CREATED);
    Optional.of(checkedDiaInfoList)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresent(
            list ->
                diaDao.updateDiaStatus(
                    list.stream().map(DiaEntity::getInnovationAwardId).collect(Collectors.toList()),
                    DiaStatus.CREATING));
    checkedDiaInfoList.forEach(
        dia -> {
          log.debug("Creating dia topic: " + dia.getInnovationAwardId());
          createDiaTopic(dia.getInnovationAwardId());
        });
  }

  private void createDiaTopic(int id) {
    try {
      DiaEntity entity = diaDao.getDiaById(id);
      List<String> memberList =
          entity
              .getMemberList()
              .stream()
              .map(DiaMemberEntity::getUserId)
              .distinct()
              .collect(Collectors.toList());
      int communityId =
          createCommunity(
              diaConfig.getCommunityName(
                  getDiaYear(entity.getApplyTime()), entity.getClassificationName()),
              memberList);
      int forumId = createForum(communityId, memberList, entity.getOaInstanceCode());
      int topicId = createTopic(forumId, entity);
      insertDiaCreationResult(id, GeneralStatus.SUCCESS, Integer.toString(topicId));
    } catch (Exception e) {
      String msg = Optional.ofNullable(e.getMessage()).orElseGet(() -> StringUtils.EMPTY);
      insertDiaCreationResult(id, GeneralStatus.FAIL, msg);
    }
  }

  private int getDiaYear(long timeMillis) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timeMillis);
    int month = calendar.get(Calendar.MONTH);
    return calendar.get(Calendar.YEAR)
        + (8 > month ? NumberUtils.INTEGER_ZERO : NumberUtils.INTEGER_ONE);
  }

  private void insertDiaCreationResult(int diaId, GeneralStatus result, String msg) {
    if (msg.length() > ERROR_MAX_LENGTH) {
      msg = msg.substring(0, ERROR_MAX_LENGTH - 1);
    }
    diaDao.updateDiaCreationResult(diaId, result, msg, System.currentTimeMillis());
  }

  private int createCommunity(String name, List<String> members) {
    CreatedCommunityData data = convertToCommunityCreationData(name, members);
    ResponseData response = communityService.createCommunity(data);
    if (HttpStatus.ACCEPTED.value() == response.getStatusCode()) {
      int communityId = communityService.getCommunityIdByCommunityName(name);
      communityService.addAdminIntoCommunity(
          Collections.singletonList(Utility.getUserIdFromSession()), communityId);
      if (!CollectionUtils.isEmpty(members)) {
        communityService.addMemberIntoCommunity(members, communityId);
      }
      return communityId;
    }
    if (HttpStatus.CREATED.value() == response.getStatusCode()) {
      response =
          communityService.reviewCommunityCreation(
              response.getId(), new ApprovalStatus().status(ReviewAction.AUTO_APPROVED));
      if (HttpStatus.CREATED.value() == response.getStatusCode()) {
        return response.getId();
      }
    }
    throw new HttpClientErrorException(HttpStatus.valueOf(response.getStatusCode()));
  }

  private int createForum(int communityId, List<String> members, String oaInstanceCode) {
    ForumData forumData = convertToForumData(communityId, members, oaInstanceCode);
    ResponseData response = forumService.createForum(forumData);
    if (HttpStatus.CREATED.value() != response.getStatusCode()) {
      throw new HttpClientErrorException(HttpStatus.valueOf(response.getStatusCode()));
    }
    return response.getId();
  }

  private int createTopic(int forumId, DiaEntity entity) {
    TopicCreationData topicData = convertToTopicCreationData(forumId, entity);
    return topicService.createTopic(topicData, false);
  }

  private void insertDiaMail(String startTime, String endTime, DiaStatus result, String message) {
    mailDao.insertMail(
        Utility.getUserIdFromSession(),
        Constants.DEFAULT_LOG_SENDER,
        diaDao.getDiaLogRecipient().stream().collect(Collectors.joining(Constants.COMMA_DELIMITER)),
        String.format(LOG_SUBJECT_FORMAT, yamlConfig.getEnvIdentity()),
        String.format(
            DiaStatus.SUCCESS == result ? LOG_CONTENT_FORMAT_SUCCEED : LOG_CONTENT_FORMAT_FAIL,
            startTime,
            endTime,
            message),
        LOG_PRIORITY);
  }

  private void validateSysPrivilege() {
    if (!userService.isSysAdmin()) {
      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
  }

  private DiaEntity convertToDiaEntity(DiaDto data) throws IOException {
    List<String> teamMemberList =
        Arrays.asList(mapper.readValue(data.getTeamMember(), DiaMemberDto[].class))
            .stream()
            .map(item -> item.getNtAccount().toLowerCase())
            .distinct()
            .collect(Collectors.toList());
    List<String> accountList = new ArrayList<>(teamMemberList);
    accountList.add(data.getLeader());
    accountList.add(data.getContactWindow());
    accountList =
        accountList
            .parallelStream()
            .distinct()
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    Map<String, String> userIdMap = getUserIdMapByAccount(accountList);
    List<DiaMemberEntity> memberList = new ArrayList<>();
    String leaderUserId = userIdMap.get(data.getLeader().toLowerCase());
    if (StringUtils.isNotEmpty(leaderUserId)) {
      memberList.add(
          new DiaMemberEntity().setUserId(leaderUserId).setUserType(DiaMemberType.TEAMLEADER));
    }
    String contactWindowUserId = userIdMap.get(data.getContactWindow().toLowerCase());
    if (StringUtils.isNotEmpty(contactWindowUserId)) {
      memberList.add(
          new DiaMemberEntity()
              .setUserId(contactWindowUserId)
              .setUserType(DiaMemberType.CONTACTWINDOW));
    }
    List<DiaMemberEntity> teamMemberUserIdList =
        teamMemberList
            .stream()
            .map(userIdMap::get)
            .filter(StringUtils::isNotEmpty)
            .map(
                item ->
                    new DiaMemberEntity().setUserId(item).setUserType(DiaMemberType.TEAMMEMBERS))
            .collect(Collectors.toList());
    memberList.addAll(teamMemberUserIdList);

    return new DiaEntity()
        .setClassificationName(DiaClassification.fromValue(data.getApplicationCategory()))
        .setProjectItemName(data.getProjectName())
        .setOaInstanceCode(data.getInstanceCode())
        .setProjectExecutiveSummary(defaultString(data.getProjectExecutiveSummar()))
        .setStatus(DiaStatus.WAIT)
        .setApplyTime(System.currentTimeMillis())
        .setMemberList(memberList)
        .setAttachmentPathList(
            Arrays.asList(mapper.readValue(data.getAttachment(), DiaAttachmentPathDto[].class))
                .stream()
                .map(item -> StringUtils.trimToEmpty(item.getFileId()))
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .map(item -> new DiaAttachmentPathEntity().setAttachmentPath(item))
                .collect(Collectors.toList()));
  }

  private CreatedCommunityData convertToCommunityCreationData(String name, List<String> members) {
    return new CreatedCommunityData()
        .name(name)
        .desc(Constants.CONTENT_EMPTY)
        .type(CreatedCommunityData.TypeEnum.PRIVATE)
        .status(CommunityStatus.OPEN)
        .admins(Collections.singletonList(Utility.getUserIdFromSession()))
        .members(members)
        .category(CommunityCategory.GENERAL)
        .notificationType(null);
  }

  private ForumData convertToForumData(
      int communityId, List<String> members, String oaInstanceCode) {
    List<ForumInfo> forumList =
        forumService.searchForumInfoList(
            communityId,
            Collections.singletonList(ForumType.PRIVATE),
            NumberUtils.INTEGER_MINUS_ONE,
            NumberUtils.INTEGER_MINUS_ONE,
            new Order(Sort.Direction.DESC, SortField.UPDATETIME.toString()));

    int applicationId =
        forumList
            .parallelStream()
            .map(item -> extractApplicationId(item.getForumName()))
            .mapToInt(v -> v)
            .max()
            .orElseGet(() -> NumberUtils.INTEGER_ZERO);
    return new ForumData()
        .communityId(communityId)
        .name(String.format(FORUM_NAME_FORMAT, ++applicationId, oaInstanceCode))
        .tag(null)
        .type(ForumType.PRIVATE)
        .status(ForumStatus.OPEN)
        .admins(Collections.singletonList(Utility.getUserIdFromSession()))
        .members(members);
  }

  private int extractApplicationId(String name) {
    Pattern mime = Pattern.compile(FORUM_NAME_REGEX);
    Matcher matcher = mime.matcher(name);
    int result = NumberUtils.INTEGER_ZERO;
    if (matcher.find()) {
      result = Integer.valueOf(matcher.group(NumberUtils.INTEGER_ONE));
    }
    return result;
  }

  private TopicCreationData convertToTopicCreationData(int forumId, DiaEntity entity) {
    List<LabelValueDto> defaultAppField = getDefaultAppField();
    List<AttachmentWithAuthor> attachmentList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(entity.getAttachmentPathList())) {
      attachmentList =
          diaDao
              .getDiaAttachmentDdfIdByPath(
                  entity
                      .getAttachmentPathList()
                      .parallelStream()
                      .map(DiaAttachmentPathEntity::getAttachmentPath)
                      .collect(Collectors.toList()))
              .stream()
              .map(
                  item ->
                      new AttachmentWithAuthor()
                          .id(item)
                          .author(Collections.singletonList(Utility.getUserIdFromSession()))
                          .appField(defaultAppField))
              .collect(Collectors.toList());
    }

    return new TopicCreationData()
        .forumId(forumId)
        .title(entity.getProjectItemName())
        .tag(Collections.emptyList())
        .type(TopicType.GENERAL)
        .text(defaultString(entity.getProjectExecutiveSummary()))
        .attachment(attachmentList)
        .appField(defaultAppField);
  }

  private List<LabelValueDto> getDefaultAppField() {
    return Collections.singletonList(new LabelValueDto().value(diaConfig.getDefaultAppFieldId()));
  }
}
