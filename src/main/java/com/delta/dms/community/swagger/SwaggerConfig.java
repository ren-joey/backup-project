package com.delta.dms.community.swagger;

import javax.servlet.ServletContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.delta.dms.community.app.CommunityApplication;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  private static final String SWAGGER_TITLE = "DMS Community REST API";
  private static final String SWAGGER_DESC = "DMS Community REST API";
  private static final String SWAGGER_API_VER = "1.0";

  @Bean
  public Docket productApi(ServletContext servletContext) {
    return new Docket(DocumentationType.SWAGGER_2)
        .pathProvider(new RelativePathProvider(servletContext))
        .useDefaultResponseMessages(false)
        .select()
        .apis(RequestHandlerSelectors.basePackage(CommunityApplication.BASE_PACKAGE))
        .paths(PathSelectors.any())
        .build()
        .apiInfo(apiInfo());
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title(SWAGGER_TITLE)
        .description(SWAGGER_DESC)
        .version(SWAGGER_API_VER)
        .build();
  }

  @Bean
  public UiConfiguration uiConfig() {
    return UiConfigurationBuilder.builder()
        .displayRequestDuration(true)
        .validatorUrl(StringUtils.EMPTY)
        .build();
  }
}
