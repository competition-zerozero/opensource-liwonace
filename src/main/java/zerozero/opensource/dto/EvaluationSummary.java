package zerozero.opensource.dto;

import java.util.List;

public record EvaluationSummary(
    int totalCount,
    int routingMatchedCount,
    int executionSucceededCount,
    int answerProvidedCount,
    double routingAccuracy,
    double executionSuccessRate,
    double answerProvidedRate,
    List<EvaluationCaseResult> cases) {}
