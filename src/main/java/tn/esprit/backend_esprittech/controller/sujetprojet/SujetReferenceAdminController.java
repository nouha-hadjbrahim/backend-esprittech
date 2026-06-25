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
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.ReferenceItemRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

@Validated
@RestController
@RequestMapping("/api/sujet-projets/admin/references")
@Tag(name = "Références sujets (admin)", description = "CRUD des domaines, prérequis et technologies")
@PreAuthorize("hasRole('ADMIN')")
public class SujetReferenceAdminController {

    private final SujetReferenceService referenceService;

    public SujetReferenceAdminController(SujetReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/domaines")
    @Operation(summary = "Lister les domaines (paginé)")
    public ResponseEntity<PagedResponse<ReferenceItemResponse>> getDomaines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(referenceService.getDomaines(page, size, search));
    }

    @PostMapping("/domaines")
    @Operation(summary = "Créer un domaine")
    public ResponseEntity<ReferenceItemResponse> createDomaine(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createDomaine(request));
    }

    @PutMapping("/domaines/{id}")
    @Operation(summary = "Modifier un domaine")
    public ResponseEntity<ReferenceItemResponse> updateDomaine(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.ok(referenceService.updateDomaine(id, request));
    }

    @DeleteMapping("/domaines/{id}")
    @Operation(summary = "Supprimer un domaine")
    public ResponseEntity<Void> deleteDomaine(@PathVariable @Positive Long id) {
        referenceService.deleteDomaine(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/prerequis")
    @Operation(summary = "Lister les prérequis (paginé)")
    public ResponseEntity<PagedResponse<ReferenceItemResponse>> getPrerequis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(referenceService.getPrerequis(page, size, search));
    }

    @PostMapping("/prerequis")
    @Operation(summary = "Créer un prérequis")
    public ResponseEntity<ReferenceItemResponse> createPrerequis(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createPrerequis(request));
    }

    @PutMapping("/prerequis/{id}")
    @Operation(summary = "Modifier un prérequis")
    public ResponseEntity<ReferenceItemResponse> updatePrerequis(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.ok(referenceService.updatePrerequis(id, request));
    }

    @DeleteMapping("/prerequis/{id}")
    @Operation(summary = "Supprimer un prérequis")
    public ResponseEntity<Void> deletePrerequis(@PathVariable @Positive Long id) {
        referenceService.deletePrerequis(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/technologies")
    @Operation(summary = "Lister les technologies (paginé)")
    public ResponseEntity<PagedResponse<ReferenceItemResponse>> getTechnologies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(referenceService.getTechnologies(page, size, search));
    }

    @PostMapping("/technologies")
    @Operation(summary = "Créer une technologie")
    public ResponseEntity<ReferenceItemResponse> createTechnologie(@Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createTechnologie(request));
    }

    @PutMapping("/technologies/{id}")
    @Operation(summary = "Modifier une technologie")
    public ResponseEntity<ReferenceItemResponse> updateTechnologie(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReferenceItemRequest request) {
        return ResponseEntity.ok(referenceService.updateTechnologie(id, request));
    }

    @DeleteMapping("/technologies/{id}")
    @Operation(summary = "Supprimer une technologie")
    public ResponseEntity<Void> deleteTechnologie(@PathVariable @Positive Long id) {
        referenceService.deleteTechnologie(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/counts")
    @Operation(summary = "Compteurs pour les onglets")
    public ResponseEntity<ReferenceCountsResponse> getCounts() {
        return ResponseEntity.ok(new ReferenceCountsResponse(
                referenceService.countDomaines(),
                referenceService.countPrerequis(),
                referenceService.countTechnologies()
        ));
    }

    public record ReferenceCountsResponse(long domaines, long prerequis, long technologies) {
    }
}
