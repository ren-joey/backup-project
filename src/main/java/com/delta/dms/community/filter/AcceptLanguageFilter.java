package com.delta.dms.community.filter;

import java.io.IOException;
import java.util.Comparator;
import java.util.Locale.LanguageRange;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import com.delta.dms.community.exception.CommunityExceptionHandler;
import com.delta.dms.community.model.AcceptLanguage;
import com.delta.dms.community.utils.I18nConstants;

public class AcceptLanguageFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Optional.ofNullable(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
          .map(
              lang ->
                  LanguageRange.parse(lang)
                      .parallelStream()
                      .sorted(Comparator.comparing(LanguageRange::getRange).reversed())
                      .collect(Collectors.toList()))
          .map(languageList -> languageList.get(NumberUtils.INTEGER_ZERO))
          .map(LanguageRange::getRange)
          .ifPresent(AcceptLanguage::set);
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      CommunityExceptionHandler.handleFilterException(
          response, I18nConstants.MSG_ACCEPT_LNGUAGE_PROPERTY_NOT_FIT);
    }
  }
}
