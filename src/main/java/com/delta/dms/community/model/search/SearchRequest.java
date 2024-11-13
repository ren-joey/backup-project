package com.delta.dms.community.model.search;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort.Order;

public class SearchRequest {

  private String query;
  private Integer pitem;
  private Integer page;
  private Order sort;
  private Map<String, List<String>> filterMap;
  private String searchActivity;
  private String from = "community";

  public SearchRequest query(String query) {
    this.query = query;
    return this;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public SearchRequest pitem(Integer pitem) {
    this.pitem = pitem;
    return this;
  }

  public Integer getPitem() {
    return pitem;
  }

  public void setPitem(Integer pitem) {
    this.pitem = pitem;
  }

  public SearchRequest page(Integer page) {
    this.page = page;
    return this;
  }

  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public SearchRequest sort(Order sort) {
    this.sort = sort;
    return this;
  }

  public Order getSort() {
    return sort;
  }

  public void setSort(Order sort) {
    this.sort = sort;
  }

  public SearchRequest filterMap(Map<String, List<String>> filterMap) {
    this.filterMap = filterMap;
    return this;
  }

  public Map<String, List<String>> getFilterMap() {
    return filterMap;
  }

  public void setFilterMap(Map<String, List<String>> filterMap) {
    this.filterMap = filterMap;
  }

  public SearchRequest searchActivity(String searchActivity) {
    this.searchActivity = searchActivity;
    return this;
  }

  public String getSearchActivity() {
    return searchActivity;
  }

  public void setSearchActivity(String searchActivity) {
    this.searchActivity = searchActivity;
  }

  public SearchRequest from(String from) {
    this.from = from;
    return this;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  @Override
  public int hashCode() {
    return Objects.hash(filterMap, page, pitem, query, searchActivity, sort, from);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (null == obj || getClass() != obj.getClass()) {
      return false;
    }
    SearchRequest other = (SearchRequest) obj;
    return Objects.equals(filterMap, other.filterMap)
        && page == other.page
        && pitem == other.pitem
        && Objects.equals(query, other.query)
        && Objects.equals(searchActivity, other.searchActivity)
        && Objects.equals(sort, other.sort)
        && Objects.equals(from, other.from);
  }

  @Override
  public String toString() {
    return "SearchRequest [q="
        + query
        + ", pitem="
        + pitem
        + ", page="
        + page
        + ", sort="
        + sort
        + ", filter="
        + filterMap
        + ", searchActivity="
        + searchActivity
        + ", from="
        + from
        + "]";
  }
}
