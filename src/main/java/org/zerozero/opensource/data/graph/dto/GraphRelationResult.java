package org.zerozero.opensource.data.graph.dto;

import java.util.Map;
import org.zerozero.opensource.data.graph.domain.GraphNode;

public record GraphRelationResult(
    GraphNode source,
    GraphNode target,
    String relation,
    Map<String, Object> properties,
    int depth) {}
