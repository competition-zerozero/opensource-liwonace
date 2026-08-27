package zerozero.opensource.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import zerozero.opensource.dto.SqlQueryResult;

@Service
public class Nl2SqlService {

  private static final Pattern SQL_BLOCK =
      Pattern.compile("```sql\\s*(.*?)\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  private final JdbcTemplate jdbcTemplate;
  private final DatabaseSchemaService schemaService;
  private final OllamaChatService chatService;
  private final SqlSafetyValidator sqlSafetyValidator;

  public Nl2SqlService(
      JdbcTemplate jdbcTemplate,
      DatabaseSchemaService schemaService,
      OllamaChatService chatService,
      SqlSafetyValidator sqlSafetyValidator) {
    this.jdbcTemplate = jdbcTemplate;
    this.schemaService = schemaService;
    this.chatService = chatService;
    this.sqlSafetyValidator = sqlSafetyValidator;
  }

  public SqlQueryResult ask(String question) {
    String generated =
        chatService.chat(systemPrompt(), userPrompt(question, schemaService.loadPublicSchema()));
    String sql = sqlSafetyValidator.validateAndLimit(extractSql(generated));
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
    return new SqlQueryResult(sql, rows);
  }

  private String systemPrompt() {
    return """
                너는 Company-X PostgreSQL 데이터베이스를 조회하는 NL2SQL 변환기다.
                사용자의 한국어 질문을 PostgreSQL SELECT SQL 하나로 변환한다.
                반드시 SQL만 출력한다. 설명, 마크다운, 주석은 출력하지 않는다.
                INSERT, UPDATE, DELETE, DROP, ALTER, CREATE 같은 변경 쿼리는 절대 만들지 않는다.
                스키마에 존재하는 테이블과 컬럼만 사용한다.
                """;
  }

  private String userPrompt(String question, String schema) {
    return """
                [스키마]
                %s

                [업무 규칙]
                - 서울 지역 매출은 sales.region = '서울' 조건을 사용한다.
                - 분기 질문은 sales.quarter 값을 사용한다. 예: 2025년 3분기 = '2025-Q3'
                - 보안 솔루션 카테고리는 sales.category = 'security' 또는 products.category = 'security'를 사용한다.
                - 활성 계약은 contracts.status = 'active' 조건을 사용한다.
                - 부서별 직원 조회는 employees.dept_id = departments.id 조인을 사용한다.
                - 제품별 계약 금액은 contracts.product_id = products.id 조인을 사용한다.

                [예시]
                질문: 2025년 3분기 총 매출액은 얼마야?
                SQL: SELECT SUM(amount) AS total_sales FROM sales WHERE quarter = '2025-Q3'

                질문: 현재 활성 상태인 계약 수는 몇 개야?
                SQL: SELECT COUNT(*) AS active_contract_count FROM contracts WHERE status = 'active'

                질문: 기술지원팀 직원 목록과 연봉을 알려줘
                SQL: SELECT e.name, e.position, e.salary FROM employees e JOIN departments d ON e.dept_id = d.id WHERE d.name = '기술지원팀'

                [질문]
                %s
                """
        .formatted(schema, question);
  }

  private String extractSql(String generated) {
    Matcher matcher = SQL_BLOCK.matcher(generated);
    if (matcher.find()) {
      return matcher.group(1);
    }
    int selectIndex = generated.toLowerCase().indexOf("select");
    if (selectIndex >= 0) {
      return generated.substring(selectIndex);
    }
    return generated;
  }
}
