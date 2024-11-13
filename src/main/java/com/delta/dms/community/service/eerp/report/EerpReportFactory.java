package com.delta.dms.community.service.eerp.report;

import static com.delta.dms.community.utils.Constants.ERR_INVALID_PARAM;
import static java.util.Optional.ofNullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.delta.dms.community.swagger.model.EerpType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class EerpReportFactory {
  private final List<BaseEerpReportService> services;

  private static final Map<EerpType, BaseEerpReportService> serviceMap =
      new EnumMap<>(EerpType.class);

  @PostConstruct
  public void init() {
    services.forEach(service -> serviceMap.put(service.getType(), service));
  }

  public BaseEerpReportService getService(EerpType type) {
    return ofNullable(serviceMap.get(type))
        .orElseThrow(() -> new IllegalArgumentException(ERR_INVALID_PARAM));
  }
}
