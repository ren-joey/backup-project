package com.delta.dms.community.adapter;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.adapter.entity.MyDmsFolder;
import com.delta.dms.community.adapter.entity.ValueLabel;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MyDmsAdapter {

  private static final LogUtil log = LogUtil.getInstance();
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private static final String FILE = "file";
  private static final String NAME = "name";
  private static final String FILE_NAME = "filename";
  private static final String GID = "gid";
  private static final String TAGS = "tags";
  private static final String FOLDER_ID = "folderId";
  private static final String RECORD_TYPE = "recordType";
  private static final String CHILDREN = "children";

  private AdapterUtil adapterUtil;
  private MyDmsConfig myDmsConfig;

  @Autowired
  public MyDmsAdapter(AdapterUtil adapterUtil, MyDmsConfig myDmsConfig) {
    this.adapterUtil = adapterUtil;
    this.myDmsConfig = myDmsConfig;
  }

  public String uploadFile(ByteArrayResource resource, String gid, int folderId, String tags) {
    return uploadFile(resource, gid, folderId, EMPTY, tags);
  }

  public String uploadFile(
      ByteArrayResource resource, String gid, int folderId, String recordType, String tags) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(FILE, resource);
    body.add(NAME, FilenameUtils.getBaseName(resource.getFilename()));
    body.add(FILE_NAME, resource.getFilename());
    body.add(GID, gid);
    body.add(TAGS, tags);
    body.add(FOLDER_ID, folderId);
    Optional.ofNullable(recordType)
        .filter(StringUtils::isNotEmpty)
        .ifPresent(type -> body.add(RECORD_TYPE, type));
    ResponseEntity<String> response =
        adapterUtil.sendRequestWithLongTimeout(
            myDmsConfig.getFileUploadUrl(),
            HttpMethod.POST,
            adapterUtil.generateHeaderWithCookies(MediaType.MULTIPART_FORM_DATA),
            body,
            null,
            String.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (HttpStatus.OK != response.getStatusCode()) {
      throw new HttpClientErrorException(
          response.getStatusCode(),
          response.getStatusCode().name(),
          StringUtils.defaultString(response.getBody()).getBytes(),
          null);
    }
    return StringUtils.defaultString(adapterUtil.getResponseBody(response));
  }

  public List<ValueLabel> getRecordTypes() {
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            myDmsConfig.getRecordTypeUrl(),
            HttpMethod.GET,
            adapterUtil.generateHeaderWithCookies(),
            null,
            null,
            JsonNode.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    log.debug(response);
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody || HttpStatus.OK != response.getStatusCode()) {
      throw new HttpClientErrorException(response.getStatusCode());
    }
    return StreamSupport.stream(responseBody.path(Constants.RESPONSE_DATA).spliterator(), false)
        .map(
            item -> {
              try {
                return mapper.treeToValue(item, ValueLabel.class);
              } catch (JsonProcessingException e) {
                log.error(e);
                return new ValueLabel();
              }
            })
        .filter(item -> StringUtils.isNotEmpty(item.getValue()))
        .collect(Collectors.toList());
  }

  public List<MyDmsFolder> getOrgFolders(String gid) {
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            myDmsConfig.getOrgFolderUrl(gid),
            HttpMethod.GET,
            adapterUtil.generateHeaderWithCookies(),
            null,
            null,
            JsonNode.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody || HttpStatus.OK != response.getStatusCode()) {
      throw new HttpClientErrorException(response.getStatusCode());
    }
    return StreamSupport.stream(
            responseBody.path(Constants.RESPONSE_DATA).path(CHILDREN).spliterator(), false)
        .map(
            item -> {
              try {
                return mapper.treeToValue(item, MyDmsFolder.class);
              } catch (JsonProcessingException e) {
                log.error(e);
                return new MyDmsFolder();
              }
            })
        .filter(item -> INTEGER_ZERO != item.getId())
        .collect(Collectors.toList());
  }

  public int createCustomFolder(MyDmsFolder folder) throws IOException {
    ResponseEntity<JsonNode> response =
        adapterUtil.sendRequest(
            myDmsConfig.getCreateFolderUrl(),
            HttpMethod.POST,
            adapterUtil.generateHeaderWithCookies(),
            mapper.writeValueAsString(singletonList(folder)),
            null,
            JsonNode.class);
    if (null == response) {
      throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    JsonNode responseBody = adapterUtil.getResponseBody(response);
    if (null == responseBody || HttpStatus.OK != response.getStatusCode()) {
      throw new HttpClientErrorException(response.getStatusCode());
    }

    List<Integer> folderIds =
        StreamSupport.stream(responseBody.path(Constants.RESPONSE_DATA).spliterator(), false)
            .map(
                item -> {
                  try {
                    return mapper.treeToValue(item, Integer.class);
                  } catch (JsonProcessingException e) {
                    log.error(e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return ofNullable(folderIds)
        .filter(CollectionUtils::isNotEmpty)
        .map(list -> list.get(INTEGER_ZERO))
        .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND));
  }
}
