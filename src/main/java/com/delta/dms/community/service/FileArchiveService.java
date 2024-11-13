package com.delta.dms.community.service;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.MyDmsAdapter;
import com.delta.dms.community.adapter.entity.MyDmsFolder;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.dao.FileArchiveQueueDao;
import com.delta.dms.community.dao.entity.AttachmentInfo;
import com.delta.dms.community.dao.entity.IdNameEntity;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.enums.FileArchiveType;
import com.delta.dms.community.model.CustomByteArrayResource;
import com.delta.dms.community.service.eerp.report.ConclusionReportUtils;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FileArchiveService {

  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final LogUtil log = LogUtil.getInstance();
  private static final String EXT_MP4 = "mp4";
  private static final String FILE_NAME = "%s - 根因改善對策%s";
  private static final String FILE_SUFFIX = "(%s)";

  private final MyDmsAdapter myDmsAdapter;
  private final ReplyService replyService;
  private final FileService fileService;
  private final ConclusionReportUtils conclusionReportUtils;
  private final FileArchiveQueueDao fileArchiveQueueDao;
  private final EerpConfig eerpConfig;

  public void archiveFiles() {
    Map<String, List<String>> fileMap =
        fileArchiveQueueDao
            .getQueue()
            .stream()
            .collect(
                groupingBy(
                    item -> item.getId().toString(), mapping(IdNameEntity::getName, toList())));
    fileMap
        .entrySet()
        .forEach(
            entry ->
                fileArchiveQueueDao.processQueue(FileArchiveType.EERPMHIGHLEVEL, entry.getValue()));
    Map<String, Integer> folderMap =
        myDmsAdapter
            .getOrgFolders(eerpConfig.getMFileArchive().getGid())
            .parallelStream()
            .filter(folder -> eerpConfig.getMFileArchive().getFolderId() == folder.getParentId())
            .collect(toMap(MyDmsFolder::getName, MyDmsFolder::getId));
    fileMap
        .getOrDefault(FileArchiveType.EERPMHIGHLEVEL.toString(), emptyList())
        .forEach(
            id -> {
              try {
                archiveConclusionFiles(folderMap, Integer.valueOf(id));
                fileArchiveQueueDao.deleteQueue(FileArchiveType.EERPMHIGHLEVEL, id);
              } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                fileArchiveQueueDao.resetQueue(FileArchiveType.EERPMHIGHLEVEL, id);
              }
            });
  }

  private void archiveConclusionFiles(Map<String, Integer> folderMap, int conclusionId)
      throws IOException {
    ReplyInfo conclusion = replyService.getReplyInfoById(conclusionId);
    if (Objects.isNull(folderMap.get(conclusion.getForumName()))) {
      int folderId =
          myDmsAdapter.createCustomFolder(
              generateMyDmsFolder(
                  conclusion.getForumName(),
                  eerpConfig.getMFileArchive().getFolderId(),
                  eerpConfig.getMFileArchive().getGid()));
      folderMap.put(conclusion.getForumName(), folderId);
    }
    List<AttachmentInfo> attachments = replyService.getReplyAttachments(conclusionId);
    Optional.ofNullable(attachments)
        .filter(CollectionUtils::isNotEmpty)
        .map(
            list ->
                list.stream()
                    .filter(attachment -> !StringUtils.equals(EXT_MP4, attachment.getFileExt()))
                    .collect(toList()))
        .ifPresent(
            list ->
                IntStream.range(0, list.size())
                    .forEach(
                        i -> {
                          String name =
                              String.format(
                                  FILE_NAME,
                                  list.get(i).getTopicTitle(),
                                  INTEGER_ONE == list.size()
                                      ? EMPTY
                                      : String.format(FILE_SUFFIX, i + 1));
                          list.get(i).setFileName(name);
                          archiveConclusionFile(
                              folderMap.get(conclusion.getForumName()), conclusionId, list.get(i));
                        }));
  }

  private void archiveConclusionFile(int folderId, int conclusionId, AttachmentInfo attachment) {
    try {
      ByteArrayResource resource =
          new CustomByteArrayResource(
              attachment.getFileName().concat(".").concat(attachment.getFileExt()),
              fileService.readRawData(attachment.getAttachmentId()));
      Map<String, List<Object>> tagMap =
          conclusionReportUtils.convertToMyDmsFileTagMap(
              attachment.getFileName(),
              attachment.getCreateUserId(),
              attachment.getCreateUserName(),
              attachment.getAppFieldList());
      conclusionReportUtils.putRecordTypeTag(tagMap, attachment.getRecordType());
      myDmsAdapter.uploadFile(
          resource,
          eerpConfig.getMFileArchive().getGid(),
          folderId,
          attachment.getRecordType(),
          mapper.writeValueAsString(tagMap));
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      fileArchiveQueueDao.updateErrorQueue(
          FileArchiveType.EERPMHIGHLEVEL, Integer.toString(conclusionId));
    }
  }

  private MyDmsFolder generateMyDmsFolder(String name, int parentId, String gid) {
    return new MyDmsFolder().setName(name).setParent_id(parentId).setGid(gid);
  }
}
