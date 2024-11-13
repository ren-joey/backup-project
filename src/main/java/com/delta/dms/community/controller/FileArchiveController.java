package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.FileArchiveService;
import com.delta.dms.community.swagger.controller.FileArchiveApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;

@Api(
    tags = {
      "FileArchive",
    })
@RestController
public class FileArchiveController implements FileArchiveApi {

  private ObjectMapper mapper = new ObjectMapper();
  private FileArchiveService fileArchiveService;
  private HttpServletRequest request;

  @Autowired
  public FileArchiveController(FileArchiveService fileArchiveService, HttpServletRequest request) {
    this.fileArchiveService = fileArchiveService;
    this.request = request;
  }

  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return Optional.ofNullable(mapper);
  }

  @Override
  public Optional<HttpServletRequest> getRequest() {
    return Optional.ofNullable(request);
  }

  @Override
  public ResponseBean<Void> archiveFiles() throws Exception {
    fileArchiveService.archiveFiles();
    return new ResponseBean<>();
  }
}
