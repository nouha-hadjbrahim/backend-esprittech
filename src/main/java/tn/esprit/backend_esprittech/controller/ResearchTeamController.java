package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.request.CreateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamResponse;
import tn.esprit.backend_esprittech.service.ResearchTeamService;

import java.util.List;

@RestController
@RequestMapping("/api/research-teams")
@Tag(name = "Equipes de recherche", description = "Création, consultation, mise à jour et suppression des équipes")
public class ResearchTeamController {

    private final ResearchTeamService researchTeamService;

    public ResearchTeamController(ResearchTeamService researchTeamService) {
        this.researchTeamService = researchTeamService;
    }

    @PostMapping
    @Operation(summary = "Créer une équipe", description = "Crée une équipe de recherche avec un chef et des membres optionnels.")
    public ResponseEntity<ResearchTeamResponse> create(@Valid @RequestBody CreateResearchTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(researchTeamService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lister les équipes", description = "Retourne toutes les équipes de recherche.")
    public ResponseEntity<List<ResearchTeamResponse>> findAll() {
        return ResponseEntity.ok(researchTeamService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une équipe", description = "Retourne une équipe de recherche par identifiant.")
    public ResponseEntity<ResearchTeamResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(researchTeamService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une équipe", description = "Met à jour une équipe de recherche existante.")
    public ResponseEntity<ResearchTeamResponse> update(@PathVariable Long id,
                                                       @RequestBody UpdateResearchTeamRequest request) {
        return ResponseEntity.ok(researchTeamService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une équipe", description = "Supprime une équipe de recherche.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        researchTeamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}