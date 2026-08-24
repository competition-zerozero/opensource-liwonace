package org.zerozero.opensource.data.document.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.config.DatasetProperties;
import org.zerozero.opensource.data.document.domain.DocumentMetadata;
import org.zerozero.opensource.data.document.domain.MarkdownChunker;
import org.zerozero.opensource.data.document.repository.DocumentChunkRepository;
import org.zerozero.opensource.data.document.repository.OllamaEmbeddingClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

  private final DatasetProperties datasetProperties;
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

  public record ImportResult(int documentCount, int chunkCount) {}
}
