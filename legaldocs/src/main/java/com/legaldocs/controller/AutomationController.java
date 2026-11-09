package com.legaldocs.controller;

import com.legaldocs.dto.AlertAckRequest;
import com.legaldocs.dto.PendingNotificationDto;
import com.legaldocs.entity.Alert;
import com.legaldocs.exception.BusinessException;
import com.legaldocs.service.AutomationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @Value("${app.automation-api-key:n8n_internal_secret_key_2026}")
    private String expectedApiKey;

    @GetMapping("/pending-notifications")
    public List<PendingNotificationDto> getPendingNotifications(
            @RequestHeader(value = "X-Automation-Key", required = false) String apiKey) {
        validateKeyIfPresent(apiKey);
        return automationService.findPendingNotifications();
    }

    @PostMapping("/alerts-ack")
    public Alert acknowledgeAlert(
            @RequestHeader(value = "X-Automation-Key", required = false) String apiKey,
            @Valid @RequestBody AlertAckRequest request) {
        validateKeyIfPresent(apiKey);
        return automationService.registerAlertAck(request);
    }

    @GetMapping("/alerts")
    public List<Alert> getRecentAlerts() {
        return automationService.getAllAlerts();
    }

    private void validateKeyIfPresent(String apiKey) {
        if (apiKey != null && !apiKey.isBlank() && !expectedApiKey.equals(apiKey)) {
            throw new BusinessException("Clave de automatización X-Automation-Key inválida.");
        }
    }
}
