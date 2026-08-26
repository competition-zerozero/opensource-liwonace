package org.zerozero.opensource.data.graph.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.config.DatasetProperties;
import org.zerozero.opensource.data.graph.domain.GraphEdge;
import org.zerozero.opensource.data.graph.domain.GraphNode;
import org.zerozero.opensource.data.graph.dto.GraphRelationResult;
import org.zerozero.opensource.data.graph.repository.GraphRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GraphImportService {

  private final DatasetProperties datasetProperties;
  private final ObjectMapper objectMapper;
  private final GraphRepository graphRepository;

  public ImportResult importGraph() {
    Path graphRoot = datasetRoot().resolve("graph");
    List<GraphNode> nodes = read(graphRoot.resolve("nodes.json"), new TypeReference<>() {});
    List<GraphEdge> edges = read(graphRoot.resolve("edges.json"), new TypeReference<>() {});
    GraphRepository.ImportCount count = graphRepository.replaceGraph(nodes, edges);
    return new ImportResult(count.nodeCount(), count.edgeCount());
  }

  public NodeSearchResult searchNodes(String name, String type) {
    return new NodeSearchResult(graphRepository.searchNodes(blankToNull(name), blankToNull(type)));
  }

  public RelationSearchResult searchRelations(
      String nodeId, int depth, String direction, String relation) {
    if (nodeId == null || nodeId.isBlank()) {
      throw new IllegalArgumentException("nodeId를 입력해야 합니다.");
    }

    String normalizedDirection = normalizeDirection(direction);
    int normalizedDepth = Math.clamp(depth, 1, 2);
    String normalizedNodeId = nodeId.trim();
    String normalizedRelation = blankToNull(relation);
    Map<String, GraphRelationResult> results = new LinkedHashMap<>();

    List<GraphRelationResult> firstDepth =
        graphRepository.findAdjacent(normalizedNodeId, normalizedDirection, normalizedRelation, 1);
    addAll(results, firstDepth);

    if (normalizedDepth == 2) {
      for (GraphRelationResult relationResult : firstDepth) {
        String nextNodeId = nextNodeId(normalizedNodeId, normalizedDirection, relationResult);
        addAll(
            results,
            graphRepository.findAdjacent(nextNodeId, normalizedDirection, normalizedRelation, 2));
      }
    }

    return new RelationSearchResult(new ArrayList<>(results.values()));
  }

  private Path datasetRoot() {
    if (datasetProperties.datasetRoot() == null || datasetProperties.datasetRoot().isBlank()) {
      throw new IllegalStateException("DATASET_ROOT 환경변수가 설정되지 않았습니다.");
    }
    return Path.of(datasetProperties.datasetRoot()).toAbsolutePath().normalize();
  }

  private <T> T read(Path path, TypeReference<T> typeReference) {
    try {
      return objectMapper.readValue(path.toFile(), typeReference);
    } catch (JacksonException exception) {
      throw new IllegalStateException("그래프 JSON을 읽지 못했습니다: " + path, exception);
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeDirection(String direction) {
    String normalized = blankToNull(direction);
    if (normalized == null) {
      return "both";
    }
    if (!List.of("in", "out", "both").contains(normalized)) {
      throw new IllegalArgumentException("direction은 in, out, both 중 하나여야 합니다.");
    }
    return normalized;
  }

  private void addAll(
      Map<String, GraphRelationResult> results, List<GraphRelationResult> relationResults) {
    for (GraphRelationResult relationResult : relationResults) {
      results.putIfAbsent(key(relationResult), relationResult);
    }
  }

  private String key(GraphRelationResult relationResult) {
    return "%s:%s:%s"
        .formatted(
            relationResult.source().id(), relationResult.relation(), relationResult.target().id());
  }

  private String nextNodeId(
      String currentNodeId, String direction, GraphRelationResult relationResult) {
    if ("in".equals(direction)) {
      return relationResult.source().id();
    }
    if ("out".equals(direction)) {
      return relationResult.target().id();
    }
    return currentNodeId.equals(relationResult.source().id())
        ? relationResult.target().id()
        : relationResult.source().id();
  }

  public record ImportResult(int nodeCount, int edgeCount) {}

  public record NodeSearchResult(List<GraphNode> results, int resultCount, boolean empty) {

    public NodeSearchResult(List<GraphNode> results) {
      this(results, results.size(), results.isEmpty());
    }
  }

  public record RelationSearchResult(
      List<GraphRelationResult> results, int resultCount, boolean empty) {

    public RelationSearchResult(List<GraphRelationResult> results) {
      this(results, results.size(), results.isEmpty());
    }
  }
}
