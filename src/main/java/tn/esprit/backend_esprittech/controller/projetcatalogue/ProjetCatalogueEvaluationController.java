package tn.esprit.backend_esprittech.controller.projetcatalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;
import tn.esprit.backend_esprittech.service.critereeval.EvaluationService;

import java.util.List;

/**
 * Évaluation d'un projet du catalogue applicatif, exposée directement par l'identifiant du
 * projet catalogue. Le moteur d'évaluation ML est le même que pour les sujets, mais les
 * données projet et les livrables sont lus depuis {@code ProjetCatalogue} / {@code LivrableCatalogue} :
 * un projet catalogue déposé manuellement (sans sujet d'origine) est donc évaluable directement.
 * Le score calculé est synchronisé dans {@code ProjetCatalogue.score}.
 *
 * <p>Séparé de {@code EvaluationController} (mappé sur {@code /api/projets}, indexé par
 * l'identifiant de sujet) pour éviter toute ambiguïté sur la sémantique de l'identifiant.</p>
 */
@RestController
@RequestMapping("/api/projets-catalogue")
@RequiredArgsConstructor
public class ProjetCatalogueEvaluationController {

    private final EvaluationService evaluationService;

    /** Recalcule le score d'un projet catalogue validé (dépôt manuel ou issu d'un sujet). */
    @PostMapping("/{id}/calculer-score")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE')")
    public EvaluationResponse calculerScore(@PathVariable Long id) {
        return evaluationService.calculateAutomaticEvaluationForCatalogue(id, "Recalcul manuel projet catalogue");
    }

    /** Renvoie la dernière évaluation disponible pour un projet catalogue. */
    @GetMapping("/{id}/evaluation")
    @PreAuthorize("isAuthenticated()")
    public EvaluationResponse getLatestEvaluation(@PathVariable Long id) {
        return evaluationService.getLatestEvaluationByProjetCatalogue(id);
    }

    /** Renvoie l'historique des evaluations d'un projet catalogue. */
    @GetMapping("/{id}/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE','CI')")
    public List<EvaluationResponse> getEvaluationHistory(@PathVariable Long id) {
        return evaluationService.getEvaluationHistoryByProjetCatalogue(id);
    }

    /** Renvoie une evaluation precise rattachee au projet catalogue. */
    @GetMapping("/{id}/evaluations/{evaluationId}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE','CI')")
    public EvaluationResponse getEvaluationDetail(@PathVariable Long id, @PathVariable Long evaluationId) {
        return evaluationService.getEvaluationDetailByProjetCatalogue(id, evaluationId);
    }
}
