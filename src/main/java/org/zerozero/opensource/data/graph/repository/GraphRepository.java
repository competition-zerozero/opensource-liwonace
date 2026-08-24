package org.zerozero.opensource.data.graph.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.opensource.data.graph.domain.GraphEdge;
import org.zerozero.opensource.data.graph.domain.GraphNode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class GraphRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  @Transactional
  public ImportCount replaceGraph(List<GraphNode> nodes, List<GraphEdge> edges) {
    jdbcTemplate.update("DELETE FROM graph_edges");
    jdbcTemplate.update("DELETE FROM graph_nodes");

    for (GraphNode node : nodes) {
      jdbcTemplate.update(
          """
          INSERT INTO graph_nodes (id, type, name, properties)
          VALUES (?, ?, ?, ?::jsonb)
          """,
          node.id(),
          node.type(),
          node.name(),
          json(node.properties()));
    }

    for (GraphEdge edge : edges) {
      jdbcTemplate.update(
          """
          INSERT INTO graph_edges (source_id, target_id, relation, properties)
          VALUES (?, ?, ?, ?::jsonb)
          """,
          edge.source(),
          edge.target(),
          edge.relation(),
          json(edge.properties()));
    }

    return new ImportCount(nodes.size(), edges.size());
  }

  public List<GraphNode> searchNodes(String name, String type) {
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT id, type, name, properties
            FROM graph_nodes
            WHERE 1 = 1
            """);
    List<Object> params = new ArrayList<>();

    if (name != null) {
      sql.append(" AND name ILIKE ?");
      params.add("%" + name + "%");
    }
    if (type != null) {
      sql.append(" AND type = ?");
      params.add(type);
    }

    sql.append(" ORDER BY type, name LIMIT 20");

    return jdbcTemplate.query(
        sql.toString(),
        (resultSet, rowNumber) ->
            new GraphNode(
                resultSet.getString("id"),
                resultSet.getString("type"),
                resultSet.getString("name"),
                readProperties(resultSet.getString("properties"))),
        params.toArray());
  }

  private String json(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JacksonException exception) {
      throw new IllegalStateException("그래프 속성을 JSON으로 변환하지 못했습니다.", exception);
    }
  }

  private Map<String, Object> readProperties(String value) {
    try {
      return objectMapper.readValue(value, new tools.jackson.core.type.TypeReference<>() {});
    } catch (JacksonException exception) {
      throw new IllegalStateException("그래프 속성을 읽지 못했습니다.", exception);
    }
  }

  public record ImportCount(int nodeCount, int edgeCount) {}
}
