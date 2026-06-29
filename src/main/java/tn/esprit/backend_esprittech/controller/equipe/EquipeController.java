package tn.esprit.backend_esprittech.controller.equipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.equipe.AjouterMembresRequest;
import tn.esprit.backend_esprittech.dto.equipe.AssignChefRequest;
import tn.esprit.backend_esprittech.dto.equipe.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.equipe.EquipeRechercheService;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/equipes")
@Tag(name = "Équipes de recherche", description = "Gestion des équipes de recherche")
public class EquipeController {

    private final EquipeRechercheService equipeService;
    private final UserService userService;
    private final UserMapper userMapper;

    public EquipeController(EquipeRechercheService equipeService,
                            UserService userService,
                            UserMapper userMapper) {
        this.equipeService = equipeService;
        this.userService = userService;
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

    @GetMapping("/users/eligible")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Utilisateurs éligibles",
               description = "Liste paginée des utilisateurs éligibles pour être chef ou membre d'une équipe donnée.")
    public ResponseEntity<PagedResponse<UserResponse>> getEligibleUsers(
            @RequestParam(required = false) Long equipeId,
            @RequestParam(defaultValue = "MEMBER") String type,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nom", "prenom"));
        Page<User> users = userService.listEligible(search, equipeId, type, pageable);
        return ResponseEntity.ok(PagedResponse.from(users, userMapper::toResponse));
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
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Ajouter des membres à une équipe")
    public ResponseEntity<EquipeResponse> ajouterMembres(
            @PathVariable Long id,
            @Valid @RequestBody AjouterMembresRequest request) {

        return ResponseEntity.ok(equipeService.ajouterMembres(id, request.memberIds()));
    }

    @DeleteMapping("/{id}/membres/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Retirer un membre d'une équipe")
    public ResponseEntity<EquipeResponse> retirerMembre(
            @PathVariable Long id,
            @PathVariable Long userId) {

        return ResponseEntity.ok(equipeService.retirerMembre(id, userId));
    }
}
