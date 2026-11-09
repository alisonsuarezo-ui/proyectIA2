package com.legaldocs.service;

import com.legaldocs.dto.AlertAckRequest;
import com.legaldocs.dto.PendingNotificationDto;
import com.legaldocs.entity.Alert;
import com.legaldocs.entity.Contract;
import com.legaldocs.entity.Obligation;
import com.legaldocs.exception.BusinessException;
import com.legaldocs.repository.AlertRepository;
import com.legaldocs.repository.ContractRepository;
import com.legaldocs.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutomationService {

    private final ContractRepository contractRepository;
    private final ObligationRepository obligationRepository;
    private final AlertRepository alertRepository;
    private final ContractService contractService;

    @Transactional
    public List<PendingNotificationDto> findPendingNotifications() {
        contractService.refreshContractStatuses();
        LocalDate today = LocalDate.now();
        List<PendingNotificationDto> notifications = new ArrayList<>();

        List<Contract> expiringContracts = contractRepository.findExpiringContracts(today, today.plusDays(30));

        for (Contract c : expiringContracts) {
            long daysLeft = ChronoUnit.DAYS.between(today, c.getEndDate());
            String alertType = resolveAlertThreshold(daysLeft);

            if (alertType != null) {
                boolean alreadySent = alertRepository.existsByContractIdAndAlertTypeAndStatus(
                        c.getId(), alertType, "SENT"
                );
                if (!alreadySent) {
                    String email = c.getResponsibleUser() != null
                            ? c.getResponsibleUser().getEmail()
                            : "admin@legaldocs.com";

                    notifications.add(new PendingNotificationDto(
                            c.getId(),
                            null,
                            c.getCode(),
                            c.getTitle(),
                            c.getCounterparty(),
                            alertType,
                            c.getEndDate(),
                            daysLeft,
                            email,
                            "[LegalDocs] Alerta de Vencimiento (" + daysLeft + " días) - Contrato " + c.getCode(),
                            "El contrato '" + c.getTitle() + "' con " + c.getCounterparty() + " vence el " + c.getEndDate() + "."
                    ));
                }
            }
        }

        List<Obligation> upcomingObligations = obligationRepository.findByStatusAndDueDateBetween(
                "PENDING", today, today.plusDays(7)
        );

        for (Obligation ob : upcomingObligations) {
            boolean alreadySent = alertRepository.existsByObligationIdAndAlertTypeAndStatus(
                    ob.getId(), "OBLIGATION_DUE", "SENT"
            );
            if (!alreadySent) {
                Contract c = ob.getContract();
                long daysLeft = ChronoUnit.DAYS.between(today, ob.getDueDate());
                String email = (ob.getResponsibleEmail() != null && !ob.getResponsibleEmail().isBlank())
                        ? ob.getResponsibleEmail()
                        : "admin@legaldocs.com";

                notifications.add(new PendingNotificationDto(
                        c.getId(),
                        ob.getId(),
                        c.getCode(),
                        c.getTitle(),
                        c.getCounterparty(),
                        "OBLIGATION_DUE",
                        ob.getDueDate(),
                        daysLeft,
                        email,
                        "[LegalDocs] Obligación Contractual Próxima a Vencer - " + c.getCode(),
                        "Obligación pendiente: '" + ob.getDescription() + "' con fecha límite " + ob.getDueDate() + "."
                ));
            }
        }

        return notifications;
    }

    @Transactional
    public Alert registerAlertAck(AlertAckRequest req) {
        Contract contract = contractRepository.findById(req.contractId())
                .orElseThrow(() -> new BusinessException("Contrato no encontrado: " + req.contractId()));

        Obligation obligation = null;
        if (req.obligationId() != null) {
            obligation = obligationRepository.findById(req.obligationId()).orElse(null);
        }

        Alert alert = Alert.builder()
                .contract(contract)
                .obligation(obligation)
                .alertType(req.alertType())
                .recipientEmail(req.recipientEmail())
                .status(req.status().toUpperCase())
                .build();

        return alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findTop10ByOrderBySentAtDesc();
    }

    private String resolveAlertThreshold(long daysLeft) {
        if (daysLeft <= 7 && daysLeft >= 0) return "DAYS_7";
        if (daysLeft <= 15 && daysLeft > 7) return "DAYS_15";
        if (daysLeft <= 30 && daysLeft > 15) return "DAYS_30";
        return null;
    }
}
