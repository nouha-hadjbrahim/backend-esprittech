package tn.esprit.backend_esprittech.controller.equipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.equipe.AffiliationEnseignantResponse;
import tn.esprit.backend_esprittech.dto.equipe.TraiterAffiliationRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.service.equipe.AffiliationEnseignantService;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/equipes")
@Tag(name = "Affiliations Enseignant", description = "Gestion des demandes d'affiliation des enseignants aux équipes")
public class AffiliationController {

    private final AffiliationEnseignantService affiliationService;
    private final UserService userService;

    public AffiliationController(AffiliationEnseignantService affiliationService,
                                 UserService userService) {
        this.affiliationService = affiliationService;
        this.userService = userService;
    }

    @PostMapping("/{equipeId}/affiliations")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Demander une affiliation à une équipe")
    public ResponseEntity<AffiliationEnseignantResponse> demanderAffiliation(@PathVariable Long equipeId) {
        User enseignant = userService.getCurrentUser();
        AffiliationEnseignantResponse response = affiliationService.demander(equipeId, enseignant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/affiliations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toutes les demandes d'affiliation (admin)")
    public ResponseEntity<List<AffiliationEnseignantResponse>> getAllDemandes() {
        return ResponseEntity.ok(affiliationService.getAllDemandes());
    }

    @GetMapping("/affiliations/mine")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes demandes d'affiliation")
    public ResponseEntity<List<AffiliationEnseignantResponse>> mesDemandes() {
        User enseignant = userService.getCurrentUser();
        return ResponseEntity.ok(affiliationService.getMesDemandes(enseignant));
    }

    @GetMapping("/{equipeId}/affiliations")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Lister les demandes d'affiliation d'une équipe")
    public ResponseEntity<List<AffiliationEnseignantResponse>> getDemandesByEquipe(@PathVariable Long equipeId) {
        return ResponseEntity.ok(affiliationService.getDemandesByEquipe(equipeId));
    }

    @PutMapping("/{equipeId}/affiliations/{affiliationId}")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_EQUIPE')")
    @Operation(summary = "Traiter une demande d'affiliation (accepter/refuser)")
    public ResponseEntity<AffiliationEnseignantResponse> traiterDemande(
            @PathVariable Long equipeId,
            @PathVariable Long affiliationId,
            @Valid @RequestBody TraiterAffiliationRequest request) {
        User decider = userService.getCurrentUser();
        AffiliationEnseignantResponse response = affiliationService.traiter(
                affiliationId, equipeId, decider, request.statut(), request.motifDecision());
        return ResponseEntity.ok(response);
    }
}
