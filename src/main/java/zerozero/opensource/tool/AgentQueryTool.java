package zerozero.opensource.tool;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import zerozero.opensource.dto.AgentAnswerResult;
import zerozero.opensource.service.AgentQueryService;

@Component
public class AgentQueryTool {

  private final AgentQueryService agentQueryService;

  public AgentQueryTool(AgentQueryService agentQueryService) {
    this.agentQueryService = agentQueryService;
  }

  @McpTool(name = "ask_companyx", description = "질문을 분석해 적절한 MCP 도구를 자동 선택하고 최종 답변을 생성합니다.")
  public AgentAnswerResult askCompanyX(
      @McpToolParam(description = "Company-X 데이터에 대해 묻는 자연어 질문입니다.", required = true)
          String question) {
    return agentQueryService.ask(question);
  }
}
