package org.zerozero.opensource.data.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.opensource.data.document.service.DocumentEmbeddingService;

@RestController
@RequestMapping("/internal/documents")
@RequiredArgsConstructor
public class DocumentImportController {

  private final DocumentEmbeddingService documentEmbeddingService;

  @PostMapping("/import")
  public DocumentEmbeddingService.ImportResult importDocuments() {
    return documentEmbeddingService.importDocuments();
  }

  @PostMapping("/search")
  public DocumentEmbeddingService.SearchResult searchDocuments(
      @RequestBody DocumentEmbeddingService.SearchRequest request) {
    return documentEmbeddingService.search(request);
  }
}
