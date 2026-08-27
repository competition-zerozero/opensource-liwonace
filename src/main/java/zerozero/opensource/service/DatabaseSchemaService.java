package zerozero.opensource.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaService {

  private final JdbcTemplate jdbcTemplate;

  public DatabaseSchemaService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String loadPublicSchema() {
    List<Map<String, Object>> columns =
        jdbcTemplate.queryForList(
            """
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name <> 'document_chunks'
                ORDER BY table_name, ordinal_position
                """);

    return columns.stream()
        .collect(Collectors.groupingBy(row -> row.get("table_name").toString()))
        .entrySet()
        .stream()
        .map(
            entry ->
                entry.getKey()
                    + "("
                    + entry.getValue().stream()
                        .map(row -> row.get("column_name") + " " + row.get("data_type"))
                        .collect(Collectors.joining(", "))
                    + ")")
        .collect(Collectors.joining("\n"));
  }
}
