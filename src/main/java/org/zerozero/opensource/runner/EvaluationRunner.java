package org.zerozero.opensource.runner;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.dto.EvaluationSummary;
import org.zerozero.opensource.service.EvaluationService;

@Component
public class EvaluationRunner implements ApplicationRunner {

  private final EvaluationService evaluationService;
  private final boolean enabled;
  private final String datasetRoot;
  private final String questionsPath;
  private final String outputPath;

  public EvaluationRunner(
      EvaluationService evaluationService,
      @Value("${app.evaluation.enabled:false}") boolean enabled,
      @Value("${app.dataset-root:}") String datasetRoot,
      @Value("${app.evaluation.questions-path:}") String questionsPath,
      @Value("${app.evaluation.output-path:build/reports/companyx-evaluation.md}")
          String outputPath) {
    this.evaluationService = evaluationService;
    this.enabled = enabled;
    this.datasetRoot = datasetRoot;
    this.questionsPath = questionsPath;
    this.outputPath = outputPath;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!enabled) {
      return;
    }

    EvaluationSummary summary = evaluationService.evaluate(resolveQuestionsPath());
    Path reportPath = evaluationService.writeMarkdownReport(summary, Path.of(outputPath));
    System.out.println("[평가 리포트] " + reportPath.toAbsolutePath());
    System.out.println("[라우팅 정확도] %.1f%%".formatted(summary.routingAccuracy()));
    System.out.println("[실행 성공률] %.1f%%".formatted(summary.executionSuccessRate()));
    System.out.println("[답변 생성률] %.1f%%".formatted(summary.answerProvidedRate()));
  }

  private Path resolveQuestionsPath() {
    if (questionsPath != null && !questionsPath.isBlank()) {
      return Path.of(questionsPath);
    }
    if (datasetRoot != null && !datasetRoot.isBlank()) {
      return Path.of(datasetRoot).resolve("questions.json");
    }
    return Path.of("/Users/seoyeong/Desktop/companyx-dataset-v1.0/questions.json");
  }
}
