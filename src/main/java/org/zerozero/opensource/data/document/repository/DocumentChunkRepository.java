package org.zerozero.opensource.data.document.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.opensource.data.document.domain.DocumentMetadata;
import org.zerozero.opensource.data.document.domain.MarkdownChunker;
import org.zerozero.opensource.data.document.dto.DocumentSearchResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  @Transactional
  public int replaceDocument(
      DocumentMetadata metadata,
      List<MarkdownChunker.Chunk> chunks,
      List<List<Double>> embeddings) {
    if (chunks.size() != embeddings.size()) {
      throw new IllegalArgumentException("문서 chunk와 임베딩 개수가 일치하지 않습니다.");
    }

    jdbcTemplate.update("DELETE FROM document_chunks WHERE doc_id = ?", metadata.id());

    String metadataJson = metadataJson(metadata);
    for (int index = 0; index < chunks.size(); index++) {
      jdbcTemplate.update(
          """
          INSERT INTO document_chunks (doc_id, chunk_index, content, embedding, metadata)
          VALUES (?, ?, ?, ?::vector, ?::jsonb)
          """,
          metadata.id(),
          chunks.get(index).index(),
          chunks.get(index).content(),
          vectorLiteral(embeddings.get(index)),
          metadataJson);
    }
    return chunks.size();
  }

  public List<DocumentSearchResult> searchSimilar(
      List<Double> queryEmbedding,
      int limit,
      double minSimilarity,
      String documentType,
      String productName) {
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT doc_id,
                   chunk_index,
                   content,
                   metadata::text AS metadata,
                   1 - (embedding <=> ?::vector) AS similarity
            FROM document_chunks
            WHERE embedding IS NOT NULL
              AND 1 - (embedding <=> ?::vector) >= ?
            """);
    List<Object> params = new ArrayList<>();
    params.add(vectorLiteral(queryEmbedding));
    params.add(vectorLiteral(queryEmbedding));
    params.add(minSimilarity);

    if (documentType != null) {
      sql.append(" AND metadata ->> 'documentType' = ?");
      params.add(documentType);
    }
    if (productName != null) {
      sql.append(" AND metadata ->> 'title' ILIKE ?");
      params.add("%" + productName + "%");
    }

    sql.append(" ORDER BY embedding <=> ?::vector LIMIT ?");
    params.add(vectorLiteral(queryEmbedding));
    params.add(limit);

    return jdbcTemplate.query(
        sql.toString(),
        (resultSet, rowNumber) ->
            new DocumentSearchResult(
                resultSet.getString("doc_id"),
                resultSet.getInt("chunk_index"),
                resultSet.getString("content"),
                resultSet.getString("metadata"),
                resultSet.getDouble("similarity")),
        params.toArray());
  }

  private String metadataJson(DocumentMetadata metadata) {
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              "documentType", metadata.type(),
              "title", metadata.title(),
              "filename", metadata.filename()));
    } catch (JacksonException exception) {
      throw new IllegalStateException("문서 메타데이터를 JSON으로 변환하지 못했습니다.", exception);
    }
  }

  private String vectorLiteral(List<Double> embedding) {
    return embedding.toString();
  }
}
