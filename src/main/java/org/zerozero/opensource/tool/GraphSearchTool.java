package org.zerozero.opensource.tool;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.dto.GraphSearchResult;
import org.zerozero.opensource.service.GraphSearchService;

@Component
public class GraphSearchTool {

  private final GraphSearchService graphSearchService;

  public GraphSearchTool(GraphSearchService graphSearchService) {
    this.graphSearchService = graphSearchService;
  }

  @McpTool(name = "graph_search", description = "Company-X 관계 데이터에서 개체와 연결 관계를 탐색합니다.")
  public GraphSearchResult graphSearch(
      @McpToolParam(description = "검색할 고객, 제품, 직원, 부서, 프로젝트 이름 또는 ID입니다.", required = true)
          String entity,
      @McpToolParam(
              description = "필터링할 관계 유형입니다. 예: USES, BELONGS_TO, MANAGES_ACCOUNT",
              required = false)
          String relation,
      @McpToolParam(description = "탐색 깊이입니다. 기본값은 1이며 최대 2까지 허용합니다.", required = false)
          Integer depth) {
    return graphSearchService.search(entity, relation, depth);
  }
}
