package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.request.AjouterMembresRequest;
import tn.esprit.backend_esprittech.dto.request.AssignChefRequest;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.List;

@RestController
@RequestMapping("/api/equipes")
@Tag(name = "Équipes de recherche", description = "Gestion des équipes de recherche")
public class EquipeController {

    private final EquipeRechercheService equipeService;
    private final UserMapper userMapper;

    public EquipeController(EquipeRechercheService equipeService,
                            UserMapper userMapper) {
        this.equipeService = equipeService;
        this.userMapper = userMapper;
    }

    // =====================================================
    // READ OPERATIONS (ADMIN + USERS)
    // =====================================================

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les équipes")
    public ResponseEntity<List<EquipeResponse>> listerEquipes() {
        return ResponseEntity.ok(equipeService.listerToutes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'une équipe")
    public ResponseEntity<EquipeResponse> getEquipe(@PathVariable Long id) {
        return ResponseEntity.ok(equipeService.findById(id));
    }

    @GetMapping("/{id}/membres")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Membres d'une équipe")
    public ResponseEntity<List<UserResponse>> getMembres(@PathVariable Long id) {
        List<UserResponse> membres = equipeService.getMembres(id).stream()
                .map(userMapper::toResponse)
                .toList();

        return ResponseEntity.ok(membres);
    }

    // =====================================================
    // WRITE OPERATIONS (ADMIN ONLY)
    // =====================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une équipe")
    public ResponseEntity<EquipeResponse> create(
            @Valid @RequestBody CreateEquipeRechercheRequest request) {

        EquipeResponse response = equipeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Mettre à jour une équipe")
    public ResponseEntity<EquipeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEquipeRechercheRequest request) {

        EquipeResponse response = equipeService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une équipe")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/chef")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assigner un chef d'équipe")
    public ResponseEntity<EquipeResponse> assignerChef(
            @PathVariable Long id,
            @Valid @RequestBody AssignChefRequest request) {

        return ResponseEntity.ok(equipeService.assignerChef(id, request.chefId()));
    }

    @DeleteMapping("/{id}/chef")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Révoquer le chef d'une équipe")
    public ResponseEntity<EquipeResponse> retirerChef(@PathVariable Long id) {
        return ResponseEntity.ok(equipeService.retirerChef(id));
    }

    @PostMapping("/{id}/membres")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajouter des membres à une équipe")
    public ResponseEntity<EquipeResponse> ajouterMembres(
            @PathVariable Long id,
            @Valid @RequestBody AjouterMembresRequest request) {

        return ResponseEntity.ok(equipeService.ajouterMembres(id, request.memberIds()));
    }

    @DeleteMapping("/{id}/membres/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retirer un membre d'une équipe")
    public ResponseEntity<EquipeResponse> retirerMembre(
            @PathVariable Long id,
            @PathVariable Long userId) {

        return ResponseEntity.ok(equipeService.retirerMembre(id, userId));
    }
}