package tn.esprit.backend_esprittech.controller.sujetprojet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.ReferenceItemRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/sujet-projets")
@Tag(name = "Sujets de projet", description = "Dépôt et consultation des sujets par l'enseignant connecté")
public class SujetProjetController {

    private final SujetProjetService sujetProjetService;
    private final SujetReferenceService referenceService;

    public SujetProjetController(SujetProjetService sujetProjetService,
                                 SujetReferenceService referenceService) {
        this.sujetProjetService = sujetProjetService;
        this.referenceService = referenceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Déposer un sujet", description = "Enregistre une demande de partage avec le statut Soumis en validation.")
    public ResponseEntity<SujetProjetResponse> creerSujet(@Valid @RequestBody SujetProjetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sujetProjetService.creerSujet(request));
    }

    @GetMapping("/mes-sujets")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes sujets", description = "Liste les sujets déposés par l'enseignant connecté.")
    public ResponseEntity<List<SujetProjetResponse>> getMesSujets(
            @RequestParam(required = false) CategorieSujet categorie,
            @RequestParam(required = false) StatutSujet statut) {
        return ResponseEntity.ok(sujetProjetService.getMesSujets(categorie, statut));
    }

    @GetMapping("/en-attente-validation")
    @PreAuthorize("hasAnyRole('CHEF_EQUIPE', 'ADMIN')")
    @Operation(summary = "Sujets en attente de validation",
            description = "Liste les sujets soumis par les encadrants de l'équipe du chef connecté (ou tous, pour l'admin).")
    public ResponseEntity<List<SujetProjetResponse>> getSujetsEnAttenteValidation() {
        return ResponseEntity.ok(sujetProjetService.getSujetsEnAttenteValidation());
    }

    // ==================== NEW METHOD ADDED HERE ====================
    @PostMapping("/{id}/terminer")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Déclarer la terminaison d'un projet",
            description = "Passe un sujet en cours de réalisation à 'REALISATION_TERMINEE'")
    public ResponseEntity<SujetProjetResponse> declarerTerminaison(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(sujetProjetService.declarerTerminaison(id));
    }
    // ============================================================

    @GetMapping("/admin/candidatures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sujets pour gestion des candidatures (admin)",
            description = "Liste tous les sujets validés ou plus avancés dans le cycle candidatures/réalisation.")
    public ResponseEntity<List<SujetProjetResponse>> getSujetsPourCandidatures() {
        return ResponseEntity.ok(sujetProjetService.getSujetsPourCandidatures());
    }

    @GetMapping("/disponibles")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sujets disponibles", description = "Catalogue des sujets validés et ouverts aux candidatures.")
    public ResponseEntity<List<SujetProjetResponse>> getSujetsDisponibles(
            @RequestParam(required = false) CategorieSujet categorie) {
        return ResponseEntity.ok(sujetProjetService.getSujetsDisponibles(categorie));
    }

    @GetMapping("/a-valider")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Sujets à valider", description = "Liste les demandes de sujets déposées par les membres de l'équipe.")
    public ResponseEntity<List<SujetProjetResponse>> getSujetsAValider(
            @RequestParam(required = false) CategorieSujet categorie,
            @RequestParam(required = false) StatutSujet statut) {
        return ResponseEntity.ok(sujetProjetService.getSujetsAValider(categorie, statut));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Valider un sujet", description = "Accepte la demande et publie le sujet dans le catalogue.")
    public ResponseEntity<SujetProjetResponse> validerSujet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(sujetProjetService.validerSujet(id));
    }

    @PostMapping("/{id}/invalider")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Refuser un sujet", description = "Rejette la demande avec un motif.")
    public ResponseEntity<SujetProjetResponse> invaliderSujet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id,
            @Valid @RequestBody SujetInvalidationRequest request) {
        return ResponseEntity.ok(sujetProjetService.invaliderSujet(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'un sujet")
    public ResponseEntity<SujetProjetResponse> getSujet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(sujetProjetService.getSujetById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Modifier un sujet", description = "Met à jour un sujet déposé par l'enseignant connecté.")
    public ResponseEntity<SujetProjetResponse> modifierSujet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id,
            @Valid @RequestBody SujetProjetRequest request) {
        return ResponseEntity.ok(sujetProjetService.modifierSujet(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Supprimer un sujet", description = "Supprime un sujet déposé par l'enseignant connecté.")
    public ResponseEntity<Void> supprimerSujet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        sujetProjetService.supprimerSujet(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/technologies")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Technologies disponibles", description = "Liste des technologies informatiques pour le formulaire.")
    public ResponseEntity<List<String>> getTechnologies() {
        return ResponseEntity.ok(sujetProjetService.getTechnologiesDisponibles());
    }

    @GetMapping("/suggestions/prerequis")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Suggestions de prérequis", description = "Choix par défaut modifiables dans le formulaire.")
    public ResponseEntity<List<String>> getPrerequisSuggestions() {
        return ResponseEntity.ok(sujetProjetService.getPrerequisSuggestions());
    }

    @GetMapping("/suggestions/domaines")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Suggestions de domaines", description = "Liste des domaines disponibles pour le formulaire.")
    public ResponseEntity<List<String>> getDomainesSuggestions() {
        return ResponseEntity.ok(sujetProjetService.getDomainesSuggestions());
    }

    @PostMapping("/references/domaines")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Ajouter un domaine", description = "Crée le domaine s'il n'existe pas encore.")
    public ResponseEntity<ReferenceItemResponse> suggestDomaine(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.findOrCreateDomaine(request.nom()));
    }

    @PostMapping("/references/prerequis")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Ajouter un prérequis", description = "Crée le prérequis s'il n'existe pas encore.")
    public ResponseEntity<ReferenceItemResponse> suggestPrerequis(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.findOrCreatePrerequis(request.nom()));
    }

    @PostMapping("/references/technologies")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Ajouter une technologie", description = "Crée la technologie si elle n'existe pas encore.")
    public ResponseEntity<ReferenceItemResponse> suggestTechnologie(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.findOrCreateTechnologie(request.nom()));
    }
}