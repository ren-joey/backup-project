package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("file")
public class FileConfig {

  private String extImg;
  private String extTxt;
  private String extOffice;
  private String extExcel;
  private String extPpt;
  private String extWord;
  private String extVideo;
  private String extPdf;
  private String extDtu;
  private Long totalMaxSize;
}
