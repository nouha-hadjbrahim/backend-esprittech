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
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

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
    @Mock private SujetProjetService sujetProjetService;
    @Mock private SujetReferenceService referenceService;
    @InjectMocks private SujetProjetController controller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator).build();
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "titre", "Sujet test", "categorie", "PFE", "description", "Description",
                "objectifs", "Objectifs",                 "prerequis", List.of("Soft skills"),
                "domaines", List.of("Informatique"), "technologies", List.of("Java"), "capaciteAccueil", 2
        );
    }

    @Test
    void creerSujet_validRequest_returns201() throws Exception {
        SujetProjetResponse sample = new SujetProjetResponse(
                1L, "Sujet test", CategorieSujet.PFE, "Description", "Objectifs",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2,
                StatutSujet.SOUMIS_EN_VALIDATION, null, false, false, 10L, "Sami Ben Ali",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null
        );
        when(sujetProjetService.creerSujet(any(SujetProjetRequest.class))).thenReturn(sample);

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated());
    }

    @Test
    void creerSujet_invalidRequest_returns400() throws Exception {
        Map<String, Object> invalid = Map.of(
                "titre", "", "categorie", "PFE", "description", "Description",
                "objectifs", "Objectifs", "prerequis", Collections.emptyList(),
                "domaines", Collections.emptyList(), "technologies", List.of(), "capaciteAccueil", 0
        );

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
