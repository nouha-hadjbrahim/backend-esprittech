package tn.esprit.backend_esprittech.controller.CritereEval;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
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
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE','CI')")
    public EvaluationResponse calculerEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return evaluationService.calculerEvaluation(id, request);
    }

    @GetMapping("/{id}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE','CI')")
    public EvaluationResponse getLatestEvaluation(@PathVariable Long id) {
        return evaluationService.getLatestEvaluationByProjet(id);
    }

    @GetMapping("/evaluables")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE','CI')")
    public List<SujetProjetResponse> getEvaluableProjects() {
        return sujetProjetService.getProjetsEvaluables();
    }
}
