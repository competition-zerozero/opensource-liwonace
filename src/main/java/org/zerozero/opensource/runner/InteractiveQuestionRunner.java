package org.zerozero.opensource.runner;

import java.util.Scanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.dto.AgentAnswerResult;
import org.zerozero.opensource.service.AgentQueryService;

@Component
public class InteractiveQuestionRunner implements ApplicationRunner {

  private final AgentQueryService agentQueryService;
  private final boolean enabled;

  public InteractiveQuestionRunner(
      AgentQueryService agentQueryService, @Value("${app.cli.enabled:false}") boolean enabled) {
    this.agentQueryService = agentQueryService;
    this.enabled = enabled;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }

    Scanner scanner = new Scanner(System.in);
    System.out.println("Company-X 질문을 입력하세요. 종료하려면 exit 또는 quit을 입력하세요.");
    while (true) {
      System.out.print("> ");
      if (!scanner.hasNextLine()) {
        return;
      }
      String question = scanner.nextLine().strip();
      if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
        return;
      }
      if (question.isBlank()) {
        continue;
      }
      AgentAnswerResult result = agentQueryService.ask(question);
      System.out.println("[선택 도구] " + result.selectedTool());
      System.out.println("[선택 이유] " + result.routingReason());
      System.out.println("[성공 여부] " + result.success());
      System.out.println("[응답 시간] " + result.elapsedMillis() + "ms");
      System.out.println("[답변] " + result.answer());
    }
  }
}
