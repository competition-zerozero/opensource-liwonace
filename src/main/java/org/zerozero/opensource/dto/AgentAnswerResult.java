package org.zerozero.opensource.dto;

public record AgentAnswerResult(
    String question,
    String selectedTool,
    String routingReason,
    Object toolResult,
    String answer,
    boolean success,
    String errorMessage,
    long elapsedMillis) {}
