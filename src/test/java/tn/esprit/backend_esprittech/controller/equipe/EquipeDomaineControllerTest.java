package tn.esprit.backend_esprittech.controller.equipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.equipe.EquipeDomaineService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipeDomaineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EquipeDomaineService service;

    @InjectMocks
    private EquipeDomaineController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EquipeDomaineResponse resp1 =
            new EquipeDomaineResponse(1L, "Informatique", LocalDateTime.now());
    private final EquipeDomaineResponse resp2 =
            new EquipeDomaineResponse(2L, "Réseaux", LocalDateTime.now());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPage_withoutSearch_returns200() throws Exception {
        when(service.getPage(0, 12, null))
                .thenReturn(new PagedResponse<>(List.of(resp1, resp2), 0, 12, 2, 1, true, true));

        mockMvc.perform(get("/api/equipes/admin/domaines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2));
    }

    @Test
    void getPage_withSearch_returns200() throws Exception {
        when(service.getPage(0, 12, "info"))
                .thenReturn(new PagedResponse<>(List.of(resp1), 0, 12, 1, 1, true, true));

        mockMvc.perform(get("/api/equipes/admin/domaines")
                        .param("search", "info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1));
    }

    @Test
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(get("/api/equipes/admin/domaines/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    void getCount_returns200() throws Exception {
        when(service.count()).thenReturn(5L);

        mockMvc.perform(get("/api/equipes/admin/domaines/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(service.create(any(EquipeDomaineRequest.class))).thenReturn(resp1);

        mockMvc.perform(post("/api/equipes/admin/domaines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipeDomaineRequest("Informatique"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Informatique"));
    }

    @Test
    void update_valid_returns200() throws Exception {
        when(service.update(eq(1L), any(EquipeDomaineRequest.class))).thenReturn(resp1);

        mockMvc.perform(put("/api/equipes/admin/domaines/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipeDomaineRequest("Informatique"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Informatique"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/equipes/admin/domaines/1"))
                .andExpect(status().isNoContent());
    }
}
