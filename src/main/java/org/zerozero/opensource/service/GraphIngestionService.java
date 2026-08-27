package org.zerozero.opensource.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.dto.GraphEdgeEntry;
import org.zerozero.opensource.dto.GraphNodeEntry;

@Service
public class GraphIngestionService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public GraphIngestionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public GraphIngestionResult ingest(Path graphPath) throws IOException {
    createTables();

    List<GraphNodeEntry> nodes =
        objectMapper.readValue(
            Files.readString(graphPath.resolve("nodes.json"), StandardCharsets.UTF_8),
            new TypeReference<>() {});
    List<GraphEdgeEntry> edges =
        objectMapper.readValue(
            Files.readString(graphPath.resolve("edges.json"), StandardCharsets.UTF_8),
            new TypeReference<>() {});

    jdbcTemplate.update("DELETE FROM graph_edges");
    jdbcTemplate.update("DELETE FROM graph_nodes");

    for (GraphNodeEntry node : nodes) {
      jdbcTemplate.update(
          """
                            INSERT INTO graph_nodes (id, type, name, properties)
                            VALUES (?, ?, ?, ?::jsonb)
                            """,
          node.id(),
          node.type(),
          node.name(),
          objectMapper.writeValueAsString(
              node.properties() == null ? Map.of() : node.properties()));
    }

    for (GraphEdgeEntry edge : edges) {
      jdbcTemplate.update(
          """
                            INSERT INTO graph_edges (source_id, target_id, relation, properties)
                            VALUES (?, ?, ?, ?::jsonb)
                            """,
          edge.source(),
          edge.target(),
          edge.relation(),
          objectMapper.writeValueAsString(
              edge.properties() == null ? Map.of() : edge.properties()));
    }

    return new GraphIngestionResult(nodes.size(), edges.size());
  }

  private void createTables() {
    jdbcTemplate.execute(
        """
                CREATE TABLE IF NOT EXISTS graph_nodes (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    name TEXT NOT NULL,
                    properties JSONB DEFAULT '{}'::jsonb
                )
                """);
    jdbcTemplate.execute(
        """
                CREATE TABLE IF NOT EXISTS graph_edges (
                    id BIGSERIAL PRIMARY KEY,
                    source_id TEXT NOT NULL REFERENCES graph_nodes(id),
                    target_id TEXT NOT NULL REFERENCES graph_nodes(id),
                    relation TEXT NOT NULL,
                    properties JSONB DEFAULT '{}'::jsonb
                )
                """);
    jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_graph_nodes_name ON graph_nodes(name)");
    jdbcTemplate.execute(
        "CREATE INDEX IF NOT EXISTS idx_graph_edges_source ON graph_edges(source_id)");
    jdbcTemplate.execute(
        "CREATE INDEX IF NOT EXISTS idx_graph_edges_target ON graph_edges(target_id)");
    jdbcTemplate.execute(
        "CREATE INDEX IF NOT EXISTS idx_graph_edges_relation ON graph_edges(relation)");
  }

  public record GraphIngestionResult(int nodeCount, int edgeCount) {}
}
