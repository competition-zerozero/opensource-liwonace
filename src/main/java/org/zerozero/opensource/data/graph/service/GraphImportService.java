package org.zerozero.opensource.data.graph.service;

import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.config.DatasetProperties;
import org.zerozero.opensource.data.graph.domain.GraphEdge;
import org.zerozero.opensource.data.graph.domain.GraphNode;
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

  public record ImportResult(int nodeCount, int edgeCount) {}
}
