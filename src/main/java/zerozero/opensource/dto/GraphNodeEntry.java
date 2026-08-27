package zerozero.opensource.dto;

import java.util.Map;

public record GraphNodeEntry(String id, String type, String name, Map<String, Object> properties) {}
