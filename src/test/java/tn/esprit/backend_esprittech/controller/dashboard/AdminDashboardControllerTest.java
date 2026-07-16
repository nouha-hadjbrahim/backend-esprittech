package tn.esprit.backend_esprittech.controller.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.backend_esprittech.dto.dashboard.extended.*;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private AdminDashboardController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getExtendedDashboard_returns200WithAllSections() throws Exception {
        AdminKpiResponse kpis = new AdminKpiResponse(
                50L, 20L, 8L, 2L,
                10L, 5L, 3L, 2L,
                5L, 12L, 3L,
                4L, 6L, 2L, 78.46
        );
        ExtendedDashboardResponse response = new ExtendedDashboardResponse(
                kpis,
                List.of(new TrendItem("janv. 2026", 5)),
                List.of(new TrendItem("janv. 2026", 10)),
                List.of(new TrendItem("janv. 2026", 3)),
                List.of(new SujetByStatutItem("En attente", 5, "#F59E0B")),
                List.of(new UsersByRoleItem("Étudiant", 30)),
                List.of(new CatalogueByDomainItem("Informatique", 4)),
                List.of(new RecentActivityItem(1L, "Sujet validé", "Alice Dupont", "À l'instant", "validation"))
        );
        when(dashboardService.getExtendedDashboard(15)).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/extended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.usersTotal").value(50))
                .andExpect(jsonPath("$.kpis.equipesTotal").value(8))
                .andExpect(jsonPath("$.trendsSujets.length()").value(1))
                .andExpect(jsonPath("$.trendsCandidatures.length()").value(1))
                .andExpect(jsonPath("$.sujetsByStatut.length()").value(1))
                .andExpect(jsonPath("$.usersByRole.length()").value(1))
                .andExpect(jsonPath("$.recentActivity.length()").value(1));

        verify(dashboardService).getExtendedDashboard(15);
    }

    @Test
    void getExtendedDashboard_serviceThrows_returns500() throws Exception {
        when(dashboardService.getExtendedDashboard(15))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/admin/dashboard/extended"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getExtendedDashboard_emptyData_returns200WithEmptyLists() throws Exception {
        AdminKpiResponse kpis = new AdminKpiResponse(
                0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                0L, 0L, 0L,
                0L, 0L, 0L, 0.0
        );
        ExtendedDashboardResponse response = new ExtendedDashboardResponse(
                kpis,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        when(dashboardService.getExtendedDashboard(15)).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/extended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.usersTotal").value(0))
                .andExpect(jsonPath("$.trendsSujets.length()").value(0))
                .andExpect(jsonPath("$.recentActivity.length()").value(0));
    }
}
