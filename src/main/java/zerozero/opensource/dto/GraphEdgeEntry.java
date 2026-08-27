package zerozero.opensource.dto;

import java.util.Map;

public record GraphEdgeEntry(
    String source, String target, String relation, Map<String, Object> properties) {}
