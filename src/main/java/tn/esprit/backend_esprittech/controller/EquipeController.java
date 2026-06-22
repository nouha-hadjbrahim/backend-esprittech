package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.List;

/**
 * Gestion des équipes de recherche :
 * création / consultation / mise à jour / suppression (accessible selon les rôles).
 */
@RestController
@RequestMapping("/api/equipes")
@Tag(name = "Équipes de recherche", description = "Création, consultation, mise à jour et suppression des équipes")
public class EquipeController {

    private final EquipeRechercheService equipeService;
    private final EquipeMapper equipeMapper;
    private final UserMapper userMapper;

    public EquipeController(EquipeRechercheService equipeService,
                            EquipeMapper equipeMapper,
                            UserMapper userMapper) {
        this.equipeService = equipeService;
        this.equipeMapper = equipeMapper;
        this.userMapper = userMapper;
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Lister les équipes", description = "Retourne toutes les équipes de recherche.")
    public ResponseEntity<List<EquipeResponse>> listerEquipes() {
        List<EquipeResponse> equipes = equipeService.listerToutes().stream()
                .map(e -> equipeMapper.toResponse(e, equipeService.compterMembres(e)))
                .toList();
        return ResponseEntity.ok(equipes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une équipe", description = "Retourne une équipe de recherche par identifiant.")
    public ResponseEntity<EquipeResponse> getEquipe(@PathVariable Long id) {
        EquipeRecherche equipe = equipeService.getEntite(id);
        return ResponseEntity.ok(equipeMapper.toResponse(equipe, equipeService.compterMembres(equipe)));
    }

    @GetMapping("/{id}/membres")
    @Operation(summary = "Membres d'une équipe", description = "Chef + enseignants affiliés (statut ACCEPTEE).")
    public ResponseEntity<List<UserResponse>> getMembres(@PathVariable Long id) {
        List<UserResponse> membres = equipeService.getMembres(id).stream()
                .map(userMapper::toResponse)
                .toList();
        return ResponseEntity.ok(membres);
    }

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Créer une équipe", description = "Crée une équipe de recherche avec un chef et des membres optionnels.")
    public ResponseEntity<EquipeResponse> create(@Valid @RequestBody CreateEquipeRechercheRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une équipe", description = "Met à jour une équipe de recherche existante.")
    public ResponseEntity<EquipeResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateEquipeRechercheRequest request) {
        return ResponseEntity.ok(equipeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une équipe", description = "Supprime une équipe de recherche.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}