package com.delta.dms.community.exception;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServletExceptionController {
  private static final String SERVLET_ERROR_EXCEPTION = "javax.servlet.error.exception";

  @GetMapping("/servletException")
  public void handleError(HttpServletRequest request) {
    RuntimeException exception = (RuntimeException) request.getAttribute(SERVLET_ERROR_EXCEPTION);
    if (null != exception) {
      throw exception;
    }
  }
}
