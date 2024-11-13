package com.delta.dms.community.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.NotificationService;
import com.delta.dms.community.swagger.controller.NotifyApi;
import com.delta.dms.community.swagger.model.NotificationResultList;
import com.delta.dms.community.swagger.model.PublishMessage;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Api(
    tags = {
      "Notify",
    })
@RestController
public class NotifyController implements NotifyApi {

  private ObjectMapper mapper = new ObjectMapper();
  private NotificationService notificationService;
  private HttpServletRequest request;

  @Autowired
  public NotifyController(NotificationService notificationService, HttpServletRequest request) {
    this.notificationService = notificationService;
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
  public ResponseBean<NotificationResultList> getNotificationOfUser(
      @NotNull
          @ApiParam(value = "Offset", required = true)
          @Valid
          @RequestParam(value = "offset", required = true)
          Integer offset,
      @NotNull
          @ApiParam(value = "Limit", required = true)
          @Valid
          @RequestParam(value = "limit", required = true)
          Integer limit)
      throws Exception {
    return new ResponseBean<>(notificationService.getNotificationResultListOfUser(offset, limit));
  }

  @Override
  public ResponseBean<Void> readNotification(
      @ApiParam(value = "Notification id", required = true) @PathVariable("notificationId")
          Integer notificationId)
      throws Exception {
    notificationService.readNotification(notificationId);
    return new ResponseBean<>();
  }

  @Override
  public ResponseBean<PublishMessage> getNotification(
      @ApiParam(value = "Notification id", required = true) @PathVariable("notificationId")
          Integer notificationId)
      throws Exception {
    return new ResponseBean<>(notificationService.getPublishMessage(notificationId));
  }

  @Override
  public ResponseBean<Void> renewAccessTimeOfNotification() throws Exception {
    notificationService.renewAccessTime();
    return new ResponseBean<>();
  }
}
