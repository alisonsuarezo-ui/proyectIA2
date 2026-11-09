package com.legaldocs.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
    @NotBlank(message = "La pregunta no puede estar vacía") String question,
    Long contractId,
    String clauseCategory
) {}
