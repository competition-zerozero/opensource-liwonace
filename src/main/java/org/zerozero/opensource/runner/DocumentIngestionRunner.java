package org.zerozero.opensource.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.zerozero.opensource.config.DocumentIngestionProperties;
import org.zerozero.opensource.service.DocumentIngestionService;

@Component
public class DocumentIngestionRunner implements ApplicationRunner {

  private final DocumentIngestionProperties properties;
  private final DocumentIngestionService ingestionService;

  public DocumentIngestionRunner(
      DocumentIngestionProperties properties, DocumentIngestionService ingestionService) {
    this.properties = properties;
    this.ingestionService = ingestionService;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!properties.enabled()) {
      return;
    }

    int savedCount = ingestionService.ingest(properties.path());
    System.out.println("문서 임베딩 적재 완료: " + savedCount + "개 chunk");
  }
}
