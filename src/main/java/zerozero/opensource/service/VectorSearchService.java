package zerozero.opensource.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import zerozero.opensource.dto.DocumentSearchResult;

@Service
public class VectorSearchService {

  private final JdbcTemplate jdbcTemplate;
  private final OllamaEmbeddingService embeddingService;

  public VectorSearchService(JdbcTemplate jdbcTemplate, OllamaEmbeddingService embeddingService) {
    this.jdbcTemplate = jdbcTemplate;
    this.embeddingService = embeddingService;
  }

  public List<DocumentSearchResult> search(String query, Integer topK) {
    int limit = topK == null ? 5 : Math.max(1, Math.min(topK, 20));
    String queryVector = toPgVectorLiteral(embeddingService.embed(query));

    return jdbcTemplate.query(
        """
                        SELECT id,
                               doc_id,
                               chunk_index,
                               LEFT(content, 1200) AS content,
                               metadata::text AS metadata,
                               embedding <=> ?::vector AS distance
                        FROM document_chunks
                        WHERE embedding IS NOT NULL
                        ORDER BY embedding <=> ?::vector
                        LIMIT ?
                        """,
        (rs, rowNum) ->
            new DocumentSearchResult(
                rs.getLong("id"),
                rs.getString("doc_id"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getString("metadata"),
                rs.getDouble("distance")),
        queryVector,
        queryVector,
        limit);
  }

  private String toPgVectorLiteral(List<Double> embedding) {
    return embedding.toString();
  }
}
