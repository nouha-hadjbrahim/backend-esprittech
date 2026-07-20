package tn.esprit.backend_esprittech.controller.projetcatalogue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.livrable.CataloguePublicLivrableResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.CreateProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.RefuserProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.service.critereeval.EvaluationService;
import tn.esprit.backend_esprittech.service.livrable.LivrableCatalogueService;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.util.List;

/**
 * API du catalogue applicatif : dépôt d'un projet complet par l'enseignant affilié,
 * validation/refus par le chef d'équipe, et consultation du catalogue public.
 * Les rôles sont contrôlés via {@link PreAuthorize} ; les règles fines (affiliation,
 * chef de l'équipe, encadrant du projet) sont vérifiées dans le service.
 */
@Validated
@RestController
@Tag(name = "Catalogue applicatif", description = "Dépôt, validation et consultation des projets complets")
public class ProjetCatalogueController {

    private final ProjetCatalogueService projetService;
    private final EvaluationService evaluationService;
    private final LivrableCatalogueService livrableCatalogueService;

    public ProjetCatalogueController(ProjetCatalogueService projetService,
                                    EvaluationService evaluationService,
                                    LivrableCatalogueService livrableCatalogueService) {
        this.projetService = projetService;
        this.evaluationService = evaluationService;
        this.livrableCatalogueService = livrableCatalogueService;
    }

    // ── Enseignant affilié ────────────────────────────────────────────────────

    @PostMapping("/api/projets")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Déposer un projet", description = "Soumet un projet complet (statut SoumisEnValidation forcé serveur).")
    public ResponseEntity<ProjetCatalogueResponse> creerProjet(@Valid @RequestBody CreateProjetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.creerProjet(request));
    }

    @GetMapping("/api/projets/mes-projets")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Mes projets", description = "Liste les projets déposés par l'enseignant connecté.")
    public ResponseEntity<List<ProjetCardResponse>> mesProjets() {
        return ResponseEntity.ok(projetService.mesProjets());
    }

    @GetMapping("/api/projets/references/domaines")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Domaines disponibles", description = "Référentiel des domaines pour le formulaire de dépôt.")
    public ResponseEntity<List<ReferenceItemResponse>> getDomaines() {
        return ResponseEntity.ok(projetService.getDomaines());
    }

    @GetMapping("/api/projets/references/technologies")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Technologies disponibles", description = "Référentiel des technologies pour le formulaire de dépôt.")
    public ResponseEntity<List<ReferenceItemResponse>> getTechnologies() {
        return ResponseEntity.ok(projetService.getTechnologies());
    }

    @GetMapping("/api/projets/references/prerequis")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    @Operation(summary = "Prérequis disponibles", description = "Référentiel des prérequis pour le formulaire de dépôt.")
    public ResponseEntity<List<ReferenceItemResponse>> getPrerequis() {
        return ResponseEntity.ok(projetService.getPrerequis());
    }

    // ── Chef d'équipe ─────────────────────────────────────────────────────────

    @GetMapping("/api/projets/a-valider")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Projets à valider", description = "Projets SoumisEnValidation des membres de l'équipe du chef connecté.")
    public ResponseEntity<List<ProjetCardResponse>> projetsAValider() {
        return ResponseEntity.ok(projetService.projetsAValider());
    }

    @PutMapping("/api/projets/{id}/valider")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Valider un projet", description = "Passe un projet de SoumisEnValidation à Valide (publication au catalogue).")
    public ResponseEntity<ProjetCatalogueResponse> valider(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(projetService.valider(id));
    }

    @PutMapping("/api/projets/{id}/refuser")
    @PreAuthorize("hasRole('CHEF_EQUIPE')")
    @Operation(summary = "Refuser un projet", description = "Passe un projet de SoumisEnValidation à Invalide (motif obligatoire).")
    public ResponseEntity<ProjetCatalogueResponse> refuser(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id,
            @Valid @RequestBody RefuserProjetRequest request) {
        return ResponseEntity.ok(projetService.refuser(id, request));
    }

    // ── Administration (admin) ─────────────────────────────────────────────────

    @GetMapping("/api/projets/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tous les projets", description = "Liste tous les projets du catalogue applicatif, quel que soit leur statut.")
    public ResponseEntity<List<ProjetCardResponse>> tousLesProjets() {
        return ResponseEntity.ok(projetService.tousLesProjets());
    }

    // ── Détail d'un projet (encadrant / chef / admin) ──────────────────────────

    @GetMapping("/api/projets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'un projet", description = "Accessible à l'encadrant du projet, au chef de l'équipe ou à l'admin.")
    public ResponseEntity<ProjetDetailsResponse> detailsProjet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(projetService.detailsProjet(id));
    }

    @DeleteMapping("/api/projets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un projet", description = "Supprime définitivement un projet du catalogue (admin).")
    public ResponseEntity<Void> supprimerProjet(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        projetService.supprimerProjetAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    // ── Catalogue public (tout authentifié) ────────────────────────────────────

    @GetMapping("/api/catalogue")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Catalogue public", description = "Projets validés et industrialisés, avec filtres optionnels.")
    public ResponseEntity<List<ProjetCardResponse>> catalogue(
            @RequestParam(required = false) TypeProjet type,
            @RequestParam(required = false) Long domaineId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(projetService.catalogue(type, domaineId, annee, search));
    }

    @GetMapping("/api/catalogue/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail catalogue", description = "Détail public d'un projet visible dans le catalogue.")
    public ResponseEntity<ProjetDetailsResponse> detailsCatalogue(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return ResponseEntity.ok(projetService.detailsCatalogue(id));
    }

    @GetMapping("/api/catalogue/{id}/evaluation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Évaluation catalogue",
            description = "Dernière évaluation ML du projet publié (catalogue ou sujet d'origine).")
    public EvaluationResponse evaluationCatalogue(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return evaluationService.getLatestEvaluationForPublishedCatalogue(id);
    }

    @GetMapping("/api/catalogue/{id}/livrables")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Livrables catalogue",
            description = "Livrables du projet publié ; sinon ceux du sujet d'origine.")
    public List<CataloguePublicLivrableResponse> livrablesCatalogue(
            @PathVariable @Positive(message = "L'identifiant doit être positif") Long id) {
        return livrableCatalogueService.findPublishedForPublicCatalogue(id);
    }

    @GetMapping("/api/catalogue/{projetId}/livrables/{livrableId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Télécharger un livrable catalogue",
            description = "Télécharge un fichier catalogue ou hérité du sujet (fromSujet=true).")
    public ResponseEntity<InputStreamResource> downloadLivrableCatalogue(
            @PathVariable @Positive Long projetId,
            @PathVariable @Positive Long livrableId,
            @RequestParam(defaultValue = "false") boolean fromSujet) {
        LivrableCatalogueService.LivrableDownload download =
                livrableCatalogueService.downloadPublished(projetId, livrableId, fromSujet);
        MediaType mediaType = download.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }
}
