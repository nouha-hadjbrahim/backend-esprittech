package tn.esprit.backend_esprittech.controller.accueil;

import lombok.RequiredArgsConstructor;
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

    /** Public : affiché sur les pages de connexion / inscription. */
    @GetMapping("/stats")
    public AccueilStatsResponse getStats() {
        return dashboardService.getAccueilStats();
    }
}
