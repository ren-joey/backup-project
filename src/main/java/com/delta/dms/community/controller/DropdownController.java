package com.delta.dms.community.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.delta.dms.community.service.DropdownService;
import com.delta.dms.community.swagger.controller.DropdownApi;
import com.delta.dms.community.swagger.model.LabelValueDto;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;

@Api(
    tags = {
      "Dropdown",
    })
@RestController
public class DropdownController implements DropdownApi {

  private ObjectMapper mapper = new ObjectMapper();

  private DropdownService dropdownService;
  private HttpServletRequest request;

  @Autowired
  public DropdownController(DropdownService dropdownService, HttpServletRequest request) {
    this.dropdownService = dropdownService;
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
  public ResponseBean<List<LabelValueDto>> getAppFieldDropdownList() throws Exception {
    return new ResponseBean<>(dropdownService.getAppFieldDropdownList());
  }

  @Override
  public ResponseBean<List<LabelValueDto>> getRecordTypeDropdownList() throws Exception {
    return new ResponseBean<>(dropdownService.getRecordDropdownList());
  }
}
