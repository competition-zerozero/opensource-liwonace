package org.zerozero.opensource.tool;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.service.AnswerGenerationService;

@Component
public class AnswerGenerationTool {

  private final AnswerGenerationService answerGenerationService;

  public AnswerGenerationTool(AnswerGenerationService answerGenerationService) {
    this.answerGenerationService = answerGenerationService;
  }

  @McpTool(name = "generate_answer", description = "MCP 도구 실행 결과를 Ollama에 전달해 자연어 답변을 생성합니다.")
  public String generateAnswer(
      @McpToolParam(description = "사용자의 원래 질문입니다.", required = true) String question,
      @McpToolParam(description = "사용한 MCP 도구 이름입니다.", required = true) String toolName,
      @McpToolParam(description = "MCP 도구가 반환한 검색 또는 조회 결과입니다.", required = true)
          String toolResult) {
    return answerGenerationService.generate(question, toolName, toolResult);
  }
}
