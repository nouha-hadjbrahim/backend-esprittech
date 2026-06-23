package tn.esprit.backend_esprittech.controller.sujetprojet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.MockEncadrantResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.util.List;

@RestController
@RequestMapping("/api/sujet-projets")
@Tag(name = "Sujets de projet", description = "Dépôt et consultation des sujets par l'encadrant")
public class SujetProjetController {

    private final SujetProjetService sujetProjetService;

    public SujetProjetController(SujetProjetService sujetProjetService) {
        this.sujetProjetService = sujetProjetService;
    }

    @PostMapping
    @Operation(summary = "Déposer un sujet", description = "Enregistre une demande de partage avec le statut Soumis en validation.")
    public ResponseEntity<SujetProjetResponse> creerSujet(@Valid @RequestBody SujetProjetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sujetProjetService.creerSujet(request));
    }

    @GetMapping("/mes-sujets")
    @Operation(summary = "Mes sujets", description = "Liste les sujets déposés par l'encadrant mock connecté.")
    public ResponseEntity<List<SujetProjetResponse>> getMesSujets(
            @RequestParam(required = false) CategorieSujet categorie,
            @RequestParam(required = false) StatutSujet statut) {
        return ResponseEntity.ok(sujetProjetService.getMesSujets(categorie, statut));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un sujet")
    public ResponseEntity<SujetProjetResponse> getSujet(@PathVariable Long id) {
        return ResponseEntity.ok(sujetProjetService.getSujetById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un sujet", description = "Met à jour un sujet déposé par l'encadrant mock.")
    public ResponseEntity<SujetProjetResponse> modifierSujet(
            @PathVariable Long id,
            @Valid @RequestBody SujetProjetRequest request) {
        return ResponseEntity.ok(sujetProjetService.modifierSujet(id, request));
    }

    @GetMapping("/technologies")
    @Operation(summary = "Technologies disponibles", description = "Liste des technologies informatiques pour le formulaire.")
    public ResponseEntity<List<String>> getTechnologies() {
        return ResponseEntity.ok(sujetProjetService.getTechnologiesDisponibles());
    }

    @GetMapping("/mock-encadrant")
    @Operation(summary = "Encadrant mock", description = "Retourne l'encadrant statique utilisé pour les tests.")
    public ResponseEntity<MockEncadrantResponse> getMockEncadrant() {
        return ResponseEntity.ok(sujetProjetService.getMockEncadrant());
    }
}
