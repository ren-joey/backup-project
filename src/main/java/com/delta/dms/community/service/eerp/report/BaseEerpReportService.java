package com.delta.dms.community.service.eerp.report;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import com.delta.dms.community.config.EerpConfig;
import com.delta.dms.community.config.MyDmsConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.dao.EerpDao;
import com.delta.dms.community.dao.MailDao;
import com.delta.dms.community.swagger.model.EerpType;
import com.delta.dms.community.utils.Constants;
import com.delta.dms.community.utils.Utility;

public abstract class BaseEerpReportService extends ConclusionReportUtils {

  protected BaseEerpReportService(
      EerpConfig eerpConfig,
      YamlConfig yamlConfig,
      EerpDao eerpDao,
      MyDmsConfig myDmsConfig,
      MailDao mailDao) {
    super(eerpConfig, yamlConfig, eerpDao, myDmsConfig, mailDao);
  }

  public void generateConclusionReportToMyDms(long startTime, long endTime) {
    if (startTime > endTime) {
      throw new IllegalArgumentException(Constants.ERR_INVALID_PARAM);
    }
    if (!StringUtils.equals(eerpConfig.getAdminId(), Utility.getUserIdFromSession())) {
      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }

    uploadConclusionReportToMyDms(startTime, endTime);
  }

  protected abstract EerpType getType();

  protected abstract void uploadConclusionReportToMyDms(long startTime, long endTime);
}
