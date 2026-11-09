package com.legaldocs.dto;

import java.time.LocalDate;

public record PendingNotificationDto(
    Long contractId,
    Long obligationId,
    String contractCode,
    String contractTitle,
    String counterparty,
    String alertType,
    LocalDate dueDate,
    long daysRemaining,
    String recipientEmail,
    String subject,
    String messageDetail
) {}
