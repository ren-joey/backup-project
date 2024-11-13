package com.delta.dms.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("integration")
public class IntegrationConfig {
    private String authorizedUid;
}
