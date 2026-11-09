package com.legaldocs.dto;

import java.util.List;

public record AiExtractionResult(
    String suggestedContractType,
    String executiveSummary,
    List<ExtractedClause> clauses,
    List<ExtractedObligation> obligations
) {
    public record ExtractedClause(
        String title,
        String category,
        String content,
        Integer pageNumber
    ) {}

    public record ExtractedObligation(
        String description,
        String dueDateIso
    ) {}
}
