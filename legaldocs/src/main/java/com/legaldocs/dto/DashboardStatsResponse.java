package com.legaldocs.dto;

import com.legaldocs.entity.Alert;
import com.legaldocs.entity.Contract;
import com.legaldocs.entity.Obligation;
import java.util.List;

public record DashboardStatsResponse(
    long totalContracts,
    long activeContracts,
    long expiringSoonContracts,
    long expiredContracts,
    long pendingObligationsCount,
    List<Contract> upcomingExpirations,
    List<Obligation> pendingObligations,
    List<Alert> recentAlerts
) {}
