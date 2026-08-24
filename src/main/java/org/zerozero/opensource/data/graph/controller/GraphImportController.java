package org.zerozero.opensource.data.graph.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerozero.opensource.data.graph.service.GraphImportService;

@RestController
@RequestMapping("/internal/graph")
@RequiredArgsConstructor
public class GraphImportController {

  private final GraphImportService graphImportService;

  @PostMapping("/import")
  public GraphImportService.ImportResult importGraph() {
    return graphImportService.importGraph();
  }
}
