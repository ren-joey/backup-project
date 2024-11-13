package com.delta.dms.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties("drc-sync")
public class DrcSyncConfig {
    private String url;
    private String email;
    private String password;
    private String projectId;
    private String collectionId;
    private List<Integer> communityId;
    private String database;
}
