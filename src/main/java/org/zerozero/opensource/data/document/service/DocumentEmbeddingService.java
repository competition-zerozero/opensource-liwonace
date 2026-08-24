package org.zerozero.opensource.data.document.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.config.DatasetProperties;
import org.zerozero.opensource.config.DocumentProperties;
import org.zerozero.opensource.data.document.domain.DocumentMetadata;
import org.zerozero.opensource.data.document.domain.MarkdownChunker;
import org.zerozero.opensource.data.document.dto.DocumentSearchResult;
import org.zerozero.opensource.data.document.repository.DocumentChunkRepository;
import org.zerozero.opensource.data.document.repository.OllamaEmbeddingClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

  private final DatasetProperties datasetProperties;
  private final DocumentProperties documentProperties;
  private final ObjectMapper objectMapper;
  private final MarkdownChunker chunker;
  private final OllamaEmbeddingClient embeddingClient;
  private final DocumentChunkRepository repository;

  public ImportResult importDocuments() {
    Path datasetRoot = datasetRoot();
    Path documentsRoot = datasetRoot.resolve("documents");
    List<DocumentMetadata> documents = readIndex(documentsRoot.resolve("index.json"));
    int chunkCount = 0;

    for (DocumentMetadata metadata : documents) {
      Path documentPath = documentsRoot.resolve(metadata.filename()).normalize();
      if (!documentPath.startsWith(documentsRoot) || !Files.isRegularFile(documentPath)) {
        throw new IllegalStateException("문서 파일을 찾을 수 없습니다: " + documentPath);
      }

      List<MarkdownChunker.Chunk> chunks = chunker.chunk(readFile(documentPath));
      List<List<Double>> embeddings =
          chunks.stream().map(chunk -> embeddingClient.embed(chunk.content())).toList();
      chunkCount += repository.replaceDocument(metadata, chunks, embeddings);
    }

    return new ImportResult(documents.size(), chunkCount);
  }

  public SearchResult search(SearchRequest request) {
    if (request == null || request.query() == null || request.query().isBlank()) {
      throw new IllegalArgumentException("검색어를 입력해야 합니다.");
    }

    int maxLimit = Math.max(1, documentProperties.maxSearchLimit());
    int defaultLimit = Math.clamp(documentProperties.defaultSearchLimit(), 1, maxLimit);
    int limit = Math.clamp(request.limit() == null ? defaultLimit : request.limit(), 1, maxLimit);
    double minSimilarity =
        Math.clamp(
            request.minSimilarity() == null
                ? documentProperties.minSimilarity()
                : request.minSimilarity(),
            0.0,
            1.0);
    List<Double> queryEmbedding = embeddingClient.embed(request.query());
    return new SearchResult(
        repository.searchSimilar(
            queryEmbedding,
            limit,
            minSimilarity,
            blankToNull(request.documentType()),
            blankToNull(request.productName())));
  }

  private Path datasetRoot() {
    if (datasetProperties.datasetRoot() == null || datasetProperties.datasetRoot().isBlank()) {
      throw new IllegalStateException("DATASET_ROOT 환경변수가 설정되지 않았습니다.");
    }
    return Path.of(datasetProperties.datasetRoot()).toAbsolutePath().normalize();
  }

  private List<DocumentMetadata> readIndex(Path indexPath) {
    try {
      return objectMapper.readValue(indexPath.toFile(), new TypeReference<>() {});
    } catch (JacksonException exception) {
      throw new IllegalStateException("문서 인덱스를 읽지 못했습니다: " + indexPath, exception);
    }
  }

  private String readFile(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new UncheckedIOException("문서를 읽지 못했습니다: " + path, exception);
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record ImportResult(int documentCount, int chunkCount) {}

  public record SearchRequest(
      String query, Integer limit, Double minSimilarity, String documentType, String productName) {}

  public record SearchResult(List<DocumentSearchResult> results) {}
}
