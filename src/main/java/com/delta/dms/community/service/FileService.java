package com.delta.dms.community.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.datahive.DDFQuery.QueryTerm;
import com.delta.datahive.DDFQuery.QueryTree;
import com.delta.datahive.activitylog.args.App;
import com.delta.datahive.activitylog.args.LogStatus;
import com.delta.datahive.activitylog.args.LogTimeUnit;
import com.delta.datahive.activitylog.args.ObjectType;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.DDF;
import com.delta.datahive.api.DDF.DDFDoc;
import com.delta.datahive.api.DDF.Data;
import com.delta.datahive.api.DDF.DataType;
import com.delta.datahive.api.DDF.DummyData;
import com.delta.datahive.api.DDF.PrivilegeSection;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.ProtectedType;
import com.delta.datahive.api.DDF.ReadArgs;
import com.delta.datahive.api.DDF.ServiceType;
import com.delta.datahive.api.DDF.UserGroupEntity;
import com.delta.datahive.data.DDFID;
import com.delta.datahive.data.DDFSection;
import com.delta.datahive.structures.Tuple;
import com.delta.datahive.types.ConverterStatus;
import com.delta.datahive.types.DDFStatus;
import com.delta.datahive.types.Sorting;
import com.delta.datahive.types.UpdateAction;
import com.delta.dms.community.adapter.StreamingAdapter;
import com.delta.dms.community.config.FileConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.DdfDao;
import com.delta.dms.community.dao.FileDao;
import com.delta.dms.community.enums.DdfCustomField;
import com.delta.dms.community.enums.DdfDocCat;
import com.delta.dms.community.enums.DdfQueueStatus;
import com.delta.dms.community.exception.AuthenticationException;
import com.delta.dms.community.exception.DataHiveException;
import com.delta.dms.community.model.DLInfo;
import com.delta.dms.community.swagger.model.Attachment;
import com.delta.dms.community.swagger.model.AttachmentDetail;
import com.delta.dms.community.swagger.model.DdfRole;
import com.delta.dms.community.swagger.model.DownloadFile;
import com.delta.dms.community.swagger.model.FileConversionStatus;
import com.delta.dms.community.swagger.model.FileIcon;
import com.delta.dms.community.swagger.model.FileType;
import com.delta.dms.community.swagger.model.ForumType;
import com.delta.dms.community.swagger.model.OnlinePDF;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PagingData;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.PreviewData;
import com.delta.dms.community.swagger.model.PreviewDataWithStatus;
import com.delta.dms.community.swagger.model.User;
import com.delta.dms.community.swagger.model.VideoMappingDto;
import com.delta.dms.community.utils.ActivityLogUtil;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.I18nConstants;
import com.delta.dms.community.utils.Utility;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@Transactional
public class FileService {

  private DataHiveStoreService dataHiveStoreService;
  private UserService userService;
  private FileDao fileDao;
  private PrivilegeService privilegeService;
  private DdfDao ddfDao;
  private EventPublishService eventPublishService;
  private StreamingAdapter streamingAdapter;
  private YamlConfig yamlConfig;
  private FileConfig fileConfig;
  private AuthService authService;

  public static final char SYM_EXT = '.';
  public static final String ONLINE_PDF_IMAGE_BASE_HTML =
      "<!DOCTYPE html><html><head><style>#page-container .pf{margin: 13px auto;border-collapse: separate;position: relative;overflow: hidden;}.page-container .pf.image{padding: 0px;line-height: 2px;max-width: 980px;}.page-container .pf.image img{max-width: 100%;}</style></head><body><div id=\"page-container\"></div></body></html>";
  public static final String ONLINE_PDF_IMAGE_CONTENT_FORMAT =
      "<div class=\"pc image\"><img src=\"%s\"></div>";
  public static final Set<PrivilegeType> PRIV_SRD =
      EnumSet.of(
          PrivilegeType.SEARCH,
          PrivilegeType.READ_INFO,
          PrivilegeType.READ_PRIVILEGE,
          PrivilegeType.READ_RAW,
          PrivilegeType.READ_PROTECTED);
  public static final Set<PrivilegeType> PRIV_ALL =
      Arrays.stream(PrivilegeType.values()).collect(Collectors.toSet());
  public static final Set<PrivilegeType> PRIV_PUBLIC_SR =
      EnumSet.of(PrivilegeType.SEARCH, PrivilegeType.READ_INFO, PrivilegeType.READ_PROTECTED);
  public static final Set<DDFSection.Type> DDF_BASE_FIELD = EnumSet.of(DDFSection.Type.BASE);

  private static final String ONLINE_PDF_FIELD_PAGES = "pages";
  private static final String ONLINE_PDF_FIELD_PAGE_NO = "pageNo";
  private static final String ONLINE_PDF_FIELD_CONTENT = "content";
  private static final String ONLINE_PDF_FIELD_TOTAL_PAGE = "totalPage";
  private static final String ONLINE_PDF_FIELD_BASE_HTML = "baseHTML";
  private static final String ONLINE_PDF_IMAGE = "image";
  private static final String MSG_DATAHIVE_FAILED = "DataHive failed";
  private static final LogUtil log = LogUtil.getInstance();

  @Autowired
  public FileService(
      DataHiveStoreService dataHiveStoreService,
      UserService userService,
      FileDao fileDao,
      PrivilegeService privilegeService,
      DdfDao ddfDao,
      EventPublishService eventPublishService,
      StreamingAdapter streamingAdapter,
      YamlConfig yamlConfig,
      FileConfig fileConfig,
      AuthService authService) {
    this.dataHiveStoreService = dataHiveStoreService;
    this.userService = userService;
    this.fileDao = fileDao;
    this.privilegeService = privilegeService;
    this.ddfDao = ddfDao;
    this.eventPublishService = eventPublishService;
    this.streamingAdapter = streamingAdapter;
    this.yamlConfig = yamlConfig;
    this.fileConfig = fileConfig;
    this.authService = authService;
  }

  public String createFile(DDF ddf, String fileName, byte[] data) {
    InputStream inputStream = new ByteArrayInputStream(data);
    DDFID result =
        dataHiveStoreService.createFile(
            new DDFDoc()
                .setDDF(ddf)
                .setData(inputStream, fileName, DataType.RAW)
                .setRawData(new Data(inputStream, fileName, MediaType.MULTIPART_FORM_DATA_VALUE)));
    return Objects.isNull(result) ? StringUtils.EMPTY : result.getUuid();
  }

  private DDFID createFile(DDF ddf, DummyData dummyData) {
    return dataHiveStoreService.createFile(new DDFDoc().setDDF(ddf).setRawData(dummyData));
  }

  public DDF readDdf(String fileId, Set<DDFSection.Type> docField) {
    return dataHiveStoreService.readDdf(fileId, docField);
  }

  public void updateFile(String fileId, DDF ddf, byte[] data) {
    if (data == null) {
      dataHiveStoreService.updateFile(fileId, new DDFDoc().setDDF(ddf));
    } else {
      DDF fileDdf = readDdf(fileId, DDF_BASE_FIELD);
      String fileName =
          Objects.requireNonNull(fileDdf.getBaseSection(), MSG_DATAHIVE_FAILED).getFileName();
      InputStream inputStream = new ByteArrayInputStream(data);
      dataHiveStoreService.updateFile(
          fileId,
          new DDFDoc()
              .setDDF(ddf)
              .setData(inputStream, fileName, DataType.RAW)
              .setRawData(new Data(inputStream, fileName, MediaType.MULTIPART_FORM_DATA_VALUE)));
    }
  }

  public void updateFileWithoutLockChecked(String fileId, DDF ddf) {
    try {
      dataHiveStoreService.updateFile(fileId, new DDFDoc().setDDF(ddf), false);
    } catch (NoSuchElementException e) {
      log.error(Optional.ofNullable(e.getMessage()).orElse(e.toString()));
    }
  }

  public void deleteRealFile(String fileId) {
    List<Tuple<String, BaseSection>> baseSectionList =
        dataHiveStoreService.readBaseSectionList(Collections.singletonList(fileId));
    if (CollectionUtils.isEmpty(baseSectionList)) {
      return;
    }
    if (ServiceType.DELTA_TUBE
        == baseSectionList.get(NumberUtils.INTEGER_ZERO).getVal().getServiceType()) {
      streamingAdapter.deleteMapping(fileId);
    }

    dataHiveStoreService.deleteFile(fileId);
  }

  public void deleteRealFile(List<String> fileIdList) {
    fileIdList.forEach(this::deleteRealFile);
  }

  public JsonNode readOnlinePdf(String fileId, int startPage, int endPage, boolean withMetadata) {
    return dataHiveStoreService.readOnlinePdf(fileId, startPage, endPage, withMetadata);
  }

  public byte[] readRawData(String fileId) {
    try {
      InputStream inputStream = dataHiveStoreService.readRawData(fileId);
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      log.error(e);
      throw new DataHiveException(e.getMessage());
    }
  }

  private byte[] readProtectedData(String fileId, ProtectedType protectedType, ReadArgs args) {
    try {
      InputStream inputStream = dataHiveStoreService.readProtectedData(fileId, protectedType, args);
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      log.error(e);
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
  }

  public String uploadFile(String fileName, byte[] data) {
    DDF ddf = convertToDdf(fileName);
    log.info("upload a general file");
    String ddfId = createFile(ddf, fileName, data);
    log.info(ddfId);
    return ddfId;
  }

  public String uploadVideoFile(
      String fileName, String videoId, String videoLanguage, long videoSize) throws IOException {
    if (!yamlConfig.isAllowUploadDtu()) {
      throw new IllegalArgumentException(I18nConstants.ERR_DTU_UPLOAD_DISABLE);
    }
    if (StringUtils.isAnyEmpty(fileName, videoId, videoLanguage)
        || NumberUtils.LONG_ZERO == videoSize
        || !StringUtils.containsIgnoreCase(
            fileConfig.getExtDtu(), FilenameUtils.getExtension(fileName))) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    DDF ddf = convertToDtuDdf(fileName, videoId);
    DummyData dummyData = new DummyData(fileName, videoSize, null);
    log.info("upload video ddf");
    DDFID ddfId = createFile(ddf, dummyData);
    if (Objects.isNull(ddfId)) {
      throw new HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND);
    }
    try {
      upsertMapping(ddfId.getUuid(), ddfId.getVersion(), videoId, videoLanguage);
      log.info(ddfId);
    } catch (Exception e) {
      log.error(e);
      dataHiveStoreService.deleteFile(ddfId.getUuid());
      throw e;
    }

    return ddfId.getUuid();
  }

  public void upsertMapping(String ddfId, int ddfVersion, String videoId, String videoLanguage)
      throws IOException {
    VideoMappingDto videoMappingDto =
        new VideoMappingDto()
            .app(Constants.INFO_PROJECT_NAME)
            .ddfId(ddfId)
            .ddfVersion(ddfVersion)
            .videoId(videoId);
    videoMappingDto.setVideoLanguage(videoLanguage);
    streamingAdapter.upsertMapping(videoMappingDto);
  }

  private DDF convertToDdf(String fileName) {
    String name = FilenameUtils.getBaseName(fileName);
    String fileExt = FilenameUtils.getExtension(fileName);
    Instant uploadTime = Instant.now();
    DDF ddf =
        new DDF()
            .setBaseSection(
                new BaseSection()
                    .setStatus(DDFStatus.DRAFT)
                    .setName(name)
                    .setDisplayTime(uploadTime)
                    .setUserCreatedDate(uploadTime)
                    .setUserModifiedDate(uploadTime)
                    .setIcon(getIconOfFile(fileExt))
                    .setInternalCategory(
                        isImage(fileExt)
                            ? DdfDocCat.TOPIC_IMAGE.toString()
                            : DdfDocCat.TOPIC_ATTACHMENT.toString())
                    .setServiceType(getServiceTypeByExt(fileExt)))
            .setPrivilegeSection(
                new PrivilegeSection()
                    .setIdMap(
                        getPrivilege(
                            Utility.getUserIdFromSession(), new ArrayList<>(), new ArrayList<>())));
    getRoleMap(Utility.getUserIdFromSession(), new ArrayList<>())
        .entrySet()
        .forEach(item -> ddf.getBaseSection().setPeople(item.getKey(), item.getValue()));
    return ddf;
  }

  private DDF convertToDtuDdf(String fileName, String videoId) {
    DDF ddf = convertToDdf(fileName);
    ddf.getBaseSection().setServiceType(ServiceType.DELTA_TUBE);
    ddf.getBaseSection().setCustom(getDtuCustom(videoId));
    return ddf;
  }

  private String getDtuCustom(String videoId) {
    final ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    objectNode.put(DdfCustomField.VIDEO_ID.toString(), videoId);
    return objectNode.toString();
  }

  private ServiceType getServiceTypeByExt(String fileExt) {
    if (StringUtils.containsIgnoreCase(fileConfig.getExtPdf(), fileExt)) {
      return ServiceType.PDF;
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtOffice(), fileExt)) {
      return ServiceType.OFFICE;
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtTxt(), fileExt)) {
      return ServiceType.TEXT;
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtImg(), fileExt)) {
      return ServiceType.IMAGE;
    } else {
      return ServiceType.FILE;
    }
  }

  private String getIconOfFile(String fileExt) {
    if (StringUtils.containsIgnoreCase(fileConfig.getExtImg(), fileExt)) {
      return FileIcon.IMG.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtExcel(), fileExt)) {
      return FileIcon.EXCEL.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtPpt(), fileExt)) {
      return FileIcon.PPT.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtWord(), fileExt)) {
      return FileIcon.WORD.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtVideo(), fileExt)) {
      return FileIcon.VIDEO.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtPdf(), fileExt)) {
      return FileIcon.PDF.toString();
    } else if (StringUtils.containsIgnoreCase(fileConfig.getExtTxt(), fileExt)) {
      return FileIcon.TXT.toString();
    } else {
      return FileIcon.OTHER.toString();
    }
  }

  public boolean isImage(String fileExt) {
    return StringUtils.containsIgnoreCase(fileConfig.getExtImg(), fileExt);
  }

  public Map<String, List<UserGroupEntity>> getRoleMap(String creator, List<String> author) {
    Map<String, List<UserGroupEntity>> roleMap = new HashMap<>();
    if (!StringUtils.isEmpty(creator)) {
      roleMap.put(
          DdfRole.APPLASSIGNEDCREATOR.toString(), Arrays.asList(new UserGroupEntity(creator)));
    }
    if(!author.isEmpty()) {
      roleMap.put(
              DdfRole.AUTHOR.toString(),
              author.stream().filter(x -> !x.isEmpty()).map(UserGroupEntity::new).collect(Collectors.toList()));
    }
    if (!StringUtils.isEmpty(yamlConfig.getSysAdminGid())) {
      roleMap.put(
              DdfRole.SYSTEMADMIN.toString(),
              Collections.singletonList(new UserGroupEntity(yamlConfig.getSysAdminGid())));
    }
    if (!StringUtils.isEmpty(yamlConfig.getPublicSearchGid())) {
      roleMap.put(
              DdfRole.SEARCHER.toString(),
              Collections.singletonList(new UserGroupEntity(yamlConfig.getPublicSearchGid())));
    }
    return roleMap
        .entrySet()
        .stream()
        .filter(item -> !item.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, Set<PrivilegeType>> getPrivilege(
      String creator, List<String> adminList, List<String> memberList) {
    Map<String, Set<PrivilegeType>> privilegeMap = new HashMap<>();
    memberList.forEach(item -> {
      if(!item.isEmpty()) {
        privilegeMap.put(item, PRIV_SRD);
      }
    });
    adminList.forEach(item -> {
      if(!item.isEmpty()) {
        privilegeMap.put(item, PRIV_ALL);
      }
    });
    if (!StringUtils.isEmpty(creator)) {
      privilegeMap.put(creator, PRIV_ALL);
    }
    return privilegeMap;
  }

  public PreviewDataWithStatus previewFile(
      String fileId, int startPage, int endPage, boolean withMetadata, boolean fromJarvis) {
    if (!fromJarvis) {
      if (!checkUserCanPreview(
          Utility.getCurrentUserIdWithGroupId(), getAttachmentDetail(fileId))) {
        throw new AuthenticationException("");
      }
      publishActivityLogEvent(fileId, Operation.READ, Constants.CONTENT_EMPTY);
    }
    if (withMetadata) {
      return getPreviewFileWithMetadata(fileId, startPage, endPage);
    } else {
      return new PreviewDataWithStatus()
          .data(
              new PreviewData()
                  .authorNames(new ArrayList<>())
                  .editorNames(new ArrayList<>())
                  .onlinePDF(getPreviewFileWithoutMetadata(fileId, startPage, endPage)))
          .status(HttpStatus.SC_OK);
    }
  }

  private boolean checkUserCanPreview(String userId, AttachmentDetail attachment) {
    return privilegeService.checkUserPrivilege(
        userId,
        attachment.getCommunityId(),
        attachment.getForumId(),
        checkPermissionObject(attachment.getForumType()).toString(),
        Operation.READ.toString());
  }

  private PreviewDataWithStatus getPreviewFileWithMetadata(
      String fileId, int startPage, int endPage) {
    DDF ddf = readDdf(fileId, DDF_BASE_FIELD);
    BaseSection baseSection = Objects.requireNonNull(ddf.getBaseSection(), MSG_DATAHIVE_FAILED);
    String creatorUserName =
        StringUtils.defaultString(
            getUserByRole(baseSection.getPeople(), DdfRole.APPLASSIGNEDCREATOR)
                .stream()
                .findFirst()
                .orElse(new UserGroupEntity(""))
                .getName());
    List<String> authorNames =
        getUserByRole(baseSection.getPeople(), DdfRole.AUTHOR)
            .stream()
            .map(item -> StringUtils.defaultString(item.getName()))
            .collect(Collectors.toList());
    PreviewData previewData =
        new PreviewData()
            .name(StringUtils.defaultString(baseSection.getName()))
            .createTime(baseSection.getCreatedTime().toEpochMilli())
            .fileExt(StringUtils.defaultString(baseSection.getFileExt()))
            .createUserName(creatorUserName)
            .authorNames(authorNames)
            .editorNames(new ArrayList<>());
    FileType fileType = getFileTypeByFileExt(previewData.getFileExt());
    OnlinePDF onlinePdf = new OnlinePDF();
    PreviewDataWithStatus previewDataWithStatus;
    if (!ServiceType.DDF_ONLY.equals(baseSection.getServiceType())
        && !ServiceType.FILE.equals(baseSection.getServiceType())) {
      ConverterStatus documentStatus = dataHiveStoreService.checkFileStatus(fileId);
      if (isImage(previewData.getFileExt())) {
        onlinePdf
            .totalPage(1)
            .baseHTML(ONLINE_PDF_IMAGE_BASE_HTML)
            .pages(
                Arrays.asList(
                    new PagingData()
                        .pageNo(0)
                        .content(
                            String.format(
                                ONLINE_PDF_IMAGE_CONTENT_FORMAT, baseSection.getDataUrl()))));
      } else {
        JsonNode onlinePdfData = readOnlinePdf(fileId, startPage, endPage, true);
        if (onlinePdfData.size() != 0) {
          onlinePdf
              .totalPage(onlinePdfData.path(ONLINE_PDF_FIELD_TOTAL_PAGE).asInt(0))
              .baseHTML(
                  Utility.getStringFromJsonNode(onlinePdfData.path(ONLINE_PDF_FIELD_BASE_HTML)))
              .pages(getPagingData(onlinePdfData));
        }
      }
      previewData =
          previewData
              .onlinePDF(onlinePdf)
              .fileType(fileType)
              .asyncStatus(documentStatus.toString());
      previewDataWithStatus =
          new PreviewDataWithStatus().data(previewData).status(HttpStatus.SC_OK);
    } else {
      previewData = previewData.fileType(fileType).asyncStatus(ConverterStatus.SUCCESS.toString());
      previewDataWithStatus =
          new PreviewDataWithStatus().data(previewData).status(HttpStatus.SC_PARTIAL_CONTENT);
    }
    return previewDataWithStatus;
  }

  private List<UserGroupEntity> getUserByRole(
      Map<String, List<UserGroupEntity>> roleMap, DdfRole role) {
    List<UserGroupEntity> userList = new ArrayList<>();
    if (roleMap != null) {
      userList = roleMap.getOrDefault(role.toString(), new ArrayList<>());
    }
    return userList;
  }

  private FileType getFileTypeByFileExt(String fileExt) {
    String imgFileExt = fileConfig.getExtImg();
    String txtFileExt = fileConfig.getExtTxt();
    String officeFileExt = fileConfig.getExtOffice();
    if (!fileExt.isEmpty()) {
      if (StringUtils.containsIgnoreCase(imgFileExt, fileExt)) {
        return FileType.IMG;
      } else if (StringUtils.containsIgnoreCase(txtFileExt, fileExt)) {
        return FileType.TXT;
      } else if (StringUtils.containsIgnoreCase(officeFileExt, fileExt)) {
        return FileType.OFFICE;
      }
    }
    return FileType.OTHERS;
  }

  private List<PagingData> getPagingData(JsonNode pdfData) {
    return StreamSupport.stream(pdfData.path(ONLINE_PDF_FIELD_PAGES).spliterator(), false)
        .map(
            item ->
                new PagingData()
                    .pageNo(item.path(ONLINE_PDF_FIELD_PAGE_NO).asInt())
                    .content(Utility.getStringFromJsonNode(item.path(ONLINE_PDF_FIELD_CONTENT))))
        .collect(Collectors.toList());
  }

  private OnlinePDF getPreviewFileWithoutMetadata(String fileId, int startPage, int endPage) {
    JsonNode pagingData = readOnlinePdf(fileId, startPage, endPage, false);
    OnlinePDF onlinePdf = new OnlinePDF();
    if (pagingData.size() != 0) {
      onlinePdf.pages(getPagingData(pagingData));
    }
    return onlinePdf;
  }

  public DownloadFile downloadFile(String fileId, boolean fromJarvis) {
    if (!fromJarvis && HttpStatus.SC_UNAUTHORIZED == checkDownloadPermission(fileId)) {
      throw new AuthenticationException("");
    }
    DDF ddf = readDdf(fileId, DDF_BASE_FIELD);
    byte[] rawData = readRawData(fileId);
    BaseSection baseSection = Objects.requireNonNull(ddf.getBaseSection(), MSG_DATAHIVE_FAILED);
    publishActivityLogEvent(fileId, Operation.DOWNLOAD, Constants.CONTENT_RAW_FILE);
    return new DownloadFile()
        .name(StringUtils.defaultString(baseSection.getName()))
        .ext(StringUtils.defaultString(baseSection.getFileExt()))
        .data(rawData);
  }

  public Attachment getAttachment(String fileId, boolean withThumbnail) {
    return getAttachmentList(Collections.singletonList(fileId), withThumbnail)
        .stream()
        .findFirst()
        .orElseGet(Attachment::new);
  }

  public List<Attachment> getAttachmentList(List<String> fileIdList, boolean withThumbnail) {
    if (CollectionUtils.isEmpty(fileIdList)) {
      return Collections.emptyList();
    }
    List<Tuple<String, BaseSection>> fileList =
        dataHiveStoreService.readBaseSectionList(
            fileIdList.stream().distinct().collect(Collectors.toList()));
    List<String> dtuFileList =
        fileList
            .parallelStream()
            .filter(item -> ServiceType.DELTA_TUBE.equals(item.getVal().getServiceType()))
            .map(Tuple::getKey)
            .collect(Collectors.toList());
    Map<String, VideoMappingDto> videoInfoList =
        streamingAdapter
            .getVideoInfo(dtuFileList)
            .parallelStream()
            .collect(Collectors.toMap(VideoMappingDto::getDdfId, Function.identity()));
    return Optional.ofNullable(fileList)
        .orElseGet(Collections::emptyList)
        .stream()
        .map(
            item ->
                transferBaseSectionToAttachment(
                    item.getKey(), item.getVal(), withThumbnail, videoInfoList.get(item.getKey())))
        .collect(Collectors.toList());
  }

  private Attachment transferBaseSectionToAttachment(
      String fileId, BaseSection baseSection, boolean withThumbnail, VideoMappingDto videoInfo) {
    videoInfo = Optional.ofNullable(videoInfo).orElseGet(VideoMappingDto::new);
    String fileExt = baseSection.getFileExt();
    Attachment attachment =
        new Attachment()
            .id(fileId)
            .modifiedTime(
                Optional.ofNullable(baseSection.getModifiedTime())
                    .map(Instant::toEpochMilli)
                    .orElseGet(() -> 0L))
            .name(baseSection.getName())
            .fileExt(fileExt)
            .author(
                Optional.ofNullable(baseSection.getPeople())
                    .map(
                        roleMap ->
                            roleMap.getOrDefault(
                                DdfRole.AUTHOR.toString(), Collections.emptyList()))
                    .map(
                        authorList ->
                            userService.getUserByIds(
                                authorList
                                    .parallelStream()
                                    .map(UserGroupEntity::getUuid)
                                    .distinct()
                                    .collect(Collectors.toList())))
                    .orElseGet(ArrayList::new))
            .refUrl(baseSection.getDataUrl())
            .size(baseSection.getSize());
    attachment.setVideoLanguage(videoInfo.getVideoLanguage());
    attachment.setVideoId(videoInfo.getVideoId());

    if (withThumbnail) {
      attachment.setThumbnail(getThumbnail(fileId, isImage(attachment.getFileExt())));
    }
    return attachment;
  }

  private String getThumbnail(String fileId, boolean isImage) {
    String thumbnail = "";
    try {
      if (isImage) {
        thumbnail =
            Base64.getEncoder()
                .encodeToString(readProtectedData(fileId, ProtectedType.IMAGE_SMALL, null));
      } else {
        thumbnail =
            Utility.getStringFromJsonNode(readOnlinePdf(fileId, 0, 1, true).path(ONLINE_PDF_IMAGE));
      }
    } catch (Exception e) {
      log.error(e);
    }
    return thumbnail;
  }

  public FileConversionStatus checkCoversionStatus(String fileId) {
    return new FileConversionStatus().asyncStatus(getFileConvertStatus(fileId).toString());
  }

  private ConverterStatus getFileConvertStatus(String fileId) {
    return dataHiveStoreService.checkFileStatus(fileId);
  }

  public AttachmentDetail getAttachmentDetail(String attachmentId) {
    AttachmentDetail attachmentDetail = fileDao.getAttachmentDetail(attachmentId);
    if (null == attachmentDetail) {
      throw new NoSuchElementException(attachmentId);
    }
    return attachmentDetail;
  }

  public Map<String, BaseSection> getFileBaseSection(List<String> fileIdList) {
    List<Tuple<String, BaseSection>> fileList =
        dataHiveStoreService.readBaseSectionList(
            fileIdList.parallelStream().distinct().collect(Collectors.toList()));
    return Optional.ofNullable(fileList)
        .orElseGet(Collections::emptyList)
        .stream()
        .collect(
            LinkedHashMap::new, (map, item) -> map.put(item.getKey(), item.getVal()), Map::putAll);
  }

  public long getAttachmentTotalSize(List<String> fileIdList, boolean ignoreDtu) {
    if (CollectionUtils.isEmpty(fileIdList)) {
      return 0;
    }
    Map<String, BaseSection> fileList = getFileBaseSection(fileIdList);
    if (ignoreDtu) {
      return fileList
          .values()
          .parallelStream()
          .filter(item -> ServiceType.DELTA_TUBE != item.getServiceType())
          .mapToLong(BaseSection::getSize)
          .sum();
    }
    return fileList.values().parallelStream().mapToLong(BaseSection::getSize).sum();
  }

  public List<String> getAttachmentOfCommunity(int communityId) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    return fileDao.getAttachmentOfCommunity(communityId, dlInfo.isDL, dlInfo.allowForumId);
  }

  public List<String> getOwnAttachmentOfCommunity(
      int communityId,
      boolean isSysAdmin,
      String userId,
      int offset,
      int limit,
      String sortField,
      String sortOrder,
      String fileExt) {
	    //取得DLInfo
		DLInfo dlInfo = authService.getDLUserInfo();
    return fileDao.getOwnAttachmentOfCommunity(
        communityId, isSysAdmin, userId, offset, limit, sortField, sortOrder, fileExt, dlInfo.isDL, dlInfo.allowForumId);
  }

  public int checkDownloadPermission(String fileId) {
    if (checkUserCanDownload(Utility.getCurrentUserIdWithGroupId(), getAttachmentDetail(fileId))) {
      return HttpStatus.SC_OK;
    } else {
      return HttpStatus.SC_UNAUTHORIZED;
    }
  }

  private boolean checkUserCanDownload(String userId, AttachmentDetail attachment) {
    return privilegeService.checkUserPrivilege(
        userId,
        attachment.getCommunityId(),
        attachment.getForumId(),
        checkPermissionObject(attachment.getForumType()).toString(),
        Operation.DOWNLOAD.toString());
  }

  private PermissionObject checkPermissionObject(ForumType forumType) {
    return !ForumType.PRIVATE.equals(forumType)
        ? PermissionObject.PUBLICFORUMATTACHMENT
        : PermissionObject.PRIVATEFORUMATTACHMENT;
  }

  public DownloadFile downloadPdfFile(String fileId, boolean fromJarvis) {
    if (!fromJarvis && HttpStatus.SC_UNAUTHORIZED == checkDownloadPermission(fileId)) {
      throw new AuthenticationException("");
    }
    DDF ddf = readDdf(fileId, DDF_BASE_FIELD);
    publishActivityLogEvent(fileId, Operation.DOWNLOAD, Constants.CONTENT_PDF_FILE);
    return getPdfData(fileId, ddf);
  }

  private DownloadFile getPdfData(String fileId, DDF ddf) {
    BaseSection baseSection = Objects.requireNonNull(ddf.getBaseSection(), MSG_DATAHIVE_FAILED);
    DownloadFile downloadFile =
        new DownloadFile().name(StringUtils.defaultString(baseSection.getName()));
    if (isImage(baseSection.getFileExt())) {
      downloadFile
          .ext(StringUtils.defaultString(baseSection.getFileExt()))
          .data(readProtectedData(fileId, ProtectedType.IMAGE_ORIGINAL, null));
    } else {
      downloadFile.ext(fileConfig.getExtPdf()).data(dataHiveStoreService.downloadPdf(fileId));
    }
    return downloadFile;
  }

  public void delete(String fileId, String fileType, Integer associatedId) {
    String messageJson = "{\"type\":\"" + fileType + "\", \"associatedId\":"
            + (associatedId != null ? associatedId : EMPTY) + "}";
    ddfDao.upsertDdfDeleteQueue(fileId, DdfQueueStatus.WAIT.getValue(), messageJson);
  }

  public void deleteAttachment(List<String> fileIdList, String fileType, Integer topicId) {
    fileIdList.forEach(fileId -> delete(fileId, fileType, topicId));
  }

  private void publishActivityLogEvent(String fileId, Operation operation, String content) {
    Integer topicId = getTopicIdbyAttachmentId(fileId);
    if (topicId != null) {
      String forumType = fileDao.getForumTypeFromTopicbyAttachmentId(fileId);
      PermissionObject permissionObject =
          !ForumType.PRIVATE.equals(ForumType.fromValue(forumType))
              ? PermissionObject.PUBLICFORUMTOPIC
              : PermissionObject.PRIVATEFORUMTOPIC;
      eventPublishService.publishActivityLogEvent(
          Utility.setActivityLogData(
              Utility.getUserIdFromSession(),
              operation.toString(),
              permissionObject.toString(),
              topicId,
              Constants.INFO_PROJECT_NAME,
              content,
              fileId));

      eventPublishService.publishActivityLogMsgEvent(
          ActivityLogUtil.convertToActivityLogMsg(
              App.COMMUNITY,
              Constants.ACTIVITY_APP_VERSION,
              Utility.getUserIdFromSession(),
              ActivityLogUtil.getOperationEnumOfActivityLog(operation),
              ActivityLogUtil.getObjectType(ObjectType.TOPICID, fileId),
              ActivityLogUtil.getObjectId(topicId, fileId),
              ActivityLogUtil.getAnnotation(permissionObject, content),
              LogStatus.SUCCESS,
              LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
              ObjectType.TOPICID,
              String.valueOf(topicId)));
    }

    Integer replyId = getReplyIdbyAttachmentId(fileId);
    if (replyId != null) {
      String forumType = fileDao.getForumTypeFromReplybyAttachmentId(fileId);
      PermissionObject permissionObject =
          !ForumType.PRIVATE.equals(ForumType.fromValue(forumType))
              ? PermissionObject.PUBLICFORUMREPLY
              : PermissionObject.PRIVATEFORUMREPLY;

      eventPublishService.publishActivityLogEvent(
          Utility.setActivityLogData(
              Utility.getUserIdFromSession(),
              operation.toString(),
              permissionObject.toString(),
              replyId,
              Constants.INFO_PROJECT_NAME,
              content,
              fileId));

      eventPublishService.publishActivityLogMsgEvent(
          ActivityLogUtil.convertToActivityLogMsg(
              App.COMMUNITY,
              Constants.ACTIVITY_APP_VERSION,
              Utility.getUserIdFromSession(),
              ActivityLogUtil.getOperationEnumOfActivityLog(operation),
              ActivityLogUtil.getObjectType(ObjectType.REPLYID, fileId),
              ActivityLogUtil.getObjectId(replyId, fileId),
              ActivityLogUtil.getAnnotation(permissionObject, content),
              LogStatus.SUCCESS,
              LogTimeUnit.fromString(yamlConfig.getLogTimeUnit()),
              ObjectType.REPLYID,
              String.valueOf(replyId)));
    }
  }

  private Integer getTopicIdbyAttachmentId(String fileId) {
    return fileDao.getTopicIdbyAttachmentId(fileId);
  }

  private Integer getReplyIdbyAttachmentId(String fileId) {
    return fileDao.getReplyIdbyAttachmentId(fileId);
  }

  public void updateBatch(QueryTree<QueryTerm> queryTree, DDF ddf, UpdateAction updateAction) {
    dataHiveStoreService.updateBatch(queryTree, ddf, updateAction);
  }

  public List<User> getAttachmentAuthor(String fileId) {
    return fileDao.getAttachmentAuthor(fileId);
  }

  public int countOwnAttachmentOfCommunity(
      int communityId, boolean isSysAdmin, String userId, String fileExt) {
    //取得DLInfo
	DLInfo dlInfo = authService.getDLUserInfo();
    return fileDao.countOwnAttachmentOfCommunity(communityId, isSysAdmin, userId, fileExt, dlInfo.isDL, dlInfo.allowForumId);
  }

  public void clickFile(String fileId) {
    publishActivityLogEvent(fileId, Operation.CLICK, Constants.CONTENT_EMPTY);
  }

  public boolean isFileTotalSizeValid(long fileTotalSize) {
    return fileConfig.getTotalMaxSize() >= fileTotalSize;
  }

  public Map<String, BaseSection> getFileBaseSectionByCondition(
      QueryTree<QueryTerm> query,
      List<Sorting> order,
      int offset,
      int limit,
      Set<PrivilegeType> privs) {
    return Optional.ofNullable(
            dataHiveStoreService.readBaseSectionList(query, order, offset, limit, privs))
        .orElseGet(Collections::emptyList)
        .stream()
        .collect(
            LinkedHashMap::new, (map, item) -> map.put(item.getKey(), item.getVal()), Map::putAll);
  }
}
