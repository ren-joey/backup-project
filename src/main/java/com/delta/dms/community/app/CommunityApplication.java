package com.delta.dms.community.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
@MapperScan("com.delta.dms.community.dao")
@ComponentScan(CommunityApplication.BASE_PACKAGE)
@PropertySource(value = "classpath:env.properties")
public class CommunityApplication extends SpringBootServletInitializer {

  public static final String BASE_PACKAGE = "com.delta.dms.community";

  public static void main(String[] args) {
    new SpringApplicationBuilder(CommunityApplication.class).run(args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(CommunityApplication.class);
  }
}
