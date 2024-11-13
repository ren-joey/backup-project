package com.delta.dms.community.model;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import com.delta.dms.community.swagger.model.SortField;

public class SortParam {

  public static final String SORT_ASC = "+";
  public static final String SORT_DESC = "-";

  private static ThreadLocal<Order> sort =
      ThreadLocal.withInitial(
          () -> new Order(Sort.Direction.DESC, SortField.UPDATETIME.toString()));

  private SortParam() {}

  public static Order get() {
    return sort.get();
  }

  public static void set(Order sortParam) {
    sort.set(sortParam);
  }

  public static void unset() {
    sort.remove();
  }
}
