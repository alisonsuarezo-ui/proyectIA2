package com.legaldocs.dto;

import java.util.List;

public record RagQueryResponse(
    String answer,
    boolean sufficientContext,
    List<SourceReference> references
) {
    public record SourceReference(
        String pointId,
        Long contractId,
        String contractCode,
        String contractTitle,
        String fileName,
        Integer pageNumber,
        String sourceType,
        String clauseTitle,
        String clauseCategory,
        String excerpt
    ) {}
}
