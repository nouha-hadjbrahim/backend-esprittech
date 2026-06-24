package tn.esprit.backend_esprittech.controller;

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
import tn.esprit.backend_esprittech.dto.request.AssignChefRequest;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EquipeRechercheService equipeService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private EquipeController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ================================================================
    // GET /api/equipes  — listerEquipes
    // ================================================================

    @Test
    void listerEquipes_withMultipleEquipes_returns200() throws Exception {
        EquipeResponse r1 = mock(EquipeResponse.class);
        EquipeResponse r2 = mock(EquipeResponse.class);

        when(equipeService.listerToutes()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/equipes"))
                .andExpect(status().isOk());

        verify(equipeService).listerToutes();
    }

    @Test
    void listerEquipes_withEmptyList_returns200AndEmptyBody() throws Exception {
        when(equipeService.listerToutes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/equipes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(equipeService).listerToutes();
    }

    @Test
    void listerEquipes_serviceThrows_returns500() throws Exception {
        when(equipeService.listerToutes())
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/equipes"))
                .andExpect(status().isInternalServerError());
    }

    // ================================================================
    // GET /api/equipes/{id}  — getEquipe
    // ================================================================

    @Test
    void getEquipe_existingId_returns200() throws Exception {
        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/equipes/1"))
                .andExpect(status().isOk());

        verify(equipeService).findById(1L);
    }

    @Test
    void getEquipe_notFound_returns404() throws Exception {
        when(equipeService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Equipe introuvable"));

        mockMvc.perform(get("/api/equipes/99"))
                .andExpect(status().isNotFound());

        verify(equipeService).findById(99L);
    }

    // ================================================================
    // GET /api/equipes/{id}/membres  — getMembres
    // ================================================================

    @Test
    void getMembres_existingId_returns200() throws Exception {
        User u1 = new User();
        User u2 = new User();
        UserResponse ur1 = mock(UserResponse.class);
        UserResponse ur2 = mock(UserResponse.class);

        when(equipeService.getMembres(1L)).thenReturn(List.of(u1, u2));
        when(userMapper.toResponse(u1)).thenReturn(ur1);
        when(userMapper.toResponse(u2)).thenReturn(ur2);

        mockMvc.perform(get("/api/equipes/1/membres"))
                .andExpect(status().isOk());

        verify(equipeService).getMembres(1L);
        verify(userMapper).toResponse(u1);
        verify(userMapper).toResponse(u2);
    }

    @Test
    void getMembres_emptyTeam_returns200AndEmptyList() throws Exception {
        when(equipeService.getMembres(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/equipes/1/membres"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(equipeService).getMembres(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getMembres_teamNotFound_returns404() throws Exception {
        when(equipeService.getMembres(99L))
                .thenThrow(new ResourceNotFoundException("Equipe introuvable"));

        mockMvc.perform(get("/api/equipes/99/membres"))
                .andExpect(status().isNotFound());

        verify(equipeService).getMembres(99L);
    }

    // ================================================================
    // POST /api/equipes  — create
    // ================================================================

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateEquipeRechercheRequest request =
                new CreateEquipeRechercheRequest("AI Lab", "Research on AI", "Informatique", null, null);
        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(equipeService).create(any());
    }

    @Test
    void create_duplicateName_returns400() throws Exception {
        CreateEquipeRechercheRequest request =
                new CreateEquipeRechercheRequest("AI Lab", "Desc", "Informatique", null, null);

        when(equipeService.create(any()))
                .thenThrow(new BusinessException("Nom déjà utilisé"));

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(equipeService).create(any());
    }

    @Test
    void create_serviceThrows_returns500() throws Exception {
        CreateEquipeRechercheRequest request =
                new CreateEquipeRechercheRequest("AI Lab", "Desc", "Informatique", null, null);

        when(equipeService.create(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(equipeService).create(any());
    }

    // ================================================================
    // PUT /api/equipes/{id}  — update
    // ================================================================

    @Test
    void update_validRequest_returns200() throws Exception {
        UpdateEquipeRechercheRequest request =
                new UpdateEquipeRechercheRequest("AI Lab v2", "Updated desc", null, null, null);
        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/equipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(equipeService).update(eq(1L), any());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        UpdateEquipeRechercheRequest request =
                new UpdateEquipeRechercheRequest("AI Lab v2", "Desc", null, null, null);

        when(equipeService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Equipe introuvable"));

        mockMvc.perform(put("/api/equipes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(equipeService).update(eq(99L), any());
    }

    @Test
    void update_invalidBusiness_returns400() throws Exception {
        UpdateEquipeRechercheRequest request =
                new UpdateEquipeRechercheRequest("AI Lab v2", "Desc", null, null, null);

        when(equipeService.update(eq(1L), any()))
                .thenThrow(new BusinessException("Règle métier violée"));

        mockMvc.perform(put("/api/equipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(equipeService).update(eq(1L), any());
    }

    // ================================================================
    // DELETE /api/equipes/{id}  — delete
    // ================================================================

    @Test
    void delete_existingId_returns204() throws Exception {
        doNothing().when(equipeService).delete(1L);

        mockMvc.perform(delete("/api/equipes/1"))
                .andExpect(status().isNoContent());

        verify(equipeService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Equipe introuvable"))
                .when(equipeService).delete(99L);

        mockMvc.perform(delete("/api/equipes/99"))
                .andExpect(status().isNotFound());

        verify(equipeService).delete(99L);
    }

    // ================================================================
    // PUT /api/equipes/{id}/chef  — assignerChef
    // ================================================================

    @Test
    void assignerChef_validRequest_returns200() throws Exception {
        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.assignerChef(1L, 2L)).thenReturn(response);

        mockMvc.perform(put("/api/equipes/1/chef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignChefRequest(2L))))
                .andExpect(status().isOk());

        verify(equipeService).assignerChef(1L, 2L);
    }

    @Test
    void assignerChef_teamNotFound_returns404() throws Exception {
        when(equipeService.assignerChef(99L, 2L))
                .thenThrow(new ResourceNotFoundException("Equipe introuvable"));

        mockMvc.perform(put("/api/equipes/99/chef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignChefRequest(2L))))
                .andExpect(status().isNotFound());

        verify(equipeService).assignerChef(99L, 2L);
    }

    @Test
    void assignerChef_userNotFound_returns404() throws Exception {
        when(equipeService.assignerChef(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Utilisateur introuvable"));

        mockMvc.perform(put("/api/equipes/1/chef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignChefRequest(999L))))
                .andExpect(status().isNotFound());

        verify(equipeService).assignerChef(1L, 999L);
    }

    @Test
    void assignerChef_invalidBusiness_returns400() throws Exception {
        when(equipeService.assignerChef(1L, 2L))
                .thenThrow(new BusinessException("L'utilisateur n'est pas éligible"));

        mockMvc.perform(put("/api/equipes/1/chef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignChefRequest(2L))))
                .andExpect(status().isBadRequest());

        verify(equipeService).assignerChef(1L, 2L);
    }
}