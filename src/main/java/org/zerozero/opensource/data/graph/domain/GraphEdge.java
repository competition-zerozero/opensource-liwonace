package org.zerozero.opensource.data.graph.domain;

import java.util.Map;

public record GraphEdge(
    String source, String target, String relation, Map<String, Object> properties) {}
