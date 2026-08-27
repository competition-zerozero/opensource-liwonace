package zerozero.opensource.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SqlSafetyValidator {

  private static final Set<String> BLOCKED_KEYWORDS =
      Set.of(
          "insert",
          "update",
          "delete",
          "drop",
          "alter",
          "create",
          "truncate",
          "grant",
          "revoke",
          "copy",
          "execute",
          "call",
          "merge");

  private static final Pattern LIMIT_PATTERN =
      Pattern.compile("\\blimit\\s+\\d+\\b", Pattern.CASE_INSENSITIVE);

  public String validateAndLimit(String sql) {
    String normalized =
        sql.strip()
            .replaceAll("(?is)^```sql\\s*", "")
            .replaceAll("(?is)^```\\s*", "")
            .replaceAll("(?is)```$", "")
            .replaceAll(";+$", "")
            .strip();

    if (normalized.contains(";")) {
      throw new IllegalArgumentException("한 번에 하나의 SELECT 문만 실행할 수 있습니다.");
    }

    String lower = normalized.toLowerCase(Locale.ROOT);
    if (!lower.startsWith("select") && !lower.startsWith("with")) {
      throw new IllegalArgumentException("SELECT 조회 쿼리만 실행할 수 있습니다.");
    }

    for (String keyword : BLOCKED_KEYWORDS) {
      if (Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
          .matcher(normalized)
          .find()) {
        throw new IllegalArgumentException("허용되지 않은 SQL 키워드입니다: " + keyword);
      }
    }

    if (!LIMIT_PATTERN.matcher(normalized).find()) {
      return normalized + " LIMIT 50";
    }
    return normalized;
  }
}
