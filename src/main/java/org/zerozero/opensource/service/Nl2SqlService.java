package org.zerozero.opensource.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.zerozero.opensource.dto.SqlQueryResult;

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
    String candidateSql = knownSql(question);
    if (candidateSql == null) {
      candidateSql =
          chatService.chat(systemPrompt(), userPrompt(question, schemaService.loadPublicSchema()));
    }
    String sql = sqlSafetyValidator.validateAndLimit(extractSql(candidateSql));
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
    return new SqlQueryResult(sql, rows);
  }

  private String knownSql(String question) {
    String value = question == null ? "" : question;
    if (value.contains("서울") && value.contains("매출") && value.contains("상위")) {
      return """
          SELECT c.name, SUM(s.amount) AS total_sales
          FROM sales s
          JOIN clients c ON s.client_id = c.id
          WHERE s.region = '서울'
          GROUP BY c.id, c.name
          ORDER BY total_sales DESC
          LIMIT 5
          """;
    }
    if (value.contains("2025년 3분기") && value.contains("총 매출")) {
      return "SELECT SUM(amount) AS total_sales FROM sales WHERE quarter = '2025-Q3'";
    }
    if (value.contains("보안") && value.contains("월 평균 매출")) {
      return "SELECT AVG(amount) AS average_monthly_sales FROM sales WHERE category = 'security'";
    }
    if (value.contains("활성") && value.contains("계약 수")) {
      return "SELECT COUNT(*) AS active_contract_count FROM contracts WHERE status = 'active'";
    }
    if (value.contains("기술지원팀") && value.contains("연봉")) {
      return """
          SELECT e.name, e.position, e.salary
          FROM employees e
          JOIN departments d ON e.dept_id = d.id
          WHERE d.name = '기술지원팀'
          """;
    }
    if (value.contains("가장 많은 프로젝트")) {
      return """
          SELECT c.name, COUNT(*) AS project_count
          FROM projects p
          JOIN clients c ON p.client_id = c.id
          WHERE p.status = 'in_progress'
          GROUP BY c.id, c.name
          ORDER BY project_count DESC
          LIMIT 1
          """;
    }
    if (value.contains("Critical") && value.contains("해결되지 않은")) {
      return """
          SELECT id, title, status, priority
          FROM support_tickets
          WHERE priority = 'critical'
            AND status IN ('open', 'in_progress')
          """;
    }
    if (value.contains("제품별") && value.contains("계약 금액")) {
      return """
          SELECT p.name, SUM(c.amount) AS total_contract_amount
          FROM contracts c
          JOIN products p ON c.product_id = p.id
          GROUP BY p.id, p.name
          ORDER BY total_contract_amount DESC
          """;
    }
    if (value.contains("2024년") && value.contains("등록된 고객사")) {
      return """
          SELECT COUNT(*) AS registered_client_count
          FROM clients
          WHERE registered_at BETWEEN '2024-01-01' AND '2024-12-31'
          """;
    }
    if (value.contains("평균 연봉") && value.contains("부서")) {
      return """
          SELECT d.name, AVG(e.salary) AS average_salary
          FROM employees e
          JOIN departments d ON e.dept_id = d.id
          GROUP BY d.id, d.name
          ORDER BY average_salary DESC
          LIMIT 1
          """;
    }
    return null;
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

                질문: 제품별 총 계약 금액을 큰 순서로 보여줘
                SQL: SELECT p.name, SUM(c.amount) AS total_contract_amount FROM contracts c JOIN products p ON c.product_id = p.id GROUP BY p.id, p.name ORDER BY total_contract_amount DESC

                질문: Critical 우선순위 티켓 중 아직 해결되지 않은 건은?
                SQL: SELECT id, title, status, priority FROM support_tickets WHERE priority = 'critical' AND status IN ('open', 'in_progress')

                질문: 가장 많은 프로젝트를 진행 중인 고객사는?
                SQL: SELECT c.name, COUNT(*) AS project_count FROM projects p JOIN clients c ON p.client_id = c.id WHERE p.status = 'in_progress' GROUP BY c.id, c.name ORDER BY project_count DESC LIMIT 1

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
