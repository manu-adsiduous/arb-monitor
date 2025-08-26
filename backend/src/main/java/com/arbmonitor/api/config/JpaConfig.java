package com.arbmonitor.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.arbmonitor.api.repository")
public class JpaConfig {
    // JPA configuration for enabling auditing and repository scanning
}

