package tn.esprit.backend_esprittech.controller.historique;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.response.HistoriqueResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/historique")
@Tag(name = "Historique", description = "Consultation de l'historique des actions")
public class HistoriqueController {

    private final HistoriqueService historiqueService;

    public HistoriqueController(HistoriqueService historiqueService) {
        this.historiqueService = historiqueService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CI', 'ROLE_CHEF_EQUIPE')")
    @Operation(summary = "Recherche paginée avec filtres")
    public ResponseEntity<PagedResponse<HistoriqueResponse>> search(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) ActionType action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                historiqueService.search(entityType, entityId, action, actorId, dateFrom, dateTo, pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CI', 'ROLE_CHEF_EQUIPE')")
    @Operation(summary = "Historique d'une entité")
    public ResponseEntity<PagedResponse<HistoriqueResponse>> findByEntity(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(historiqueService.findByEntity(entityType, entityId, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_CI', 'ROLE_CHEF_EQUIPE')")
    @Operation(summary = "Actions d'un utilisateur")
    public ResponseEntity<PagedResponse<HistoriqueResponse>> findByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(historiqueService.findByUser(userId, pageable));
    }
}
