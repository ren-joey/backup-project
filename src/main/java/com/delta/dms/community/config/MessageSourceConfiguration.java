package com.delta.dms.community.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import java.nio.charset.StandardCharsets;

@Configuration
public class MessageSourceConfiguration {
    private static final String MESSAGE_SOURCE_STORE_BASENAME = "i18n";
    private static final Integer REFRESH_CACHE_TIME_SECONDS = 3600; // Refresh cache once every hour

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames(MESSAGE_SOURCE_STORE_BASENAME);
        source.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        source.setUseCodeAsDefaultMessage(true);
        source.setCacheSeconds(REFRESH_CACHE_TIME_SECONDS);
        return source;
    }
}
