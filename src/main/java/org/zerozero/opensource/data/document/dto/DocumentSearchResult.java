package org.zerozero.opensource.data.document.dto;

public record DocumentSearchResult(
    String docId, int chunkIndex, String content, String metadata, double similarity) {}
