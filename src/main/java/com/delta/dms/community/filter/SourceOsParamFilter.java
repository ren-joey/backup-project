package com.delta.dms.community.filter;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.delta.datahive.activitylog.args.SourceOS;
import com.delta.dms.community.exception.CommunityExceptionHandler;
import com.delta.dms.community.model.SourceOsParam;
import com.delta.dms.community.utils.I18nConstants;

public class SourceOsParamFilter extends OncePerRequestFilter {

  public static final String SOURCE_OS_PARAM_FIELD = "Source-OS";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String sourceOsField = request.getHeader(SOURCE_OS_PARAM_FIELD);
      if (Objects.nonNull(sourceOsField)) {
        SourceOsParam.set(getSourceOs(sourceOsField));
      }
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      CommunityExceptionHandler.handleFilterException(response, e.getMessage());
    }
  }

  private SourceOS getSourceOs(String field) {
    SourceOS sourceOs = SourceOS.fromString(field);
    if (sourceOs == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_SOURCE_OS_PROPERTY_NOT_FIT);
    }
    return sourceOs;
  }
}
