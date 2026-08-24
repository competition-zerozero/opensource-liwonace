package org.zerozero.opensource.data.document.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.config.DocumentProperties;

@Component
public class MarkdownChunker {

  private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+.+");

  private final int maxChunkChars;

  public MarkdownChunker(DocumentProperties properties) {
    if (properties.maxChunkChars() <= 0) {
      throw new IllegalArgumentException("문서 chunk 최대 길이는 0보다 커야 합니다.");
    }
    this.maxChunkChars = properties.maxChunkChars();
  }

  public List<Chunk> chunk(String markdown) {
    List<String> sections = splitByHeading(markdown);
    List<String> chunks = new ArrayList<>();

    for (String section : sections) {
      for (int start = 0; start < section.length(); start += maxChunkChars) {
        chunks.add(section.substring(start, Math.min(start + maxChunkChars, section.length())));
      }
    }

    List<Chunk> result = new ArrayList<>();
    for (int index = 0; index < chunks.size(); index++) {
      result.add(new Chunk(index, chunks.get(index).trim()));
    }
    return result.stream().filter(chunk -> !chunk.content().isBlank()).toList();
  }

  private List<String> splitByHeading(String markdown) {
    List<String> sections = new ArrayList<>();
    StringBuilder current = new StringBuilder();

    for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
      if (HEADING.matcher(line).matches() && !current.toString().isBlank()) {
        sections.add(current.toString());
        current.setLength(0);
      }
      current.append(line).append('\n');
    }

    if (!current.toString().isBlank()) {
      sections.add(current.toString());
    }
    return sections;
  }

  public record Chunk(int index, String content) {}
}
