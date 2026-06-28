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
import tn.esprit.backend_esprittech.dto.candidature.request.LivrableRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.LivrableResponse;
import tn.esprit.backend_esprittech.enums.StatutAffectation;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.enums.TypeLivrable;
import tn.esprit.backend_esprittech.service.candidature.CandidatureService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureControllerTest {

    @Mock
    private CandidatureService candidatureService;

    @InjectMocks
    private CandidatureController controller;

    private CandidatureResponse candidatureResponse;
    private AffectationResponse affectationResponse;
    private LivrableResponse livrableResponse;

    @BeforeEach
    void setUp() {
        candidatureResponse = new CandidatureResponse(100L, 10L, 1L, "Ben Ali", "Sami",
                StatutCandidature.DEPOSEE, null, "Motivé", Instant.now(), null);

        affectationResponse = new AffectationResponse(200L, 10L, 1L, "Ben Ali", "Sami",
                StatutAffectation.RETIREE_ARCHIVEE, Instant.now(), Instant.now(), "Abandon");

        livrableResponse = new LivrableResponse(1L, 10L, TypeLivrable.DOCUMENTATION,
                "Rapport final", "https://git.esprit.tn/repo", null, true,
                2L, "Trabelsi", Instant.now());
    }

    // ── US-14 ────────────────────────────────────────────────────────

    @Test
    void ouvrirCandidatures_appelleService_retourneNoContent() {
        ResponseEntity<Void> response = controller.ouvrirCandidatures(10L);

        verify(candidatureService).ouvrirCandidatures(10L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ── US-16 ────────────────────────────────────────────────────────

    @Test
    void deposerCandidature_forceSujetIdDepuisUrl_retourneCreated() {
        when(candidatureService.deposerCandidature(any())).thenReturn(candidatureResponse);

        ResponseEntity<CandidatureResponse> response =
                controller.deposerCandidature(10L, new CandidatureRequest(999L, "Motivé"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(candidatureResponse, response.getBody());
        verify(candidatureService).deposerCandidature(
                argThat(req -> req.sujetId().equals(10L) && "Motivé".equals(req.messageEtudiant())));
    }

    // ── US-17 / US-18 ──────────────────────────────────────────────

    @Test
    void deciderCandidature_delegueAuService() {
        DecisionCandidatureRequest request = new DecisionCandidatureRequest(true, null);
        when(candidatureService.deciderCandidature(100L, request)).thenReturn(candidatureResponse);

        ResponseEntity<CandidatureResponse> response = controller.deciderCandidature(100L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(candidatureResponse, response.getBody());
    }

    @Test
    void accepterCandidature_appelleServiceAvecAccepterTrue() {
        when(candidatureService.deciderCandidature(eq(100L),
                argThat(r -> Boolean.TRUE.equals(r.accepter())))).thenReturn(candidatureResponse);

        ResponseEntity<CandidatureResponse> response = controller.accepterCandidature(100L);

        assertEquals(candidatureResponse, response.getBody());
    }

    @Test
    void refuserCandidature_appelleServiceAvecMotif() {
        when(candidatureService.deciderCandidature(eq(100L),
                argThat(r -> Boolean.FALSE.equals(r.accepter()) && "Profil insuffisant".equals(r.motifRefus()))))
                .thenReturn(candidatureResponse);

        ResponseEntity<CandidatureResponse> response = controller.refuserCandidature(100L,
                new DecisionCandidatureRequest(false, "Profil insuffisant"));

        assertEquals(candidatureResponse, response.getBody());
    }

    // ── US-19 + US-20 ──────────────────────────────────────────────

    @Test
    void fermerCandidatures_appelleService_retourneNoContent() {
        ResponseEntity<Void> response = controller.fermerCandidatures(10L);

        verify(candidatureService).fermerCandidatures(10L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ── US-22 ────────────────────────────────────────────────────────

    @Test
    void retirerEtudiant_delegueAuServiceEtRetourneAffectation() {
        RetraitEtudiantRequest request = new RetraitEtudiantRequest("Abandon");
        when(candidatureService.retirerEtudiant(200L, request)).thenReturn(affectationResponse);

        ResponseEntity<AffectationResponse> response = controller.retirerEtudiant(200L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(affectationResponse, response.getBody());
    }

    // ── US-23 ────────────────────────────────────────────────────────

    @Test
    void ajouterLivrable_retourneCreated() {
        LivrableRequest request = new LivrableRequest(TypeLivrable.DOCUMENTATION,
                "Rapport final", "https://git.esprit.tn/repo", null, true);
        when(candidatureService.ajouterLivrable(10L, request)).thenReturn(livrableResponse);

        ResponseEntity<LivrableResponse> response = controller.ajouterLivrable(10L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(livrableResponse, response.getBody());
    }

    // ── US-24 ────────────────────────────────────────────────────────

    @Test
    void declarerTerminaison_appelleService_retourneNoContent() {
        ResponseEntity<Void> response = controller.declarerTerminaison(10L);

        verify(candidatureService).declarerTerminaison(10L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ── Lectures ─────────────────────────────────────────────────────

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

    @Test
    void getLivrablesParSujet_retourneListe() {
        when(candidatureService.getLivrablesParSujet(10L)).thenReturn(List.of(livrableResponse));

        ResponseEntity<List<LivrableResponse>> response = controller.getLivrablesParSujet(10L);

        assertEquals(1, response.getBody().size());
    }
}
