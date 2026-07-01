package tn.esprit.backend_esprittech.controller.candidature;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.service.candidature.CandidatureService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    // US-14 : Ouvrir candidatures
    @PostMapping("/sujets/{sujetId}/ouvrir-candidatures")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<Void> ouvrirCandidatures(@PathVariable Long sujetId) {
        candidatureService.ouvrirCandidatures(sujetId);
        return ResponseEntity.noContent().build();
    }

    // US-16 : Déposer candidature
    @PostMapping("/sujets/{sujetId}/candidatures")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<CandidatureResponse> deposerCandidature(
            @PathVariable Long sujetId,
            @Valid @RequestBody CandidatureRequest request) {
        CandidatureRequest withSujetId = new CandidatureRequest(sujetId, request.messageEtudiant());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatureService.deposerCandidature(withSujetId));
    }

    // US-17 & US-18 : Décider candidature
    @PostMapping("/candidatures/{candidatureId}/decider")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<CandidatureResponse> deciderCandidature(
            @PathVariable Long candidatureId,
            @Valid @RequestBody DecisionCandidatureRequest request) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId, request));
    }

    @PostMapping("/candidatures/{candidatureId}/accepter")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<CandidatureResponse> accepterCandidature(@PathVariable Long candidatureId) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId,
                new DecisionCandidatureRequest(true, null)));
    }

    @PostMapping("/candidatures/{candidatureId}/refuser")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<CandidatureResponse> refuserCandidature(
            @PathVariable Long candidatureId,
            @Valid @RequestBody DecisionCandidatureRequest request) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId,
                new DecisionCandidatureRequest(false, request.motifRefus())));
    }

    // US-19 + US-20 : Fermer candidatures
    @PostMapping("/sujets/{sujetId}/fermer-candidatures")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<Void> fermerCandidatures(@PathVariable Long sujetId) {
        candidatureService.fermerCandidatures(sujetId);
        return ResponseEntity.noContent().build();
    }

    // US-22 : Retrait d'étudiant
    @PostMapping("/affectations/{affectationId}/retirer")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<AffectationResponse> retirerEtudiant(
            @PathVariable Long affectationId,
            @Valid @RequestBody RetraitEtudiantRequest request) {
        return ResponseEntity.ok(candidatureService.retirerEtudiant(affectationId, request));
    }

    // Lectures
    @GetMapping("/sujets/{sujetId}/candidatures")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'CHEF_EQUIPE', 'ADMIN')")
    public ResponseEntity<List<CandidatureResponse>> getCandidaturesParSujet(@PathVariable Long sujetId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesParSujet(sujetId));
    }

    @GetMapping("/candidatures/mes-candidatures")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures() {
        return ResponseEntity.ok(candidatureService.getMesCandidatures());
    }

    @DeleteMapping("/candidatures/{candidatureId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<Void> retirerMaCandidature(@PathVariable Long candidatureId) {
        candidatureService.retirerMaCandidature(candidatureId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sujets/{sujetId}/affectations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AffectationResponse>> getAffectationsParSujet(@PathVariable Long sujetId) {
        return ResponseEntity.ok(candidatureService.getAffectationsParSujet(sujetId));
    }
}