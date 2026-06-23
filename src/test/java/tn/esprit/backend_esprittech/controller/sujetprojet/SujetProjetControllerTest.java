package tn.esprit.backend_esprittech.controller.sujetprojet;

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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.MockEncadrantResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.sujetprojet.MockEncadrantContext;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SujetProjetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SujetProjetService sujetProjetService;

    @InjectMocks
    private SujetProjetController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SujetProjetResponse sampleResponse;

    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        sampleResponse = new SujetProjetResponse(
                1L, "Sujet", CategorieSujet.PFE, "Desc", "Obj", "Pre", "Info",
                List.of("Java"), 2, StatutSujet.SOUMIS_EN_VALIDATION, null, false, false,
                MockEncadrantContext.ENCADRANT_ID, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null
        );
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "titre", "Sujet test",
                "categorie", "PFE",
                "description", "Description",
                "objectifs", "Objectifs",
                "prerequis", "Prérequis",
                "domaine", "Informatique",
                "technologies", List.of("Java"),
                "capaciteAccueil", 2
        );
    }

    @Test
    void creerSujet_validRequest_returns201() throws Exception {
        when(sujetProjetService.creerSujet(any(SujetProjetRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.titre").value("Sujet"));
    }

    @Test
    void creerSujet_invalidRequest_returns400() throws Exception {
        Map<String, Object> invalid = Map.of(
                "titre", "",
                "categorie", "PFE",
                "description", "Description",
                "objectifs", "Objectifs",
                "prerequis", "Prérequis",
                "domaine", "Informatique",
                "technologies", Collections.emptyList(),
                "capaciteAccueil", 0
        );

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation"));
    }

    @Test
    void getMesSujets_returns200() throws Exception {
        when(sujetProjetService.getMesSujets(null, null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/sujet-projets/mes-sujets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getMesSujets_withFilters_returns200() throws Exception {
        when(sujetProjetService.getMesSujets(CategorieSujet.PFE, StatutSujet.SOUMIS_EN_VALIDATION))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/sujet-projets/mes-sujets")
                        .param("categorie", "PFE")
                        .param("statut", "SOUMIS_EN_VALIDATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categorie").value("PFE"));
    }

    @Test
    void getSujet_returns200() throws Exception {
        when(sujetProjetService.getSujetById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/sujet-projets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getSujet_notFound_returns400() throws Exception {
        when(sujetProjetService.getSujetById(99L))
                .thenThrow(new IllegalArgumentException("Sujet introuvable"));

        mockMvc.perform(get("/api/sujet-projets/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Sujet introuvable"));
    }

    @Test
    void modifierSujet_validRequest_returns200() throws Exception {
        when(sujetProjetService.modifierSujet(eq(1L), any(SujetProjetRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/sujet-projets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void modifierSujet_invalidRequest_returns400() throws Exception {
        Map<String, Object> invalid = Map.of(
                "titre", "Titre",
                "categorie", "PFE",
                "description", "",
                "objectifs", "Objectifs",
                "prerequis", "Prérequis",
                "domaine", "Informatique",
                "technologies", List.of("Java"),
                "capaciteAccueil", 2
        );

        mockMvc.perform(put("/api/sujet-projets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTechnologies_returns200() throws Exception {
        when(sujetProjetService.getTechnologiesDisponibles()).thenReturn(List.of("Angular", "Java"));

        mockMvc.perform(get("/api/sujet-projets/technologies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Angular"));
    }

    @Test
    void getMockEncadrant_returns200() throws Exception {
        MockEncadrantResponse encadrant = new MockEncadrantResponse(
                1001L, "Dr. Ben Ali Sami", "Encadrant", "benali.sami@esprittech.tn", "Informatique"
        );
        when(sujetProjetService.getMockEncadrant()).thenReturn(encadrant);

        mockMvc.perform(get("/api/sujet-projets/mock-encadrant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomComplet").value("Dr. Ben Ali Sami"));
    }
}
