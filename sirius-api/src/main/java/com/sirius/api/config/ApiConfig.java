package com.sirius.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SiriusSecurityProperties.class)
public class ApiConfig {
}
