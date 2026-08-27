package zerozero.opensource.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import zerozero.opensource.dto.AgentAnswerResult;
import zerozero.opensource.dto.RouteDecision;

@Service
public class AgentQueryService {

  private static final Pattern COMPANY_ENTITY =
      Pattern.compile("(?i)\\b(Client-[A-Z]|Product-[A-Z0-9]+|Project-[A-Z0-9]+)\\b");

  private final QuestionRouterService routerService;
  private final VectorSearchService vectorSearchService;
  private final Nl2SqlService nl2SqlService;
  private final GraphSearchService graphSearchService;
  private final AnswerGenerationService answerGenerationService;
  private final ObjectMapper objectMapper;

  public AgentQueryService(
      QuestionRouterService routerService,
      VectorSearchService vectorSearchService,
      Nl2SqlService nl2SqlService,
      GraphSearchService graphSearchService,
      AnswerGenerationService answerGenerationService,
      ObjectMapper objectMapper) {
    this.routerService = routerService;
    this.vectorSearchService = vectorSearchService;
    this.nl2SqlService = nl2SqlService;
    this.graphSearchService = graphSearchService;
    this.answerGenerationService = answerGenerationService;
    this.objectMapper = objectMapper;
  }

  public AgentAnswerResult ask(String question) {
    long startedAt = System.currentTimeMillis();
    RouteDecision decision = routerService.route(question);
    try {
      Object toolResult = callTool(question, decision.toolName());
      String answer =
          answerGenerationService.generate(question, decision.toolName(), toJson(toolResult));
      return new AgentAnswerResult(
          question,
          decision.toolName(),
          decision.reason(),
          toolResult,
          answer,
          true,
          null,
          elapsedMillis(startedAt));
    } catch (RuntimeException exception) {
      String answer = "질문을 처리하지 못했습니다. 원인: " + exception.getMessage();
      return new AgentAnswerResult(
          question,
          decision.toolName(),
          decision.reason(),
          null,
          answer,
          false,
          exception.getMessage(),
          elapsedMillis(startedAt));
    }
  }

  private Object callTool(String question, String toolName) {
    return switch (toolName) {
      case "nl2sql" -> nl2SqlService.ask(question);
      case "graph_search" ->
          graphSearchService.search(
              extractEntity(question), extractRelation(question), extractDepth(question));
      case "vector_search" -> vectorSearchService.search(question, 5);
      default -> throw new IllegalArgumentException("지원하지 않는 도구입니다: " + toolName);
    };
  }

  private String extractEntity(String question) {
    Matcher matcher = COMPANY_ENTITY.matcher(question == null ? "" : question);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return question;
  }

  private String extractRelation(String question) {
    String value = question == null ? "" : question;
    if (value.contains("사용") || value.contains("제품")) {
      return "USES";
    }
    if (value.contains("소속") || value.contains("부서")) {
      return "BELONGS_TO";
    }
    if (value.contains("담당") || value.contains("관리")) {
      return "MANAGES_ACCOUNT";
    }
    return null;
  }

  private int extractDepth(String question) {
    String value = question == null ? "" : question;
    if (value.contains("관련 프로젝트") || value.contains("연결된") || value.contains("관계망")) {
      return 2;
    }
    return 1;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalStateException("도구 결과를 JSON으로 변환하지 못했습니다.", exception);
    }
  }

  private long elapsedMillis(long startedAt) {
    return System.currentTimeMillis() - startedAt;
  }
}
