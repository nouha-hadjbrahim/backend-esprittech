package tn.esprit.backend_esprittech.controller.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE','CI')")
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}