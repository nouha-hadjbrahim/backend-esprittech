package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.List;

/**
 * Consultation des équipes de recherche (accessible à tout utilisateur authentifié).
 */
@RestController
@RequestMapping("/api/equipes")
@Tag(name = "Équipes de recherche", description = "Consultation des équipes et de leurs membres")
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

    @GetMapping
    @Operation(summary = "Lister les équipes")
    public ResponseEntity<List<EquipeResponse>> listerEquipes() {
        List<EquipeResponse> equipes = equipeService.listerToutes().stream()
                .map(e -> equipeMapper.toResponse(e, equipeService.compterMembres(e)))
                .toList();
        return ResponseEntity.ok(equipes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une équipe")
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
}
