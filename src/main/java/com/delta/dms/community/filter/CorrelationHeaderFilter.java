package com.delta.dms.community.filter;

import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.delta.dms.community.exception.CommunityExceptionHandler;
import com.delta.set.utils.RequestId;

public class CorrelationHeaderFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String currentCorrId = request.getHeader(RequestId.CORRELATION_ID_HEADER);
      if (!request.getDispatcherType().equals(DispatcherType.ASYNC)) {
        if (currentCorrId == null) {
          RequestId.get();
        } else {
          RequestId.set(currentCorrId);
        }
      }
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      CommunityExceptionHandler.handleFilterException(response, e.getMessage());
    }
  }
}
