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
import org.zerozero.opensource.data.graph.dto.GraphRelationResult;
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

  public List<GraphRelationResult> findAdjacent(
      String nodeId, String direction, String relation, int depth) {
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT e.relation,
                   e.properties AS edge_properties,
                   s.id AS source_id,
                   s.type AS source_type,
                   s.name AS source_name,
                   s.properties AS source_properties,
                   t.id AS target_id,
                   t.type AS target_type,
                   t.name AS target_name,
                   t.properties AS target_properties
            FROM graph_edges e
            JOIN graph_nodes s ON s.id = e.source_id
            JOIN graph_nodes t ON t.id = e.target_id
            WHERE 1 = 1
            """);
    List<Object> params = new ArrayList<>();

    if ("out".equals(direction)) {
      sql.append(" AND e.source_id = ?");
      params.add(nodeId);
    } else if ("in".equals(direction)) {
      sql.append(" AND e.target_id = ?");
      params.add(nodeId);
    } else {
      sql.append(" AND (e.source_id = ? OR e.target_id = ?)");
      params.add(nodeId);
      params.add(nodeId);
    }

    if (relation != null) {
      sql.append(" AND e.relation = ?");
      params.add(relation);
    }

    sql.append(" ORDER BY e.relation, s.name, t.name LIMIT 100");

    return jdbcTemplate.query(
        sql.toString(),
        (resultSet, rowNumber) ->
            new GraphRelationResult(
                node(
                    resultSet.getString("source_id"),
                    resultSet.getString("source_type"),
                    resultSet.getString("source_name"),
                    resultSet.getString("source_properties")),
                node(
                    resultSet.getString("target_id"),
                    resultSet.getString("target_type"),
                    resultSet.getString("target_name"),
                    resultSet.getString("target_properties")),
                resultSet.getString("relation"),
                readProperties(resultSet.getString("edge_properties")),
                depth),
        params.toArray());
  }

  private GraphNode node(String id, String type, String name, String properties) {
    return new GraphNode(id, type, name, readProperties(properties));
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
