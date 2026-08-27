package zerozero.opensource.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import zerozero.opensource.dto.RouteDecision;

@Service
public class QuestionRouterService {

  private static final String NL2SQL = "nl2sql";
  private static final String VECTOR_SEARCH = "vector_search";
  private static final String GRAPH_SEARCH = "graph_search";

  private static final List<String> NL2SQL_KEYWORDS =
      List.of(
          "몇 건", "몇개", "몇 개", "개수", "수는", "평균", "합계", "총", "매출", "고객 수", "계약 수", "분기", "월별", "연도별",
          "상위", "하위", "금액", "연봉", "목록");

  private static final List<String> VECTOR_KEYWORDS =
      List.of(
          "장애", "원인", "조치", "해결", "보고", "보고서", "문서", "매뉴얼", "기술문서", "회의록", "제안서", "내용", "사례", "요약",
          "이슈");

  private static final List<String> GRAPH_KEYWORDS =
      List.of(
          "담당자", "소속", "연결", "관계", "관련", "프로젝트", "사용하는 제품", "사용 제품", "누가", "어느 팀", "어떤 제품", "고객",
          "부서", "관리", "담당");

  public RouteDecision route(String question) {
    String normalized = normalize(question);
    int nl2sqlScore = score(normalized, NL2SQL_KEYWORDS);
    int vectorScore = score(normalized, VECTOR_KEYWORDS);
    int graphScore = score(normalized, GRAPH_KEYWORDS);

    if (graphScore > 0 && graphScore >= nl2sqlScore && graphScore >= vectorScore) {
      return new RouteDecision(GRAPH_SEARCH, "관계/담당자/연결 탐색 키워드가 감지되었습니다.");
    }
    if (nl2sqlScore > 0 && nl2sqlScore >= vectorScore) {
      return new RouteDecision(NL2SQL, "집계/수치/정형 데이터 조회 키워드가 감지되었습니다.");
    }
    if (vectorScore > 0) {
      return new RouteDecision(VECTOR_SEARCH, "문서/장애/회의록 검색 키워드가 감지되었습니다.");
    }
    return new RouteDecision(VECTOR_SEARCH, "명확한 정형/관계 키워드가 없어 문서 검색을 기본 도구로 선택했습니다.");
  }

  private String normalize(String question) {
    if (question == null) {
      return "";
    }
    return question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
  }

  private int score(String question, List<String> keywords) {
    int score = 0;
    for (String keyword : keywords) {
      if (question.contains(keyword.toLowerCase(Locale.ROOT))) {
        score++;
      }
    }
    return score;
  }
}
