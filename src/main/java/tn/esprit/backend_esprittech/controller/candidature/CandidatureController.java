package tn.esprit.backend_esprittech.controller.candidature;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.LivrableRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.LivrableResponse;
import tn.esprit.backend_esprittech.service.candidature.CandidatureService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    // ── US-14 : Ouvrir candidatures ──────────────────────────────────
    // POST /api/sujets/{id}/ouvrir-candidatures
    @PostMapping("/sujets/{sujetId}/ouvrir-candidatures")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<Void> ouvrirCandidatures(@PathVariable Long sujetId) {
        candidatureService.ouvrirCandidatures(sujetId);
        return ResponseEntity.noContent().build();
    }

    // ── US-16 : Étudiant dépose une candidature ──────────────────────
    // POST /api/sujets/{id}/candidatures
    @PostMapping("/sujets/{sujetId}/candidatures")
    @PreAuthorize("hasRole('ROLE_ETUDIANT')")
    public ResponseEntity<CandidatureResponse> deposerCandidature(
            @PathVariable Long sujetId,
            @Valid @RequestBody CandidatureRequest request) {
        // sujetId dans l'URL est la source d'autorité ; le body peut l'omettre
        CandidatureRequest withSujetId = new CandidatureRequest(sujetId, request.messageEtudiant());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatureService.deposerCandidature(withSujetId));
    }

    // ── US-17 & US-18 : Accepter ou refuser une candidature ─────────
    // POST /api/candidatures/{id}/accepter  (body: { "accepter": true })
    // POST /api/candidatures/{id}/refuser   (body: { "accepter": false, "motifRefus": "..." })
    @PostMapping("/candidatures/{candidatureId}/decider")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<CandidatureResponse> deciderCandidature(
            @PathVariable Long candidatureId,
            @Valid @RequestBody DecisionCandidatureRequest request) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId, request));
    }

    // Alias pratiques pour correspondre aux endpoints indicatifs du cahier des charges
    @PostMapping("/candidatures/{candidatureId}/accepter")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<CandidatureResponse> accepterCandidature(@PathVariable Long candidatureId) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId,
                new DecisionCandidatureRequest(true, null)));
    }

    @PostMapping("/candidatures/{candidatureId}/refuser")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<CandidatureResponse> refuserCandidature(
            @PathVariable Long candidatureId,
            @Valid @RequestBody DecisionCandidatureRequest request) {
        return ResponseEntity.ok(candidatureService.deciderCandidature(candidatureId,
                new DecisionCandidatureRequest(false, request.motifRefus())));
    }

    // ── US-19 + US-20 : Fermer candidatures (+ bascule auto réalisation)
    // POST /api/sujets/{id}/fermer-candidatures
    @PostMapping("/sujets/{sujetId}/fermer-candidatures")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<Void> fermerCandidatures(@PathVariable Long sujetId) {
        candidatureService.fermerCandidatures(sujetId);
        return ResponseEntity.noContent().build();
    }

    // ── US-22 : Retrait d'un étudiant (archivage) ────────────────────
    @PostMapping("/affectations/{affectationId}/retirer")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<AffectationResponse> retirerEtudiant(
            @PathVariable Long affectationId,
            @Valid @RequestBody RetraitEtudiantRequest request) {
        return ResponseEntity.ok(candidatureService.retirerEtudiant(affectationId, request));
    }

    // ── US-23 : Dépôt de livrables ───────────────────────────────────
    @PostMapping("/sujets/{sujetId}/livrables")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<LivrableResponse> ajouterLivrable(
            @PathVariable Long sujetId,
            @Valid @RequestBody LivrableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatureService.ajouterLivrable(sujetId, request));
    }

    // ── US-24 : Déclaration de terminaison ───────────────────────────
    // POST /api/projets/{id}/terminer
    @PostMapping("/projets/{sujetId}/terminer")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE')")
    public ResponseEntity<Void> declarerTerminaison(@PathVariable Long sujetId) {
        candidatureService.declarerTerminaison(sujetId);
        return ResponseEntity.noContent().build();
    }

    // ── Lectures ─────────────────────────────────────────────────────

    @GetMapping("/sujets/{sujetId}/candidatures")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE', 'ROLE_ADMIN')")
    public ResponseEntity<List<CandidatureResponse>> getCandidaturesParSujet(@PathVariable Long sujetId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesParSujet(sujetId));
    }

    @GetMapping("/candidatures/mes-candidatures")
    @PreAuthorize("hasRole('ROLE_ETUDIANT')")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures() {
        return ResponseEntity.ok(candidatureService.getMesCandidatures());
    }

    @GetMapping("/sujets/{sujetId}/affectations")
    @PreAuthorize("hasAnyRole('ROLE_ENCADRANT', 'ROLE_CHEF_EQUIPE_RECHERCHE', 'ROLE_ADMIN')")
    public ResponseEntity<List<AffectationResponse>> getAffectationsParSujet(@PathVariable Long sujetId) {
        return ResponseEntity.ok(candidatureService.getAffectationsParSujet(sujetId));
    }

    @GetMapping("/sujets/{sujetId}/livrables")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LivrableResponse>> getLivrablesParSujet(@PathVariable Long sujetId) {
        return ResponseEntity.ok(candidatureService.getLivrablesParSujet(sujetId));
    }
}
