package com.legaldocs.controller;

import com.legaldocs.dto.DashboardStatsResponse;
import com.legaldocs.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardStatsResponse getStats() {
        return dashboardService.getDashboardMetrics();
    }
}
