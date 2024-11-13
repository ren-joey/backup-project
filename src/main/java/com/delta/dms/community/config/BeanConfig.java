package com.delta.dms.community.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.delta.dms.community.filter.*;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import com.delta.datahive.searchapi.SearchManager;
import com.delta.dms.community.adapter.CustomRestTemplateCustomizer;

@Configuration
public class BeanConfig {

  private static final String CLASSPATH = "classpath";
  private static final String CLASSPATH_RESOURCE_CLASS = "classpath.resource.loader.class";
  private static final String LANG_CONVERT_FILTER_NAME = "langConvertFilter";
  private static final String LANG_CONVERT_FILTER_URL_PATTERNS = "/*";

  @Bean
  public RestTemplateBuilder restTemplateBuilder(
      CustomRestTemplateCustomizer customRestTemplateCustomizer) {
    return new RestTemplateBuilder(customRestTemplateCustomizer);
  }

  @Bean
  public VelocityEngine velocityEngine() {
    Properties properties = new Properties();
    properties.setProperty(RuntimeConstants.RESOURCE_LOADER, CLASSPATH);
    properties.setProperty(CLASSPATH_RESOURCE_CLASS, ClasspathResourceLoader.class.getName());
    return new VelocityEngine(properties);
  }

  @Bean(name = "textTemplateEngine")
  public SpringTemplateEngine textTemplateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.addTemplateResolver(textTemplateResolver());
    templateEngine.setEnableSpringELCompiler(true);
    return templateEngine;
  }

  private ITemplateResolver textTemplateResolver() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/template/");
    templateResolver.setSuffix(".txt");
    templateResolver.setTemplateMode(TemplateMode.TEXT);
    templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.toString());
    templateResolver.setCheckExistence(true);
    templateResolver.setCacheable(false);
    return templateResolver;
  }

  @Bean
  public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new ShallowEtagHeaderFilter());
    filterRegistrationBean.setName("etag filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<AcceptLanguageFilter> acceptLanguageFilter() {
    FilterRegistrationBean<AcceptLanguageFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new AcceptLanguageFilter());
    filterRegistrationBean.setName("acceptLanguage filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<CorrelationHeaderFilter> correlationHeaderFilter() {
    FilterRegistrationBean<CorrelationHeaderFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new CorrelationHeaderFilter());
    filterRegistrationBean.setName("correlationHeader filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<CrossDomainFilter> crossDomainFilter() {
    FilterRegistrationBean<CrossDomainFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new CrossDomainFilter());
    filterRegistrationBean.setName("crossDomainFilter filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<SortParamFilter> sortParamFilter() {
    FilterRegistrationBean<SortParamFilter> filterRegistrationBean = new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new SortParamFilter());
    filterRegistrationBean.setName("sortParamFilter filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<SourceOsParamFilter> sortSourceOsFilter() {
    FilterRegistrationBean<SourceOsParamFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new SourceOsParamFilter());
    filterRegistrationBean.setName("sourceOsParamFilter filter");
    filterRegistrationBean.addUrlPatterns("/*");
    return filterRegistrationBean;
  }

  @Bean
  public FilterRegistrationBean<LangConvertFilter> langConvertFilter() {
    FilterRegistrationBean<LangConvertFilter> filterRegistrationBean =
            new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new LangConvertFilter());
    filterRegistrationBean.setName(LANG_CONVERT_FILTER_NAME);
    filterRegistrationBean.addUrlPatterns(LANG_CONVERT_FILTER_URL_PATTERNS);
    return filterRegistrationBean;
  }

  @Bean
  public SearchManager searchManager() {
    return new SearchManager();
  }
}
