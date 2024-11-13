package com.delta.dms.community.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;

public class CrossDomainFilter extends OncePerRequestFilter {
  private static final LogUtil log = LogUtil.getInstance();
  private static final String ORIGIN = "Origin";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String origin =
        request.getHeader(ORIGIN) == null
            ? Constants.CORS_ALLOW_ORIGIN_ALL
            : request.getHeader(ORIGIN);
    log.debug("doFilterInternal::origin:" + origin);
    response.addHeader(Constants.CORS_ALLOW_ORIGIN, origin);
    log.debug("Sending Header....");
    // CORS "pre-flight" request
    response.addHeader(Constants.CORS_ALLOW_METHODS, Constants.CORS_REQ_METHODS);
    response.addHeader(Constants.CORS_ALLOW_HEADERS, Constants.CORS_REQ_HEADERS);
    response.addHeader(Constants.CORS_MAX_AGE, Constants.CORS_MAX_AGE_7200);
    response.addHeader(Constants.CORS_ALLOW_CREDENTIALS, Constants.CORS_CREDENTIALS);
    filterChain.doFilter(request, response);
  }
}
