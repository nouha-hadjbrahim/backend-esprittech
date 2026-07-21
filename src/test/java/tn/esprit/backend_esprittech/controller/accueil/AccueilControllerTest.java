package tn.esprit.backend_esprittech.controller.accueil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.accueil.AccueilStatsResponse;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccueilControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private AccueilController controller;

    @Test
    void getStats_shouldReturnDashboardStats() {
        AccueilStatsResponse response = new AccueilStatsResponse(3, 812, 20, 8, 6);
        when(dashboardService.getAccueilStats()).thenReturn(response);

        AccueilStatsResponse result = controller.getStats();

        assertEquals(response, result);
        verify(dashboardService).getAccueilStats();
    }
}
