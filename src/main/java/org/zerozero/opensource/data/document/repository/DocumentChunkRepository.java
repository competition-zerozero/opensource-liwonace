package org.zerozero.opensource.data.document.repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.zerozero.opensource.data.document.domain.DocumentMetadata;
import org.zerozero.opensource.data.document.domain.MarkdownChunker;
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
