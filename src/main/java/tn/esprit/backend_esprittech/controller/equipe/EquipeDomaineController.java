package tn.esprit.backend_esprittech.controller.equipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.service.equipe.EquipeDomaineService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/equipes/admin/domaines")
@Tag(name = "Domaines d'équipe (admin)", description = "CRUD des domaines pour les équipes de recherche")
@PreAuthorize("hasRole('ADMIN')")
public class EquipeDomaineController {

    private final EquipeDomaineService service;

    public EquipeDomaineController(EquipeDomaineService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les domaines (paginé)")
    public ResponseEntity<PagedResponse<EquipeDomaineResponse>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getPage(page, size, search));
    }

    @GetMapping("/all")
    @Operation(summary = "Lister tous les domaines (non paginé, pour dropdowns)")
    public ResponseEntity<List<EquipeDomaineResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/count")
    @Operation(summary = "Nombre total de domaines")
    public ResponseEntity<Long> getCount() {
        return ResponseEntity.ok(service.count());
    }

    @PostMapping
    @Operation(summary = "Créer un domaine")
    public ResponseEntity<EquipeDomaineResponse> create(@Valid @RequestBody EquipeDomaineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un domaine")
    public ResponseEntity<EquipeDomaineResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipeDomaineRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un domaine")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
