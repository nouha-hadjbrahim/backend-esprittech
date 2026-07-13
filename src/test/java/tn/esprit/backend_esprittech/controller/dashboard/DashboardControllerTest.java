package tn.esprit.backend_esprittech.controller.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.dashboard.DashboardService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDashboard_returns200WithKpis() throws Exception {
        DashboardResponse response = new DashboardResponse(
                10L, 7L, 2L, 1L,
                20L, 5L, 12L, 3L,
                4L, 1L,
                6L, 2L, 78.46
        );
        when(dashboardService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sujetsTotal").value(10))
                .andExpect(jsonPath("$.candidaturesTotal").value(20))
                .andExpect(jsonPath("$.scoreMoyen").value(78.46));

        verify(dashboardService).getDashboard();
    }

    @Test
    void getDashboard_serviceThrows_returns500() throws Exception {
        when(dashboardService.getDashboard())
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDashboard_allZeros_returns200WithZeros() throws Exception {
        DashboardResponse response = new DashboardResponse(
                0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                0L, 0L,
                0L, 0L, 0.0
        );
        when(dashboardService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sujetsTotal").value(0))
                .andExpect(jsonPath("$.candidaturesTotal").value(0))
                .andExpect(jsonPath("$.scoreMoyen").value(0.0));
    }
}
