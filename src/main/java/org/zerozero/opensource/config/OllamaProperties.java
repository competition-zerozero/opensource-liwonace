package org.zerozero.opensource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
    String baseUrl,
    String embeddingModel,
    int embeddingDimension,
    int connectTimeoutSeconds,
    int readTimeoutSeconds) {}
