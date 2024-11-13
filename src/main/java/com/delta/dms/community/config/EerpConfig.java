package com.delta.dms.community.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.config.entity.FileArchive;
import com.delta.dms.community.swagger.model.EerpType;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("eerp")
public class EerpConfig {
  private String adminId;
  private String host;
  private String token;
  private String basePath;
  private String mErrorCodePath;
  private String mCausesPath;
  private String mSolutionPath;
  private String mPushConclusionPath;
  private String mConclusionReportGid;
  private int mConclusionReportFolderId;
  private long mConclusionReportStartTime;
  private long mConclusionReportDuration;
  private long mConclusionReportEffectiveDuration;
  private String chartDefaultColor;
  private Map<Integer, String> chartConclusionColor;
  private Map<Integer, String> chartErrorLevelColor;
  private Map<Integer, String> chartEffectiveSolutionColor;
  private String qBasePath;
  private String qPhenomenonCodePath;
  private String qFailureCodePath;
  private String qDutyCodePath;
  private String qReasonCodePath;
  private String qSolutionCodePath;
  private String pPushConclusionPath;
  private long pConclusionReportStartTime;
  private long pConclusionReportDuration;
  private long pConclusionReportEffectiveDuration;
  private Map<String, Integer> pConclusionReportPath;
  private Map<String, List<String>> pReportContactMail;
  private Map<String, String> conclusionReportNameFormat;
  private String defaultAppFieldId;
  private Set<String> mRecordType;
  private FileArchive mFileArchive;

  private StringBuilder getBaseUrl() {
    return new StringBuilder().append(host).append(basePath);
  }

  public String getEerpmErrorCodeUrl() {
    return getBaseUrl().append(mErrorCodePath).toString();
  }

  public String getEerpmCausesUrl() {
    return getBaseUrl().append(mCausesPath).toString();
  }

  public String getEerpmSolutionUrl() {
    return getBaseUrl().append(mSolutionPath).toString();
  }

  public String getEerpmPushConclusionUrl() {
    return getBaseUrl().append(mPushConclusionPath).toString();
  }

  public String getEerpChartConclusionColor(int id) {
    return Optional.ofNullable(chartConclusionColor.get(id))
        .orElseThrow(IllegalArgumentException::new);
  }

  public String getEerpChartErrorLevelColor(int id) {
    return Optional.ofNullable(chartErrorLevelColor.get(id))
        .orElseThrow(IllegalArgumentException::new);
  }

  public String getEerpChartEffectiveSolutionColor(int id) {
    return Optional.ofNullable(chartEffectiveSolutionColor.get(id))
        .orElseThrow(IllegalArgumentException::new);
  }

  public StringBuilder getEerpqBaseUrl() {
    return getBaseUrl().append(qBasePath);
  }

  public String getEerpqPhenomenonCodeUrl() {
    return getEerpqBaseUrl().append(qPhenomenonCodePath).toString();
  }

  public String getEerpqFailureCodeUrl() {
    return getEerpqBaseUrl().append(qFailureCodePath).toString();
  }

  public String getEerpqDutyCodeUrl() {
    return getEerpqBaseUrl().append(qDutyCodePath).toString();
  }

  public String getEerpqReasonCodeUrl() {
    return getEerpqBaseUrl().append(qReasonCodePath).toString();
  }

  public String getEerpqSolutionCodeUrl() {
    return getEerpqBaseUrl().append(qSolutionCodePath).toString();
  }

  public String getEerppPushConclusionUrl() {
    return getBaseUrl().append(pPushConclusionPath).toString();
  }

  public int getEerppConclusionReportPath(String gid) {
    return Optional.ofNullable(pConclusionReportPath.get(gid))
        .orElseThrow(IllegalArgumentException::new);
  }

  public List<String> getEerppReportContactMail(String gid) {
    return Optional.ofNullable(pReportContactMail.get(gid))
        .orElseThrow(IllegalArgumentException::new);
  }

  public String getConclusionReportName(EerpType type, String startDate, String endDate) {
    return String.format(
        Optional.ofNullable(conclusionReportNameFormat.get(type.toString()))
            .orElseThrow(IllegalArgumentException::new),
        startDate,
        endDate);
  }
}
