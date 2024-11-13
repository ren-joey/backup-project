package com.delta.dms.community.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.smb.session.SmbSession;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.stereotype.Service;
import com.delta.dms.community.config.DiaConfig;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

@Service
public class DiaSmbService {

  private static final LogUtil log = LogUtil.getInstance();

  private DiaConfig diaConfig;
  private SmbSessionFactory smbSessionFactory;
  private SmbSession smbSession = null;

  @Autowired
  public DiaSmbService(DiaConfig diaConfig) {
    this.diaConfig = diaConfig;
    initSmbSessionFactory();
  }

  private void initSmbSessionFactory() {
    smbSessionFactory = new SmbSessionFactory();
    smbSessionFactory.setHost(diaConfig.getSmbHost());
    smbSessionFactory.setPort(diaConfig.getSmbPort());
    smbSessionFactory.setDomain(diaConfig.getSmbAuthDomain());
    smbSessionFactory.setUsername(diaConfig.getAdminUsername());
    smbSessionFactory.setPassword(diaConfig.getAdminPassword());
    smbSessionFactory.setShareAndDir(diaConfig.getSmbSharedDir());
  }

  private SmbSession getSmbSession() {
    if (null == smbSession || !smbSession.isOpen()) {
      smbSession = smbSessionFactory.getSession();
    }
    log.debug("SmbSession opened");
    return smbSession;
  }

  public Map<String, Long> listFileNameAndSize(String path) throws IOException {
    SmbSession session = getSmbSession();
    SmbFile[] files = session.list(path);
    log.debug(String.format("%s contains %d files", path, files.length));
    return Arrays.asList(files)
        .parallelStream()
        .filter(this::isFile)
        .collect(Collectors.toMap(SmbFile::getName, SmbFile::getContentLengthLong));
  }

  public byte[] downloadFile(String path, String fileName) throws IOException {
    smbSession = getSmbSession();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    smbSession.read(concatPathAndName(path, fileName), os);
    os.close();
    log.debug(String.format("%s/%s has been downloaded, length: %d", path, fileName, os.size()));
    return os.toByteArray();
  }

  public void close() {
    Optional.ofNullable(smbSession)
        .ifPresent(
            session -> {
              session.close();
              session = null;
            });
  }

  private boolean isFile(SmbFile file) {
    try {
      return file.isFile();
    } catch (SmbException e) {
      log.error(e);
    }
    return false;
  }

  private String concatPathAndName(String path, String fileName) {
    return new StringBuilder(path).append(Constants.SLASH).append(fileName).toString();
  }
}
