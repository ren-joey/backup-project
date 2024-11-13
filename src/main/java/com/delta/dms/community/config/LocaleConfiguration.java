package com.delta.dms.community.config;

import com.delta.dms.community.utils.CustomLocaleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleConfiguration  {
    @Bean
    public LocaleResolver localeResolver() {
        return new CustomLocaleResolver();
    }
}
