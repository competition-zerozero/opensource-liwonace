package zerozero.opensource.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import zerozero.opensource.dto.GraphSearchResult;

@Service
public class GraphSearchService {

  private final JdbcTemplate jdbcTemplate;

  public GraphSearchService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public GraphSearchResult search(String entity, String relation, Integer depth) {
    int safeDepth = depth == null ? 1 : Math.max(1, Math.min(depth, 2));
    String relationFilter = relation == null || relation.isBlank() ? null : relation;

    List<GraphSearchResult.GraphNodeResult> nodes =
        jdbcTemplate.query(
            """
                        WITH RECURSIVE exact_seed AS (
                            SELECT id, type, name, properties, 0 AS level
                            FROM graph_nodes
                            WHERE lower(id) = lower(?)
                               OR lower(name) = lower(?)
                        ),
                        seed AS (
                            SELECT * FROM exact_seed
                            UNION ALL
                            SELECT id, type, name, properties, 0 AS level
                            FROM graph_nodes
                            WHERE NOT EXISTS (SELECT 1 FROM exact_seed)
                              AND (id ILIKE '%' || ? || '%'
                               OR name ILIKE '%' || ? || '%')
                            LIMIT 10
                        ),
                        walk AS (
                            SELECT * FROM seed
                            UNION
                            SELECT n.id, n.type, n.name, n.properties, w.level + 1
                            FROM walk w
                            JOIN graph_edges e ON e.source_id = w.id OR e.target_id = w.id
                            JOIN graph_nodes n ON n.id = CASE WHEN e.source_id = w.id THEN e.target_id ELSE e.source_id END
                            WHERE w.level < ?
                              AND (? IS NULL OR e.relation = ?)
                        )
                        SELECT DISTINCT id, type, name, properties::text AS properties
                        FROM walk
                        LIMIT 60
                        """,
            (rs, rowNum) ->
                new GraphSearchResult.GraphNodeResult(
                    rs.getString("id"),
                    rs.getString("type"),
                    rs.getString("name"),
                    rs.getString("properties")),
            entity,
            entity,
            entity,
            entity,
            safeDepth,
            relationFilter,
            relationFilter);

    if (nodes.isEmpty()) {
      return new GraphSearchResult(List.of(), List.of());
    }

    String[] nodeIds =
        nodes.stream().map(GraphSearchResult.GraphNodeResult::id).toArray(String[]::new);

    List<GraphSearchResult.GraphEdgeResult> edges =
        jdbcTemplate.query(
            """
                        SELECT source_id, target_id, relation, properties::text AS properties
                        FROM graph_edges
                        WHERE source_id = ANY (?::text[])
                          AND target_id = ANY (?::text[])
                          AND (? IS NULL OR relation = ?)
                        LIMIT 120
                        """,
            (rs, rowNum) ->
                new GraphSearchResult.GraphEdgeResult(
                    rs.getString("source_id"),
                    rs.getString("target_id"),
                    rs.getString("relation"),
                    rs.getString("properties")),
            (Object) nodeIds,
            (Object) nodeIds,
            relationFilter,
            relationFilter);

    return new GraphSearchResult(nodes, edges);
  }
}
