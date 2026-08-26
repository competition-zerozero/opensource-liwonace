package org.zerozero.opensource.data.graph.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/nodes")
  public GraphImportService.NodeSearchResult searchNodes(
      @RequestParam(required = false) String name, @RequestParam(required = false) String type) {
    return graphImportService.searchNodes(name, type);
  }

  @GetMapping("/relations")
  public GraphImportService.RelationSearchResult searchRelations(
      @RequestParam String nodeId,
      @RequestParam(defaultValue = "1") int depth,
      @RequestParam(defaultValue = "both") String direction,
      @RequestParam(required = false) String relation) {
    return graphImportService.searchRelations(nodeId, depth, direction, relation);
  }
}
