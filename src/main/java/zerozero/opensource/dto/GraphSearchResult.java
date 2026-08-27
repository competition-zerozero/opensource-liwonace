package zerozero.opensource.dto;

import java.util.List;

public record GraphSearchResult(List<GraphNodeResult> nodes, List<GraphEdgeResult> edges) {
  public record GraphNodeResult(String id, String type, String name, String properties) {}

  public record GraphEdgeResult(String source, String target, String relation, String properties) {}
}
