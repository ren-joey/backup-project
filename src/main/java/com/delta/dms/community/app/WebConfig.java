package com.delta.dms.community.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.delta.dms.community.converter.StringToEnumConverter;
import com.delta.dms.community.interceptor.AuthenticateInterceptor;
import com.delta.dms.community.interceptor.BasicAuthInterceptor;
import com.delta.dms.community.interceptor.ThreadLocalInterceptor;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static com.delta.dms.community.utils.Constants.REQUEST_LOCALE_PARAM;

@Configuration
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

  private static final String[] AUTHENTICATE_INTERCEPTOR_EXCLUDE_PATH = {
    "/error",
    "/index.html",
    "/auth/login",
    "/auth/logout",
    "/info",
    "/v2/api-docs",
    "/swagger-ui.html/**",
    "/webjars/**",
    "/swagger-resources/**",
    "/innovation/award"
  };

  private static final String[] BASICAUTH_INTERCEPTOR_PATH = {"/innovation/award"};

  @Autowired private AuthenticateInterceptor authenticateInterceptor;
  @Autowired private BasicAuthInterceptor basicAuthInterceptor;
  @Autowired private ThreadLocalInterceptor threadLocalInterceptor;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins("*")
        .allowCredentials(true)
        .allowedMethods(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name())
        .allowedHeaders("*")
        .maxAge(86400);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(authenticateInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(AUTHENTICATE_INTERCEPTOR_EXCLUDE_PATH)
        .order(Ordered.HIGHEST_PRECEDENCE);
    registry
        .addInterceptor(basicAuthInterceptor)
        .addPathPatterns(BASICAUTH_INTERCEPTOR_PATH)
        .order(Ordered.HIGHEST_PRECEDENCE);
    registry
        .addInterceptor(threadLocalInterceptor)
        .addPathPatterns("/**")
        .order(Ordered.HIGHEST_PRECEDENCE);
  }

  @Override
  public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    configurer.enable();
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new StringToEnumConverter());
  }
}
