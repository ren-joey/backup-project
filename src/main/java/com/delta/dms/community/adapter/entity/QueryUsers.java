package com.delta.dms.community.adapter.entity;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class QueryUsers {
  private List<String> emails;
  private List<String> mids;
  private List<String> names;
  private List<String> samaccounts;
  private List<String> uids;
}
