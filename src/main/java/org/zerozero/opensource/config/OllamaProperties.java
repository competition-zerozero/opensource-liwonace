package org.zerozero.opensource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
    String baseUrl,
    String embeddingModel,
    String chatModel,
    int embeddingDimension,
    int connectTimeoutSeconds,
    int readTimeoutSeconds) {
  public OllamaProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "http://localhost:11434";
    }
    if (embeddingModel == null || embeddingModel.isBlank()) {
      embeddingModel = "nomic-embed-text";
    }
    if (chatModel == null || chatModel.isBlank()) {
      chatModel = "gemma3:latest";
    }
    if (embeddingDimension <= 0) {
      embeddingDimension = 768;
    }
    if (connectTimeoutSeconds <= 0) {
      connectTimeoutSeconds = 10;
    }
    if (readTimeoutSeconds <= 0) {
      readTimeoutSeconds = 60;
    }
  }
}
