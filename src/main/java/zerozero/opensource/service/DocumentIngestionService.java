package zerozero.opensource.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import zerozero.opensource.dto.DocumentIndexEntry;

@Service
public class DocumentIngestionService {

    private static final int MAX_CHUNK_LENGTH = 1_200;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OllamaEmbeddingService embeddingService;

    public DocumentIngestionService(
            JdbcTemplate jdbcTemplate,
            OllamaEmbeddingService embeddingService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    public int ingest(Path documentsPath) throws IOException {
        Path indexPath = documentsPath.resolve("index.json");
        List<DocumentIndexEntry> entries = objectMapper.readValue(
                Files.readString(indexPath, StandardCharsets.UTF_8),
                new TypeReference<>() {
                }
        );

        int savedCount = 0;
        for (DocumentIndexEntry entry : entries) {
            Path documentPath = documentsPath.resolve(entry.filename());
            String content = Files.readString(documentPath, StandardCharsets.UTF_8);
            List<String> chunks = splitMarkdown(content);

            jdbcTemplate.update("DELETE FROM document_chunks WHERE doc_id = ?", entry.id());

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String embedding = toPgVectorLiteral(embeddingService.embed(chunk));
                String metadata = objectMapper.writeValueAsString(Map.of(
                        "docId", entry.id(),
                        "type", entry.type(),
                        "title", entry.title(),
                        "filename", entry.filename()
                ));

                jdbcTemplate.update("""
                                INSERT INTO document_chunks (doc_id, chunk_index, content, embedding, metadata)
                                VALUES (?, ?, ?, ?::vector, ?::jsonb)
                                """,
                        entry.id(),
                        i,
                        chunk,
                        embedding,
                        metadata
                );
                savedCount++;
            }
        }
        return savedCount;
    }

    private List<String> splitMarkdown(String content) {
        List<String> chunks = new ArrayList<>();
        String[] sections = content.split("(?m)(?=^## )");

        for (String section : sections) {
            String normalized = section.strip();
            if (normalized.isBlank()) {
                continue;
            }
            if (normalized.length() <= MAX_CHUNK_LENGTH) {
                chunks.add(normalized);
                continue;
            }
            for (int start = 0; start < normalized.length(); start += MAX_CHUNK_LENGTH) {
                int end = Math.min(start + MAX_CHUNK_LENGTH, normalized.length());
                chunks.add(normalized.substring(start, end).strip());
            }
        }

        if (chunks.isEmpty()) {
            chunks.add(content.strip());
        }
        return chunks;
    }

    private String toPgVectorLiteral(List<Double> embedding) {
        return embedding.toString();
    }
}
