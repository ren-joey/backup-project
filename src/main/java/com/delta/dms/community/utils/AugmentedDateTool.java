package com.delta.dms.community.utils;

import java.util.Date;
import org.apache.velocity.tools.generic.DateTool;

public class AugmentedDateTool extends DateTool {

  public Date epochToDate(long msSinceEpoch) {
    return new Date(msSinceEpoch);
  }
}
