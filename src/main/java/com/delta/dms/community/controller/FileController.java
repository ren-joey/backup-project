package com.delta.dms.community.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.delta.dms.community.service.FileService;
import com.delta.dms.community.swagger.controller.FileApi;
import com.delta.dms.community.swagger.model.Attachment;
import com.delta.dms.community.swagger.model.DownloadFile;
import com.delta.dms.community.swagger.model.FileConversionStatus;
import com.delta.dms.community.swagger.model.PreviewData;
import com.delta.dms.community.swagger.model.PreviewDataWithStatus;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "File",
    })
@RestController
public class FileController implements FileApi {

  private ObjectMapper mapper = new ObjectMapper();
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FileService fileService;

  private static final String HEADER_CONTENT_DISPOSITION_FORMAT =
      "attachment; filename=\"%s.%s\";filename*=UTF-8''%s.%s";

  @Autowired
  public FileController(
      FileService fileService, HttpServletRequest request, HttpServletResponse response) {
    this.fileService = fileService;
    this.request = request;
    this.response = response;
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
  public ResponseBean<String> uploadFile(
      @ApiParam(value = "") @Valid @RequestPart(value = "file", required = false)
          MultipartFile file,
      @ApiParam(value = "") @RequestParam(value = "fileName", required = false) String fileName,
      @ApiParam(value = "") @RequestParam(value = "videoId", required = false) String videoId,
      @ApiParam(value = "") @RequestParam(value = "videoLanguage", required = false)
          String videoLanguage,
      @ApiParam(value = "") @RequestParam(value = "videoSize", required = false) Long videoSize)
      throws Exception {
    if (Objects.isNull(file)) {
      return new ResponseBean<>(
          fileService.uploadVideoFile(fileName, videoId, videoLanguage, videoSize));
    }
    return new ResponseBean<>(fileService.uploadFile(file.getOriginalFilename(), file.getBytes()));
  }

  @Override
  public ResponseBean<PreviewData> previewFile(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId,
      @NotNull
          @ApiParam(value = "Start page number", required = true)
          @Valid
          @RequestParam(value = "startPage", required = true)
          Integer startPage,
      @NotNull
          @ApiParam(value = "End page number", required = true)
          @Valid
          @RequestParam(value = "endPage", required = true)
          Integer endPage,
      @NotNull
          @ApiParam(value = "With metadata", required = true)
          @Valid
          @RequestParam(value = "withMetaData", required = true)
          Boolean withMetaData,
      @ApiParam(value = "Whether request is from Jarvis", defaultValue = "false")
          @Valid
          @RequestParam(value = "fromJarvis", required = false, defaultValue = "false")
          Boolean fromJarvis)
      throws Exception {
    PreviewDataWithStatus previewDataWithStatus =
        fileService.previewFile(fileId, startPage, endPage, withMetaData, fromJarvis);
    response.setStatus(previewDataWithStatus.getStatus());
    return new ResponseBean<>(previewDataWithStatus.getData());
  }

  @Override
  public ResponseBean<Void> downloadFile(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId,
      @ApiParam(value = "Whether request is from Jarvis", defaultValue = "false")
          @Valid
          @RequestParam(value = "fromJarvis", required = false, defaultValue = "false")
          Boolean fromJarvis)
      throws Exception {
    DownloadFile downloadFile = fileService.downloadFile(fileId, fromJarvis);
    String fileName =
        URLEncoder.encode(
                new String(downloadFile.getName().getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.toString())
            .replaceAll("\\+", "%20");
    response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        String.format(
            HEADER_CONTENT_DISPOSITION_FORMAT,
            fileName,
            downloadFile.getExt(),
            fileName,
            downloadFile.getExt()));
    response.setHeader(
        HttpHeaders.CONTENT_LENGTH,
        String.valueOf(Optional.ofNullable(downloadFile.getData()).orElse(new byte[0]).length));
    IOUtils.write(downloadFile.getData(), response.getOutputStream());
    response.flushBuffer();
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> checkDownloadPermission(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId)
      throws Exception {
    response.setStatus(fileService.checkDownloadPermission(fileId));
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> downloadPDFFile(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId,
      @ApiParam(value = "Whether request is from Jarvis", defaultValue = "false")
          @Valid
          @RequestParam(value = "fromJarvis", required = false, defaultValue = "false")
          Boolean fromJarvis)
      throws Exception {
    DownloadFile downloadFile = fileService.downloadPdfFile(fileId, fromJarvis);
    String fileName =
        URLEncoder.encode(
                new String(downloadFile.getName().getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.toString())
            .replaceAll("\\+", "%20");
    response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        String.format(
            HEADER_CONTENT_DISPOSITION_FORMAT,
            fileName,
            downloadFile.getExt(),
            fileName,
            downloadFile.getExt()));
    response.setHeader(
        HttpHeaders.CONTENT_LENGTH,
        String.valueOf(Optional.ofNullable(downloadFile.getData()).orElse(new byte[0]).length));
    IOUtils.write(downloadFile.getData(), response.getOutputStream());
    response.flushBuffer();
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Void> deleteFile(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId")
          List<String> fileId)
      throws Exception {
    fileService.deleteRealFile(fileId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<Attachment> getFileDetail(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId,
      @NotNull
          @ApiParam(
              value = "identify request from web or mobile",
              required = true,
              allowableValues = "web, mobile",
              defaultValue = "web")
          @Valid
          @RequestParam(value = "deviceType", required = true, defaultValue = "web")
          String deviceType)
      throws Exception {
    return new ResponseBean<>(fileService.getAttachment(fileId, true));
  }

  @Override
  public ResponseBean<FileConversionStatus> checkConversionStatus(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId)
      throws Exception {
    return new ResponseBean<>(fileService.checkCoversionStatus(fileId));
  }

  @Override
  public ResponseBean<Void> clickFile(
      @ApiParam(value = "Id of the file", required = true) @PathVariable("fileId") String fileId)
      throws Exception {
    fileService.clickFile(fileId);
    return new ResponseBean<>();
  }
}
