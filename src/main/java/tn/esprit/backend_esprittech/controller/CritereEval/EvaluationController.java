package tn.esprit.backend_esprittech.controller.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.util.List;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final SujetProjetService sujetProjetService;

    @PostMapping("/{id}/evaluations/calculer")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public EvaluationResponse calculerEvaluation(@PathVariable Long id) {
        return evaluationService.calculateAutomaticEvaluation(id, "Recalcul manuel");
    }

    @PostMapping("/{id}/calculer-score")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public EvaluationResponse calculerScore(@PathVariable Long id) {
        return evaluationService.calculateAutomaticEvaluation(id, "Recalcul manuel");
    }

    @GetMapping("/{id}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public EvaluationResponse getLatestEvaluation(@PathVariable Long id) {
        return evaluationService.getLatestEvaluationByProjet(id);
    }

    @GetMapping("/evaluables")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SujetProjetResponse> getEvaluableProjects() {
        return sujetProjetService.getProjetsEvaluables();
    }
}
