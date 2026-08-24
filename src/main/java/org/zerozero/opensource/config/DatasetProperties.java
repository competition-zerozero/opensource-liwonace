package org.zerozero.opensource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record DatasetProperties(String datasetRoot) {}
