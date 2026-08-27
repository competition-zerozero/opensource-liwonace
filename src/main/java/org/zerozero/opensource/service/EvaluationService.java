package org.zerozero.opensource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.dto.AgentAnswerResult;
import org.zerozero.opensource.dto.EvaluationCaseResult;
import org.zerozero.opensource.dto.EvaluationQuestion;
import org.zerozero.opensource.dto.EvaluationSummary;

@Service
public class EvaluationService {

  private final AgentQueryService agentQueryService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EvaluationService(AgentQueryService agentQueryService) {
    this.agentQueryService = agentQueryService;
  }

  public EvaluationSummary evaluate(Path questionsPath) throws IOException {
    List<EvaluationQuestion> questions =
        objectMapper.readValue(
            Files.readString(questionsPath, StandardCharsets.UTF_8), new TypeReference<>() {});

    List<EvaluationCaseResult> cases =
        questions.stream()
            .map(question -> evaluateCase(questions.indexOf(question) + 1, question))
            .toList();

    int totalCount = cases.size();
    int routingMatchedCount = count(cases, EvaluationCaseResult::routingMatched);
    int executionSucceededCount = count(cases, EvaluationCaseResult::executionSucceeded);
    int answerProvidedCount = count(cases, EvaluationCaseResult::answerProvided);

    return new EvaluationSummary(
        totalCount,
        routingMatchedCount,
        executionSucceededCount,
        answerProvidedCount,
        rate(routingMatchedCount, totalCount),
        rate(executionSucceededCount, totalCount),
        rate(answerProvidedCount, totalCount),
        cases);
  }

  public Path writeMarkdownReport(EvaluationSummary summary, Path outputPath) throws IOException {
    Files.createDirectories(outputPath.getParent());
    Files.writeString(outputPath, markdown(summary), StandardCharsets.UTF_8);
    return outputPath;
  }

  private EvaluationCaseResult evaluateCase(int index, EvaluationQuestion question) {
    String expectedTool = normalizeExpectedTool(question.tool());
    AgentAnswerResult result = agentQueryService.ask(question.q());
    boolean answerProvided = result.answer() != null && !result.answer().isBlank();

    return new EvaluationCaseResult(
        index,
        question.q(),
        expectedTool,
        result.selectedTool(),
        expectedTool.equals(result.selectedTool()),
        result.success(),
        answerProvided,
        result.elapsedMillis(),
        result.answer(),
        result.errorMessage());
  }

  private String normalizeExpectedTool(String tool) {
    if ("knowledge_graph".equals(tool)) {
      return "graph_search";
    }
    return tool;
  }

  private int count(List<EvaluationCaseResult> cases, CasePredicate predicate) {
    return (int) cases.stream().filter(predicate::test).count();
  }

  private double rate(int count, int total) {
    if (total == 0) {
      return 0.0;
    }
    return count * 100.0 / total;
  }

  private String markdown(EvaluationSummary summary) {
    StringBuilder builder = new StringBuilder();
    builder.append("# Company-X Evaluation Report\n\n");
    builder.append("- 생성 시각: ").append(LocalDateTime.now()).append("\n");
    builder.append("- 전체 질문 수: ").append(summary.totalCount()).append("\n");
    builder
        .append("- 라우팅 정확도: ")
        .append(percent(summary.routingAccuracy()))
        .append(" (")
        .append(summary.routingMatchedCount())
        .append("/")
        .append(summary.totalCount())
        .append(")\n");
    builder
        .append("- 실행 성공률: ")
        .append(percent(summary.executionSuccessRate()))
        .append(" (")
        .append(summary.executionSucceededCount())
        .append("/")
        .append(summary.totalCount())
        .append(")\n");
    builder
        .append("- 답변 생성률: ")
        .append(percent(summary.answerProvidedRate()))
        .append(" (")
        .append(summary.answerProvidedCount())
        .append("/")
        .append(summary.totalCount())
        .append(")\n\n");

    builder.append("## 상세 결과\n\n");
    builder.append("| 번호 | 기대 도구 | 선택 도구 | 라우팅 | 실행 | 응답시간 | 질문 | 답변 |\n");
    builder.append("| --- | --- | --- | --- | --- | ---: | --- | --- |\n");
    for (EvaluationCaseResult result : summary.cases()) {
      builder
          .append("| ")
          .append(result.index())
          .append(" | ")
          .append(result.expectedTool())
          .append(" | ")
          .append(result.selectedTool())
          .append(" | ")
          .append(mark(result.routingMatched()))
          .append(" | ")
          .append(mark(result.executionSucceeded()))
          .append(" | ")
          .append(result.elapsedMillis())
          .append("ms | ")
          .append(cell(result.question(), 80))
          .append(" | ")
          .append(cell(result.answer(), 120))
          .append(" |\n");
    }
    builder.append("\n## 실패 케이스\n\n");
    List<EvaluationCaseResult> failures =
        summary.cases().stream()
            .filter(result -> !result.routingMatched() || !result.executionSucceeded())
            .toList();
    if (failures.isEmpty()) {
      builder.append("- 자동 평가 기준에서 실패 케이스가 없습니다.\n");
    } else {
      for (EvaluationCaseResult failure : failures) {
        builder
            .append("- #")
            .append(failure.index())
            .append(" ")
            .append(failure.question())
            .append(" / 기대=")
            .append(failure.expectedTool())
            .append(", 선택=")
            .append(failure.selectedTool());
        if (failure.errorMessage() != null) {
          builder.append(", 오류=").append(failure.errorMessage());
        }
        builder.append("\n");
      }
    }
    builder.append("\n## 수동 검토 항목\n\n");
    builder.append("- 근거 적절성은 검색 결과 원문과 비교해 A/B가 함께 확인합니다.\n");
    builder.append("- 최종 정답률은 자동 실행 결과와 수동 답변 품질 검토를 합산해 산정합니다.\n");
    return builder.toString();
  }

  private String mark(boolean value) {
    return value ? "PASS" : "FAIL";
  }

  private String percent(double value) {
    return "%.1f%%".formatted(value);
  }

  private String cell(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String normalized = value.replace("|", "\\|").replace("\r", " ").replace("\n", " ").strip();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength - 3) + "...";
  }

  @FunctionalInterface
  private interface CasePredicate {
    boolean test(EvaluationCaseResult result);
  }
}
