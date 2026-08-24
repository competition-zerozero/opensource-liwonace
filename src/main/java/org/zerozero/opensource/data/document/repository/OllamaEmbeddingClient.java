package org.zerozero.opensource.data.document.repository;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.zerozero.opensource.config.OllamaProperties;

@Component
public class OllamaEmbeddingClient {

  private final RestClient restClient;
  private final OllamaProperties properties;

  public OllamaEmbeddingClient(OllamaProperties properties) {
    this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    this.properties = properties;
  }

  public List<Double> embed(String content) {
    try {
      EmbedResponse response =
          restClient
              .post()
              .uri("/api/embed")
              .body(new EmbedRequest(properties.embeddingModel(), List.of(content)))
              .retrieve()
              .body(EmbedResponse.class);

      if (response == null || response.embeddings() == null || response.embeddings().size() != 1) {
        throw new IllegalStateException("Ollama가 올바른 임베딩 응답을 반환하지 않았습니다.");
      }
      List<Double> embedding = response.embeddings().getFirst();
      if (embedding.size() != properties.embeddingDimension()) {
        throw new IllegalStateException(
            "임베딩 차원이 일치하지 않습니다. expected=%d, actual=%d"
                .formatted(properties.embeddingDimension(), embedding.size()));
      }
      return embedding;
    } catch (RestClientResponseException exception) {
      throw new IllegalStateException(
          "Ollama 임베딩 요청에 실패했습니다. status=" + exception.getStatusCode(), exception);
    }
  }

  private record EmbedRequest(String model, List<String> input) {}

  private record EmbedResponse(List<List<Double>> embeddings) {}
}
