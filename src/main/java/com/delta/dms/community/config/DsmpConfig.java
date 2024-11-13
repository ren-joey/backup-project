package com.delta.dms.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dsmp")
public class DsmpConfig {
    private Integer communityId;
}