package org.zerozero.opensource.data.document.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.zerozero.opensource.config.DocumentProperties;

class MarkdownChunkerTest {

  private final MarkdownChunker chunker = new MarkdownChunker(new DocumentProperties(100));

  @Test
  void headingSectionsAreSplitIntoChunks() {
    var chunks = chunker.chunk("# 제목\n본문\n\n## 원인\n원인 내용");

    assertThat(chunks).hasSize(2);
    assertThat(chunks.get(0).index()).isZero();
    assertThat(chunks.get(0).content()).contains("# 제목", "본문");
    assertThat(chunks.get(1).content()).contains("## 원인", "원인 내용");
  }

  @Test
  void longSectionsAreSplitByMaximumLength() {
    var chunks = chunker.chunk("# 제목\n" + "a".repeat(250));

    assertThat(chunks).hasSize(3);
    assertThat(chunks)
        .allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(100));
  }
}
