package org.zerozero.opensource.dto;

public record EvaluationCaseResult(
    int index,
    String question,
    String expectedTool,
    String selectedTool,
    boolean routingMatched,
    boolean executionSucceeded,
    boolean answerProvided,
    long elapsedMillis,
    String answer,
    String errorMessage) {}
