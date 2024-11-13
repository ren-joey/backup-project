package com.delta.dms.community.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Configuration
@ConfigurationProperties("autocomplete")
public class AutocompleteConfig {
  private Map<String, List<String>> userFilters;
}
