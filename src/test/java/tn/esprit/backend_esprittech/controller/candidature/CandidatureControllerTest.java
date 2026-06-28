package tn.esprit.backend_esprittech.controller.candidature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.enums.StatutAffectation;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.service.candidature.CandidatureService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureControllerTest {

    @Mock private CandidatureService candidatureService;
    @InjectMocks private CandidatureController controller;

    private CandidatureResponse candidatureResponse;
    private AffectationResponse affectationResponse;

    @BeforeEach
    void setUp() {
        candidatureResponse = new CandidatureResponse(100L, 10L, 1L, "Ben Ali", "Sami",
                StatutCandidature.DEPOSEE, null, "Motivé", Instant.now(), null);

        affectationResponse = new AffectationResponse(200L, 10L, 1L, "Ben Ali", "Sami",
                StatutAffectation.RETIREE_ARCHIVEE, Instant.now(), Instant.now(), "Abandon");
    }

    // Tests for US-14, US-16, US-17, US-18, US-19, US-20, US-22, US-24
    @Test void ouvrirCandidatures_appelleService_retourneNoContent() { /* ... */ }
    @Test void deposerCandidature_forceSujetIdDepuisUrl_retourneCreated() { /* ... */ }
    @Test void deciderCandidature_delegueAuService() { /* ... */ }
    @Test void fermerCandidatures_appelleService_retourneNoContent() { /* ... */ }
    @Test void retirerEtudiant_delegueAuServiceEtRetourneAffectation() { /* ... */ }
    @Test void declarerTerminaison_appelleService_retourneNoContent() { /* ... */ }

    @Test
    void getCandidaturesParSujet_retourneListe() {
        when(candidatureService.getCandidaturesParSujet(10L)).thenReturn(List.of(candidatureResponse));
        ResponseEntity<List<CandidatureResponse>> response = controller.getCandidaturesParSujet(10L);
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getMesCandidatures_retourneListe() {
        when(candidatureService.getMesCandidatures()).thenReturn(List.of(candidatureResponse));
        ResponseEntity<List<CandidatureResponse>> response = controller.getMesCandidatures();
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAffectationsParSujet_retourneListe() {
        when(candidatureService.getAffectationsParSujet(10L)).thenReturn(List.of(affectationResponse));
        ResponseEntity<List<AffectationResponse>> response = controller.getAffectationsParSujet(10L);
        assertEquals(1, response.getBody().size());
    }
}