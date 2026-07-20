package tn.esprit.backend_esprittech.controller.critereeval;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.service.critereeval.EvaluationService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.util.List;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final SujetProjetService sujetProjetService;

    @PostMapping("/{id}/evaluations/calculer")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE')")
    public EvaluationResponse calculerEvaluation(@PathVariable Long id) {
        return evaluationService.calculateAutomaticEvaluation(id, "Recalcul manuel");
    }

    @PostMapping("/{id}/calculer-score")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE')")
    public EvaluationResponse calculerScore(@PathVariable Long id) {
        return evaluationService.calculateAutomaticEvaluation(id, "Recalcul manuel");
    }

    @GetMapping("/{id}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public EvaluationResponse getLatestEvaluation(@PathVariable Long id) {
        return evaluationService.getLatestEvaluationByProjet(id);
    }

    @GetMapping("/{id}/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public List<EvaluationResponse> getEvaluationHistory(@PathVariable Long id) {
        return evaluationService.getEvaluationHistoryByProjet(id);
    }

    @PostMapping("/{id}/evaluation/valider")
    @PreAuthorize("hasAnyRole('ADMIN','CI')")
    public EvaluationResponse validerEvaluation(
            @PathVariable Long id,
            @RequestBody(required = false) EvaluationValidationRequest request
    ) {
        return evaluationService.validateLatestEvaluation(id, request);
    }

    @PostMapping("/{id}/evaluation/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN','CI')")
    public EvaluationResponse rejeterEvaluation(
            @PathVariable Long id,
            @RequestBody(required = false) EvaluationValidationRequest request
    ) {
        return evaluationService.rejectLatestEvaluation(id, request);
    }

    @PostMapping("/{id}/evaluation/override")
    @PreAuthorize("hasRole('ADMIN')")
    public EvaluationResponse overrideEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationOverrideRequest request
    ) {
        return evaluationService.overrideLatestEvaluation(id, request);
    }

    @GetMapping("/evaluables")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SujetProjetResponse> getEvaluableProjects() {
        return sujetProjetService.getProjetsEvaluables();
    }
}