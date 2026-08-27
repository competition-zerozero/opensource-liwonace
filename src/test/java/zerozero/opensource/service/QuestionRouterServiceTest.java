package zerozero.opensource.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionRouterServiceTest {

  private final QuestionRouterService routerService = new QuestionRouterService();

  @Test
  void routeToNl2SqlForAggregationQuestion() {
    assertThat(routerService.route("2025년 3분기 총 매출액은 얼마야?").toolName()).isEqualTo("nl2sql");
  }

  @Test
  void routeToVectorSearchForIncidentQuestion() {
    assertThat(routerService.route("최근 서버 장애 사례와 원인을 알려줘").toolName()).isEqualTo("vector_search");
  }

  @Test
  void routeToGraphSearchForRelationQuestion() {
    assertThat(routerService.route("Client-A는 어떤 제품을 사용해?").toolName()).isEqualTo("graph_search");
  }
}
