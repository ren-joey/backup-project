package com.delta.dms.community.model;

import com.delta.datahive.activitylog.args.SourceOS;

public class SourceOsParam {

  private static ThreadLocal<SourceOS> sourceOs = ThreadLocal.withInitial(() -> SourceOS.WEB);

  private SourceOsParam() {}

  public static SourceOS get() {
    return sourceOs.get();
  }

  public static void set(SourceOS sourceOsParam) {
    sourceOs.set(sourceOsParam);
  }

  public static void unset() {
    sourceOs.remove();
  }

  public static boolean isMobile() {
    return SourceOS.ANDROID == sourceOs.get() || SourceOS.IOS == sourceOs.get();
  }
}
