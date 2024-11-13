package com.delta.dms.community.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import com.delta.dms.community.exception.CommunityExceptionHandler;
import com.delta.dms.community.model.SortParam;
import com.delta.dms.community.swagger.model.SortField;
import com.delta.dms.community.utils.I18nConstants;

public class SortParamFilter extends OncePerRequestFilter {

  private static final String SORT_PARAM_FIELD = "sort";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String sortParam = request.getParameter(SORT_PARAM_FIELD);
      if (sortParam != null) {
        SortParam.set(parseSort(sortParam));
      }
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      CommunityExceptionHandler.handleFilterException(response, e.getMessage());
    }
  }

  private Order parseSort(String sort) {
    Direction sortOrder = getOrder(sort.substring(0, 1));
    SortField sortField = getSortField(sort.substring(1));
    return new Order(sortOrder, sortField.toString());
  }

  private Direction getOrder(String prefix) {
    if (prefix.equals(SortParam.SORT_ASC)) {
      return Direction.ASC;
    } else if (prefix.equals(SortParam.SORT_DESC)) {
      return Direction.DESC;
    } else {
      throw new IllegalArgumentException(I18nConstants.MSG_SORT_DIRECTION_NOT_FIT);
    }
  }

  private SortField getSortField(String field) {
    SortField sortField = SortField.fromValue(field);
    if (sortField == null) {
      throw new IllegalArgumentException(I18nConstants.MSG_SORT_PROPERTY_NOT_FIT);
    }
    return sortField;
  }
}
