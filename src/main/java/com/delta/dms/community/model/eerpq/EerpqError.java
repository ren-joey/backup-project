package com.delta.dms.community.model.eerpq;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EerpqError<T> {
  private int totoalSize;
  private int pageNum;
  private int pageSize;
  private List<T> dataList;
}
