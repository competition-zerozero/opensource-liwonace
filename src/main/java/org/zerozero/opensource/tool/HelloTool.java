package org.zerozero.opensource.tool;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class HelloTool {

  @McpTool(name = "hello", description = "MCP 연결 테스트용 인사 메시지를 반환합니다.")
  public String hello(
      @McpToolParam(description = "인사말에 포함할 이름입니다.", required = false) String name) {
    String target = name == null || name.isBlank() ? "MCP" : name;
    return "Hello, " + target + "!";
  }
}
