package org.zerozero.opensource.data.graph.domain;

import java.util.Map;

public record GraphNode(String id, String type, String name, Map<String, Object> properties) {}
