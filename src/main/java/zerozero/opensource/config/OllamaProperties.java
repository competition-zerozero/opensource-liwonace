package zerozero.opensource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
        String baseUrl,
        String embeddingModel
) {
    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = "nomic-embed-text";
        }
    }
}
