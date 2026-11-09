package com.legaldocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ContractRequest(
    @NotBlank(message = "El código es obligatorio") String code,
    @NotBlank(message = "El título es obligatorio") String title,
    @NotBlank(message = "La contraparte es obligatoria") String counterparty,
    @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,
    @NotNull(message = "La fecha de vencimiento es obligatoria") LocalDate endDate,
    Long contractTypeId,
    Long responsibleUserId,
    String observations
) {}
