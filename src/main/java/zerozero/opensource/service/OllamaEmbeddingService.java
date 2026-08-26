package zerozero.opensource.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import zerozero.opensource.config.OllamaProperties;

@Service
public class OllamaEmbeddingService {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaEmbeddingService(RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    public List<Double> embed(String text) {
        Map<?, ?> response = restClient.post()
                .uri("/api/embeddings")
                .body(Map.of(
                        "model", properties.embeddingModel(),
                        "prompt", text
                ))
                .retrieve()
                .body(Map.class);

        Object embedding = response == null ? null : response.get("embedding");
        if (!(embedding instanceof List<?> values)) {
            throw new IllegalStateException("Ollama 임베딩 응답에 embedding 값이 없습니다.");
        }

        return values.stream()
                .map(Number.class::cast)
                .map(Number::doubleValue)
                .toList();
    }
}
