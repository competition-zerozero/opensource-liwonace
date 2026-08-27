package org.zerozero.opensource.tool;

import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.dto.DocumentSearchResult;
import org.zerozero.opensource.service.VectorSearchService;

@Component
public class VectorSearchTool {

  private final VectorSearchService vectorSearchService;

  public VectorSearchTool(VectorSearchService vectorSearchService) {
    this.vectorSearchService = vectorSearchService;
  }

  @McpTool(name = "vector_search", description = "Company-X 문서 데이터를 의미 기반으로 검색합니다.")
  public List<DocumentSearchResult> vectorSearch(
      @McpToolParam(description = "검색할 자연어 질문입니다.", required = true) String query,
      @McpToolParam(description = "반환할 최대 문서 개수입니다. 기본값은 5입니다.", required = false) Integer topK) {
    return vectorSearchService.search(query, topK);
  }
}
