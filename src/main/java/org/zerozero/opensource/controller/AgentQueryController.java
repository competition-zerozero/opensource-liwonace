package org.zerozero.opensource.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.opensource.dto.AgentAnswerResult;
import org.zerozero.opensource.dto.SqlQueryResult;
import org.zerozero.opensource.service.AgentQueryService;
import org.zerozero.opensource.service.Nl2SqlService;

@RestController
@RequestMapping("/api")
public class AgentQueryController {

  private final AgentQueryService agentQueryService;
  private final Nl2SqlService nl2SqlService;

  public AgentQueryController(AgentQueryService agentQueryService, Nl2SqlService nl2SqlService) {
    this.agentQueryService = agentQueryService;
    this.nl2SqlService = nl2SqlService;
  }

  @PostMapping("/ask")
  public AgentAnswerResult ask(@RequestBody AskRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      throw new IllegalArgumentException("질문을 입력해야 합니다.");
    }
    return agentQueryService.ask(request.question());
  }

  @PostMapping("/nl2sql")
  public SqlQueryResult nl2Sql(@RequestBody AskRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      throw new IllegalArgumentException("질문을 입력해야 합니다.");
    }
    return nl2SqlService.ask(request.question());
  }

  public record AskRequest(String question) {}
}
