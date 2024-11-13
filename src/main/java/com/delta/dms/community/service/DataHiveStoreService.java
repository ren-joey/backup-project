package com.delta.dms.community.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.delta.datahive.DDFQuery.QueryTerm;
import com.delta.datahive.DDFQuery.QueryTree;
import com.delta.datahive.api.DDF.BaseSection;
import com.delta.datahive.api.DDF.DDF;
import com.delta.datahive.api.DDF.DDFDoc;
import com.delta.datahive.api.DDF.DDFDocManager;
import com.delta.datahive.api.DDF.DataStatus;
import com.delta.datahive.api.DDF.ModifyResult;
import com.delta.datahive.api.DDF.PrivilegeType;
import com.delta.datahive.api.DDF.ProtectedType;
import com.delta.datahive.api.DDF.ReadArgs;
import com.delta.datahive.api.config.Config;
import com.delta.datahive.data.DDFID;
import com.delta.datahive.data.DDFSection;
import com.delta.datahive.structures.Tuple;
import com.delta.datahive.types.ConverterStatus;
import com.delta.datahive.types.Sorting;
import com.delta.datahive.types.UpdateAction;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.exception.DataHiveException;
import com.delta.dms.community.exception.UnauthorizedException;
import com.delta.dms.community.model.Jwt;
import com.delta.set.utils.LogUtil;
import com.delta.set.utils.RequestId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DataHiveStoreService {

  private YamlConfig yamlConfig;
  private static final LogUtil log = LogUtil.getInstance();
  private static final int DEFAULT_OFFSET = 0;

  @Autowired
  public DataHiveStoreService(YamlConfig yamlConfig) {
    this.yamlConfig = yamlConfig;
  }

  public DDFID createFile(DDFDoc doc) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    ModifyResult result = ddfDocManager.create(doc);
    return result.getId();
  }

  private DDFDocManager makeDdfDocManager() {
    return DDFDocManager.builder()
        .setAppKey(yamlConfig.getAppId())
        .setConfig(
            Config.customConfig()
                .setField(Config.ConfigurableField.CACHE_PATH, yamlConfig.getHiveCachePath()))
        .setRequestId(RequestId.get())
        .setToken(Jwt.get())
        .build();
  }

  public DDF readDdf(String fileId, Set<DDFSection.Type> docField) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      return ddfDocManager.readDDF(fileId, docField);
    } catch (AccessDeniedException | NoSuchFileException e) {
      log.error(e);
      return new DDF();
    }
  }

  public InputStream readRawData(String fileId) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      return ddfDocManager.readRawData(fileId);
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (NoSuchFileException e) {
      log.error(e);
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public JsonNode readOnlinePdf(String fileId, int startPage, int endPage, boolean withMetadata) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    InputStream protectedData = null;
    try {
      DataStatus status = ddfDocManager.queryStatus(fileId);
      ConverterStatus converterStatus = status.getConverterStatus();
      if (ConverterStatus.SUCCESS.equals(converterStatus)) {
        protectedData =
            readProtectedData(
                fileId,
                ProtectedType.JSONVIEW,
                new ReadArgs()
                    .setGetMeta(withMetadata)
                    .setPageStart(startPage)
                    .setPageEnd(endPage));
      }
    } catch (Exception e) {
      log.error(e);
      return new ObjectMapper().createObjectNode();
    }

    try {
      if (protectedData == null) {
        return new ObjectMapper().createObjectNode();
      }
      return new ObjectMapper().readTree(protectedData);
    } catch (IOException e) {
      log.error(e);
      return new ObjectMapper().createObjectNode();
    }
  }

  public InputStream readProtectedData(String fileId, ProtectedType protectedType, ReadArgs args) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      return ddfDocManager.readProtectedData(fileId, protectedType, args);
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (NoSuchFileException e) {
      log.error(e);
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public void updateFile(String fileId, DDFDoc doc) {
    updateFile(fileId, doc, true);
  }

  public void updateFile(String fileId, DDFDoc doc, boolean doLock) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      ddfDocManager.update(fileId, doc, null, doLock);
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (NoSuchFileException e) {
      log.error(e);
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public void deleteFile(String fileId) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      ddfDocManager.delete(fileId);
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (NoSuchFileException e) {
      log.error(e);
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public byte[] downloadPdf(String fileId) {
    try {
      InputStream inputStream = readProtectedData(fileId, ProtectedType.PDF, null);
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      log.error(e);
      throw new DataHiveException(e.getMessage());
    }
  }

  public ConverterStatus checkFileStatus(String fileId) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    DataStatus status;
    try {
      status = ddfDocManager.queryStatus(fileId);
      return status.getConverterStatus();
    } catch (NoSuchFileException e) {
      log.error(e);
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public void updateBatch(QueryTree<QueryTerm> queryTree, DDF ddf, UpdateAction updateAction) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    try {
      ddfDocManager.updateBatch(queryTree, ddf, updateAction);
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (Exception e) {
      log.error(e);
      throw new DataHiveException(e.getMessage());
    }
  }

  public List<Tuple<String, BaseSection>> readBaseSectionList(List<String> fileIdList) {
    if (CollectionUtils.isEmpty(fileIdList)) {
      return Collections.emptyList();
    }
    DDFDocManager ddfDocManager = makeDdfDocManager();
    QueryTree<QueryTerm> query =
        QueryTerm.builder().setField(QueryTerm.Field.UUID).setValues(fileIdList.toArray()).build();
    return ddfDocManager.readSectionList(
        query, new ArrayList<>(), DDFSection.Type.BASE, DEFAULT_OFFSET, fileIdList.size());
  }

  public List<Tuple<String, BaseSection>> readBaseSectionList(
      QueryTree<QueryTerm> query,
      List<Sorting> order,
      int offset,
      int limit,
      Set<PrivilegeType> privs) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    return ddfDocManager.readSectionList(query, order, DDFSection.Type.BASE, offset, limit, privs);
  }

  public String upsertFile(DDFDoc doc, String key, boolean updateExisting, Integer historyCount) {
    DDFDocManager ddfDocManager = makeDdfDocManager();
    ModifyResult result;
    try {
      result = ddfDocManager.upsert(doc, key, updateExisting);
      if (null != historyCount) {
        QueryTree<QueryTerm> queryTree =
            QueryTerm.builder().setField(QueryTerm.Field.UPSERT_KEY).setValues(key).build();
        ddfDocManager.setHistoryCount(queryTree, historyCount);
      }
      DDFID ddfId = result.getId();
      if (ddfId == null) {
        return "";
      } else {
        return ddfId.getUuid();
      }
    } catch (AccessDeniedException e) {
      log.error(e);
      throw new UnauthorizedException(e.getMessage());
    } catch (Exception e) {
      log.error(e);
      throw new DataHiveException(e.getMessage());
    }
  }
}
