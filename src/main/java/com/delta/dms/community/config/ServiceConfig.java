package com.delta.dms.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    private final ActivityLog activityLog = new ActivityLog();

    @Data
    public static class ActivityLog {
        private String searchservice;
    }
}