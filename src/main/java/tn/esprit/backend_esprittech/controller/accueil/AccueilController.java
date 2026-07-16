package tn.esprit.backend_esprittech.controller.accueil;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.accueil.AccueilStatsResponse;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

@RestController
@RequestMapping("/api/accueil")
@RequiredArgsConstructor
public class AccueilController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public AccueilStatsResponse getStats() {
        return dashboardService.getAccueilStats();
    }
}
