package zerozero.opensource.dto;

import java.util.List;
import java.util.Map;

public record SqlQueryResult(String sql, List<Map<String, Object>> rows) {}
