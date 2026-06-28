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
import tn.esprit.backend_esprittech.dto.equipe.AffiliationEnseignantResponse;
import tn.esprit.backend_esprittech.dto.equipe.TraiterAffiliationRequest;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.equipe.AffiliationEnseignantService;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AffiliationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AffiliationEnseignantService affiliationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AffiliationController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserResponse enseignantResponse;
    private AffiliationEnseignantResponse affiliationResponse;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        enseignantResponse = new UserResponse(1L, "Doe", "John", "john@test.tn",
                "JD001", Role.ROLE_ENSEIGNANT, TypeUtilisateur.ENSEIGNANT,
                "Informatique", true, Instant.now());

        affiliationResponse = new AffiliationEnseignantResponse(
                100L, enseignantResponse, 10L, "Équipe Alpha",
                StatutAffiliation.EN_ATTENTE, LocalDateTime.now(), null, null
        );
    }

    // ================================================================
    // POST /api/equipes/{equipeId}/affiliations — demanderAffiliation
    // ================================================================

    @Test
    void demanderAffiliation_success_returns201() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(affiliationService.demander(eq(10L), any())).thenReturn(affiliationResponse);

        mockMvc.perform(post("/api/equipes/10/affiliations"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.equipeId").value(10))
                .andExpect(jsonPath("$.equipeNom").value("Équipe Alpha"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        verify(affiliationService).demander(10L, currentUser);
    }

    @Test
    void demanderAffiliation_serviceThrowsBusinessException_returns400() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(affiliationService.demander(eq(10L), any()))
                .thenThrow(new BusinessException("Vous êtes déjà membre de cette équipe"));

        mockMvc.perform(post("/api/equipes/10/affiliations"))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // GET /api/equipes/affiliations/mine — mesDemandes
    // ================================================================

    @Test
    void mesDemandes_returns200() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(affiliationService.getMesDemandes(currentUser)).thenReturn(List.of(affiliationResponse));

        mockMvc.perform(get("/api/equipes/affiliations/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));

        verify(affiliationService).getMesDemandes(currentUser);
    }

    @Test
    void mesDemandes_emptyList_returns200() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(affiliationService.getMesDemandes(currentUser)).thenReturn(List.of());

        mockMvc.perform(get("/api/equipes/affiliations/mine"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ================================================================
    // GET /api/equipes/{equipeId}/affiliations — getDemandesByEquipe
    // ================================================================

    @Test
    void getDemandesByEquipe_returns200() throws Exception {
        when(affiliationService.getDemandesByEquipe(10L)).thenReturn(List.of(affiliationResponse));

        mockMvc.perform(get("/api/equipes/10/affiliations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].equipeId").value(10));

        verify(affiliationService).getDemandesByEquipe(10L);
    }

    @Test
    void getDemandesByEquipe_empty_returns200() throws Exception {
        when(affiliationService.getDemandesByEquipe(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/equipes/10/affiliations"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ================================================================
    // PUT /api/equipes/{equipeId}/affiliations/{affiliationId} — traiterDemande
    // ================================================================

    @Test
    void traiterDemande_accept_returns200() throws Exception {
        User currentUser = new User();
        currentUser.setId(2L);
        currentUser.setRole(Role.ROLE_CHEF_EQUIPE);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(affiliationService.traiter(100L, 10L, currentUser,
                StatutAffiliation.ACCEPTEE, "Bienvenue"))
                .thenReturn(affiliationResponse);

        String body = objectMapper.writeValueAsString(
                new TraiterAffiliationRequest(StatutAffiliation.ACCEPTEE, "Bienvenue"));

        mockMvc.perform(put("/api/equipes/10/affiliations/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void traiterDemande_invalidBody_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new TraiterAffiliationRequest(null, null));

        mockMvc.perform(put("/api/equipes/10/affiliations/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
