package tn.esprit.backend_esprittech.controller.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.dashboard.extended.ExtendedDashboardResponse;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/extended")
    @PreAuthorize("hasRole('ADMIN')")
    public ExtendedDashboardResponse getExtendedDashboard(
            @RequestParam(defaultValue = "15") int recentActivityLimit) {
        return dashboardService.getExtendedDashboard(Math.min(Math.max(recentActivityLimit, 1), 100));
    }
}
