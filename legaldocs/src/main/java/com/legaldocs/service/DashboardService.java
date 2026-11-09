package com.legaldocs.service;

import com.legaldocs.dto.DashboardStatsResponse;
import com.legaldocs.entity.Contract;
import com.legaldocs.entity.Obligation;
import com.legaldocs.repository.AlertRepository;
import com.legaldocs.repository.ContractRepository;
import com.legaldocs.repository.ObligationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContractService contractService;
    private final ContractRepository contractRepository;
    private final ObligationRepository obligationRepository;
    private final AlertRepository alertRepository;

    public DashboardStatsResponse getDashboardMetrics() {
        contractService.refreshContractStatuses();

        long total = contractRepository.count();
        long active = contractRepository.countByStatus("ACTIVE");
        long expiringSoon = contractRepository.countByStatus("EXPIRING_SOON");
        long expired = contractRepository.countByStatus("EXPIRED");
        long pendingObligationsCount = obligationRepository.countByStatus("PENDING");

        LocalDate today = LocalDate.now();
        List<Contract> upcomingExpirations = contractRepository.findExpiringContracts(today, today.plusDays(30));
        List<Obligation> pendingObligations = obligationRepository.findByStatusOrderByDueDateAsc("PENDING");

        return new DashboardStatsResponse(
                total,
                active,
                expiringSoon,
                expired,
                pendingObligationsCount,
                upcomingExpirations,
                pendingObligations,
                alertRepository.findTop10ByOrderBySentAtDesc()
        );
    }
}
