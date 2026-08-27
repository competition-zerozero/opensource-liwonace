package zerozero.opensource.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import zerozero.opensource.config.OllamaProperties;

@Service
public class OllamaChatService {

  private final RestClient restClient;
  private final OllamaProperties properties;

  public OllamaChatService(RestClient ollamaRestClient, OllamaProperties properties) {
    this.restClient = ollamaRestClient;
    this.properties = properties;
  }

  public String chat(String systemPrompt, String userPrompt) {
    Map<?, ?> response =
        restClient
            .post()
            .uri("/api/chat")
            .body(
                Map.of(
                    "model", properties.chatModel(),
                    "stream", false,
                    "messages",
                        List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt))))
            .retrieve()
            .body(Map.class);

    Object message = response == null ? null : response.get("message");
    if (message instanceof Map<?, ?> messageMap
        && messageMap.get("content") instanceof String content) {
      return content.strip();
    }
    throw new IllegalStateException("Ollama 채팅 응답에 message.content 값이 없습니다.");
  }
}
