package com.legaldocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertAckRequest(
    @NotNull Long contractId,
    Long obligationId,
    @NotBlank String alertType,
    @NotBlank String recipientEmail,
    @NotBlank String status
) {}
